//
// The contents of this file are subject to the Mozilla Public
// License Version 1.1 (the "License"); you may not use this file
// except in compliance with the License. You may obtain a copy
// of the License at http://www.mozilla.org/MPL/
// 
// Software distributed under the License is distributed on an
// "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
// implied. See the License for the specific language governing
// rights and limitations under the License.
// 
// The Original Code is State Machine Compiler (SMC).
// 
// The Initial Developer of the Original Code is Charles W. Rapp.
// Portions created by Charles W. Rapp are
// Copyright (C) 2007. Charles W. Rapp.
// All Rights Reserved.
//
// Contributor(s):
//    Chris Liscio
//
// Namespace
//	statemap
//
// Description
//  This namespace contains the finite state machine context
//  class. The user can derive FSM contexts from this class and
//  interface to them with the methods of this class.
//
// Notes
//  The finite state machine needs to be initialized to the
//  starting state of the FSM.  This must be done manually in
//  the constructor of the derived class. 
//
// Author
//	Chris Liscio
//
// RCS ID
// $Id$
//
// CHANGE LOG
// (See bottom of this file)
//

#import "statemap.h"

@implementation SMCState
- (id)initWithName:(NSString*)name stateId:(int)stateId;
{
    self = [super init];
    if (!self) {
        return nil;
    }
    
    _name = [name copy];
    _stateId = stateId;
    
    return self;
}
- (int)stateId
{
    return _stateId;
}
- (void)setStateId:(int)newStateId
{
    _stateId = newStateId;
}
- (NSString*)name
{
    return _name;
}

- (void)setName:(NSString*)aValue
{
    NSString* oldName = _name;
    _name = [aValue retain];
    [oldName release];
}

- (void)dealloc
{
    [_name release];
    [super dealloc];
}
@end

@implementation SMCStateEntry
+ (id)stateEntryWithState:(SMCState*)state next:(SMCStateEntry*)next;
{
    SMCStateEntry *ret = [[[SMCStateEntry alloc] init] autorelease];
    [ret setState:state];
    [ret setNext:next];
    return ret;
}

- (void)dealloc
{
    [self setState:nil];
    [self setNext:nil];
    [super dealloc];
}

- (SMCState*)state;
{
    return _state;
}

- (void)setState:(SMCState*)aValue;
{
    id old = _state;
    _state = [aValue retain];
    [old release];
}

- (SMCStateEntry*)next;
{
    return _next;
}

- (void)setNext:(SMCStateEntry*)aValue;
{
    id old = _next;
    _next = [aValue retain];
    [old release];
}
@end

@implementation SMCFSMContext
- (id) initWithState: (SMCState*) aState;
{
    self = [super init];

    if (!self)
    {
        return nil;
    }

    _state = aState;

    return self;
}

- (BOOL)isInTransition;
{
    return(_state == NULL ? YES : NO);
}

- (NSString*)transition
{
    return _transition;
}

- (void)setTransition:(NSString*)aValue
{
    NSString* oldTransition = _transition;
    _transition = [aValue retain];
    [oldTransition release];
}

- (void)dealloc
{
    while (_stateStack != NULL) {
        SMCStateEntry *entry = _stateStack;
        _stateStack = [_stateStack next];
        [entry release];
    }
    [super dealloc];
}

- (void)clearState
{
    _previousState = _state;
    _state = NULL;
}

- (SMCState*)previousState;
{
    return _previousState;
}

- (void)setState:(SMCState*)state;
{
    _state = state;
    if ([self debugFlag]) {
        TRACE( @"NEW STATE    : %@\n\r", [_state name] );
    }
}

- (BOOL)isStateStackEmpty;
{
    return (_stateStack == nil);
}

// Returns the state stack's depth.
- (int)stateStackDepth
{
    SMCStateEntry *state_ptr;
    int retval;

    for (state_ptr = _stateStack, retval = 0;
         state_ptr != NULL;
         state_ptr = [state_ptr next], ++retval)
        ;

    return (retval);
}

// Push the current state on top of the state stack
// and make the specified state the current state.
- (void)pushState:(SMCState*)state
{
    SMCStateEntry *new_entry;

    // Do the push only if there is a state to be pushed
    // on the stack.
    if (_state != NULL)
    {
        new_entry = [[SMCStateEntry stateEntryWithState:_state next:_stateStack] retain];
        _stateStack = new_entry;
    }

    _state = state;

    if ([self debugFlag]) {
        TRACE(@"PUSH TO STATE: %@\n\r", [_state name]);
    }
}

// Make the state on top of the state stack the
// current state.
- (void)popState
{
    SMCStateEntry *entry;

    // Popping when there was no previous push is an error.
    NSAssert(_stateStack != NULL, @"Popping empty state stack");

    _state = [_stateStack state];
    entry = _stateStack;
    _stateStack = [_stateStack next];
    [entry release];

    if ([self debugFlag]) {
        TRACE(@"POP TO STATE : %@\n\r", [_state name]);
    }
}

// Remove all states from the state stack.
- (void)emptyStateStack
{
    SMCStateEntry *state_ptr, *next_ptr;

    for (state_ptr = _stateStack;
         state_ptr != NULL;
         state_ptr = next_ptr)
    {
        next_ptr = [state_ptr next];
        [state_ptr release];
    }
    
    _stateStack = NULL;
}

- (BOOL)debugFlag
{
    return _debugFlag;
}

- (void)setDebugFlag:(BOOL)newDebugFlag
{
    _debugFlag = newDebugFlag;
}

@end

//
// CHANGE LOG
// $Log$
// Revision 1.2  2009/04/10 14:02:10  cwrapp
// Set initial state via initializer.
//
// Revision 1.1  2007/01/15 00:23:50  cwrapp
// Release 4.4.0 initial commit.
//
