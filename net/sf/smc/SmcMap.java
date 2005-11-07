//
// The contents of this file are subject to the Mozilla Public
// License Version 1.1 (the "License"); you may not use this file
// except in compliance with the License. You may obtain a copy of
// the License at http://www.mozilla.org/MPL/
// 
// Software distributed under the License is distributed on an "AS
// IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
// implied. See the License for the specific language governing
// rights and limitations under the License.
// 
// The Original Code is State Machine Compiler (SMC).
// 
// The Initial Developer of the Original Code is Charles W. Rapp.
// Portions created by Charles W. Rapp are
// Copyright (C) 2000 - 2005. Charles W. Rapp.
// All Rights Reserved.
// 
// Contributor(s):
//   Eitan Suez contributed examples/Ant.
//   (Name withheld) contributed the C# code generation and
//   examples/C#.
//   Francois Perrad contributed the Python code generation and
//   examples/Python.
//
// RCS ID
// $Id$
//
// CHANGE LOG
// (See the bottom of this file.)
//

package net.sf.smc;

import java.io.PrintStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public final class SmcMap
    extends SmcElement
{
//---------------------------------------------------------------
// Member methods
//

    public SmcMap(String name,
                  int lineNumber,
                  SmcFSM fsm)
    {
        super (name, lineNumber);

        _fsm = fsm;
        _defaultState = null;
        _states = (List) new ArrayList();
    }

    public SmcFSM getFSM()
    {
        return (_fsm);
    }

    public List getStates()
    {
        return(_states);
    }

    public void addState(SmcState state)
    {
        if (state.getInstanceName().compareTo("DefaultState") == 0)
        {
            _defaultState = state;
        }
        else
        {
            _states.add(state);
        }

        return;
    }

    public boolean findState(SmcState state)
    {
        SmcState state2;
        Iterator it;
        boolean retval;

        for (it = _states.iterator(), retval = false;
             it.hasNext() == true && retval == false;
            )
        {
            state2 = (SmcState) it.next();
            if (state.getInstanceName().equals(
                    state2.getInstanceName()) == true)
            {
                retval = true;
            }
        }

        return(retval);
    }

    public boolean isKnownState(String stateName)
    {
        SmcState state;
        Iterator it;
        boolean retval;

        for (it = _states.iterator(), retval = false;
             it.hasNext() == true && retval == false;
            )
        {
            state = (SmcState) it.next();
            if (stateName.equals(
                    state.getInstanceName()) == true)
            {
                retval = true;
            }
        }

        return (retval);
    }

    public boolean hasDefaultState()
    {
        return(_defaultState == null ? false : true);
    }

    public SmcState getDefaultState()
    {
        return(_defaultState);
    }

    // Return all transitions appearing in this map.
    public List getTransitions()
    {
        SmcState state;
        Iterator stateIt;
        List trans_list;
        List retval;

        // If this map has a default state, then initialize the
        // transition list to the default state's transitions.
        // Otherwise, set it to the empty list.
        if (_defaultState != null)
        {
            retval =
                (List) new ArrayList(_defaultState.getTransitions());
        }
        else
        {
            retval = (List) new ArrayList();
        }

        // Get each state's transition list and merge it into the
        // results.
        for (stateIt = _states.iterator();
             stateIt.hasNext() == true;
            )
        {
            state = (SmcState) stateIt.next();
            trans_list = state.getTransitions();
            retval =
                Smc.merge(
                    trans_list,
                    retval,
                    new Comparator() {
                        public int compare(Object o1,
                                           Object o2) {
                            return(
                                ((SmcTransition) o1).compareTo(
                                    ((SmcTransition) o2)));
                        }
                    });
        }

        return(retval);
    }

    public List getUndefinedDefaultTransitions()
    {
        List retval = (List) new ArrayList();
        List definedDefaultTransitions;
        Iterator stateIt;
        Iterator transIt;
        SmcTransition transition;
        SmcState state;

        if (_defaultState == null)
        {
            definedDefaultTransitions = (List) new ArrayList();
        }
        else
        {
            definedDefaultTransitions =
                    _defaultState.getTransitions();
            Collections.sort(
                definedDefaultTransitions,
                new Comparator() {
                    public int compare(Object o1,
                                       Object o2) {
                        return(((SmcTransition) o1).compareTo(
                                   (SmcTransition) o2));
                    }
                });
        }

        // Make a transitions list in all the states.
        // For each transition that is *not* defined in the
        // default state, create a default definition for that
        // transition.
        for (stateIt = _states.iterator();
             stateIt.hasNext() == true;
            )
        {
            state = (SmcState) stateIt.next();
            for (transIt = state.getTransitions().iterator();
                 transIt.hasNext() == true;
                )
            {
                // Create the default transition only if it is
                // not already in the default transition list.
                // DO NOT ADD TRANSITIONS NAMED "DEFAULT".
                transition = (SmcTransition) transIt.next();
                if (transition.getName().equals(
                        "Default") != false &&
                    definedDefaultTransitions.contains(
                        transition) == false &&
                    retval.contains(transition) == false)
                {
                    retval.add(transition);
                }
            }
        }

        return(retval);
    }

    public String toString()
    {
        String retval;
        Iterator state_it;
        SmcState state;

        retval = "%map " + _name;
        if (_defaultState != null)
        {
            retval += "\n" + _defaultState;
        }

        for (state_it = _states.iterator();
             state_it.hasNext() == true;
            )
        {
            state = (SmcState) state_it.next();
            retval += "\n" + state;
        }

        return(retval);
    }

    // Returns the next unique state identifier.
    public static int getNextStateId()
    {
        return (_StateId++);
    }

    //-----------------------------------------------------------
    // SmcElement Abstract Methods.
    //

    public void accept(SmcVisitor visitor)
    {
        visitor.visit(this);
        return;
    }

    //
    // end of SmcElement Abstract Methods.
    //-----------------------------------------------------------

//---------------------------------------------------------------
// Member data
//

    private SmcFSM _fsm;
    private List _states;
    private SmcState _defaultState;

    //-----------------------------------------------------------
    // Statics.
    //

    // Use this to generate unique state IDs.
    private static int _StateId = 0;
}

//
// CHANGE LOG
// $Log$
// Revision 1.6  2005/11/07 19:34:54  cwrapp
// Changes in release 4.3.0:
// New features:
//
// + Added -reflect option for Java, C#, VB.Net and Tcl code
//   generation. When used, allows applications to query a state
//   about its supported transitions. Returns a list of transition
//   names. This feature is useful to GUI developers who want to
//   enable/disable features based on the current state. See
//   Programmer's Manual section 11: On Reflection for more
//   information.
//
// + Updated LICENSE.txt with a missing final paragraph which allows
//   MPL 1.1 covered code to work with the GNU GPL.
//
// + Added a Maven plug-in and an ant task to a new tools directory.
//   Added Eiten Suez's SMC tutorial (in PDF) to a new docs
//   directory.
//
// Fixed the following bugs:
//
// + (GraphViz) DOT file generation did not properly escape
//   double quotes appearing in transition guards. This has been
//   corrected.
//
// + A note: the SMC FAQ incorrectly stated that C/C++ generated
//   code is thread safe. This is wrong. C/C++ generated is
//   certainly *not* thread safe. Multi-threaded C/C++ applications
//   are required to synchronize access to the FSM to allow for
//   correct performance.
//
// + (Java) The generated getState() method is now public.
//
// Revision 1.5  2005/05/28 19:28:42  cwrapp
// Moved to visitor pattern.
//
// Revision 1.5  2005/02/21 15:36:20  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.4  2005/02/03 16:46:46  charlesr
// In implementing the Visitor pattern, the generateCode()
// methods have been moved to the appropriate Visitor
// subclasses (e.g. SmcJavaGenerator). This class now extends
// SmcElement.
//
// Revision 1.3  2004/10/30 16:06:07  charlesr
// Added Graphviz DOT file generation.
//
// Revision 1.2  2004/09/06 16:40:32  charlesr
// Added C# support.
//
// Revision 1.1  2004/05/31 13:55:06  charlesr
// Added support for VB.net code generation.
//
// Revision 1.0  2003/12/14 21:04:18  charlesr
// Initial revision
//
