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
// The Original Code is  State Map Compiler(SMC).
// 
// The Initial Developer of the Original Code is Charles W. Rapp.
// Portions created by Charles W. Rapp are
// Copyright (C) 2000 Charles W. Rapp.
// All Rights Reserved.
// 
// Contributor(s): 
//
// RCS ID
// $Id$
//
// statemap.java --
//
//  This package defines the FSMContext class which must be inherited by
//  any Java class wanting to use an smc-generated state map.
//
// Change Log
// $Log$
// Revision 1.1  2001/06/26 22:16:24  cwrapp
// Changes in release 1.0.0:
// Checking in code for first production release.
// If SMC should crash, critical information are printed out along
// with instructions explaining where to send that information.
//

package statemap;

/**
 * A <code>StateUndefinedException</code> is thrown by
 * an SMC-generated state machine whenever a transition is taken
 * and there is no state currently set. This occurs when a
 * transition is issued from with a transition action.
 */
public final class StateUndefinedException
    extends RuntimeException
{
    /**
     * Default constructor.
     */
    public StateUndefinedException()
    {
        super();
    }

    /**
     * Constructs a <code>StateUndefinedException</code>
     * with a detail message.
     * @param reason the detail message.
     */
    public StateUndefinedException(String reason)
    {
        super(reason);
    }
}
