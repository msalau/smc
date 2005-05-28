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
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public final class SmcGuard
    extends SmcElement
{
//---------------------------------------------------------------
// Member methods
//

    public SmcGuard(String cond,
                    int lineNumber,
                    SmcTransition transition)
    {
        super (transition.getName(), lineNumber);

        _transition = transition;
        _condition = cond;
        _endState = "";
        _pushState = "";
        _actions = null;
        _popArgs = "";
    }

    public SmcTransition getTransition()
    {
        return (_transition);
    }

    public String getCondition()
    {
        return(_condition);
    }

    public int getTransType()
    {
        return(_transType);
    }

    public void setTransType(int transType)
    {
        _transType = transType;
        return;
    }

    public String getEndState()
    {
        return(_endState);
    }

    public void setEndState(String endState)
    {
        _endState = endState;
        return;
    }

    public String getPushState()
    {
        return (_pushState);
    }

    public void setPushState(String state)
    {
        _pushState = state;
        return;
    }

    public String getPopArgs()
    {
        return (_popArgs);
    }

    public void setPopArgs(String args)
    {
        _popArgs = args;
        return;
    }

    public List getActions()
    {
        return (_actions);
    }

    public void setActions(List actions)
    {
        _actions = (List) ((LinkedList) actions).clone();
        return;
    }

    public String toString()
    {
        StringBuffer retval = new StringBuffer(512);

        retval.append(_name);

        if (_condition.length() > 0)
        {
            retval.append(" [");
            retval.append(_condition);
            retval.append("]");
        }

        switch(_transType)
        {
            case Smc.TRANS_NOT_SET:
                retval.append(" not set");
                break;

            case Smc.TRANS_SET:
            case Smc.TRANS_PUSH:
                retval.append(" set");
                break;

            case Smc.TRANS_POP:
                retval.append(" pop");
                break;
        }

        retval.append(" ");
        retval.append(_endState);

        if (_transType == Smc.TRANS_PUSH)
        {
            retval.append("/");
            retval.append(" push(");
            retval.append(_pushState);
            retval.append(")");
        }

        retval.append(" {\n");
        if (_actions.size() > 0)
        {
            Iterator ait;

            for (ait = _actions.iterator();
                 ait.hasNext() == true;
                )
            {
                retval.append("    ");
                retval.append((SmcAction) ait.next());
                retval.append(";\n");
            }
        }
        retval.append("}");

        return(retval.toString());
    }

    // Returns true if this guard references the ctxt variable.
    public boolean hasCtxtReference()
    {
        boolean retcode = false;

        // The ctxt variable may appear in the condition, the
        // actions or in the pop arguments.
        if ((_condition != null &&
             _condition.indexOf("ctxt.") >= 0) ||
            (_actions != null &&
             _actions.isEmpty() == false) ||
            (_transType == Smc.TRANS_POP &&
             _popArgs != null &&
             _popArgs.indexOf("ctxt.") >= 0))
        {
            retcode = true;
        }

        return (retcode);
    }

    //-----------------------------------------------------------
    // SmcElement Abstract Methods.
    //

    public void accept(SmcVisitor visitor)
    {
        visitor.visit(this);
    }

    //
    // end of SmcElement Abstract Methods.
    //-----------------------------------------------------------

    // Scope the state name. If the state is unscoped, then
    // return "<mapName>.<stateName>". If the state named
    // contains the scope string "::", replace that with a ".".
    private String _scopeStateName(final String stateName,
                                   final String mapName)
    {
        int index;
        StringWriter retval = new StringWriter();

        index = stateName.indexOf("::");
        if (index < 0)
        {
            retval.write(mapName);
            retval.write(".");
            retval.write(stateName);
        }
        else
        {
            retval.write(stateName.substring(0, index));
            retval.write('.');
            retval.write(stateName.substring(index + 2));
        }

        return (retval.toString());
    }

//---------------------------------------------------------------
// Member data.
//

    private SmcTransition _transition;
    private String _condition;
    private int _transType;
    private String _endState;
    private String _pushState;
    private String _popArgs;
    private List _actions;

    //-----------------------------------------------------------
    // Constants.
    //
    /* package */ static final String NIL_STATE = "nil";
}

//
// CHANGE LOG
// $Log$
// Revision 1.6  2005/05/28 19:28:42  cwrapp
// Moved to visitor pattern.
//
// Revision 1.7  2005/02/21 15:35:24  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.6  2005/02/21 15:14:07  charlesr
// Moved isLoopback() method from this class to SmcCodeGenerator.
//
// Revision 1.5  2005/02/03 16:45:49  charlesr
// In implementing the Visitor pattern, the generateCode()
// methods have been moved to the appropriate Visitor
// subclasses (e.g. SmcJavaGenerator). This class now extends
// SmcElement.
//
// Revision 1.4  2004/11/14 18:24:59  charlesr
// Minor improvements to the DOT file output.
//
// Revision 1.3  2004/10/30 16:04:39  charlesr
// Added Graphviz DOT file generation.
// Added getPopArgs() method.
//
// Revision 1.2  2004/09/06 16:40:01  charlesr
// Added C# support.
//
// Revision 1.1  2004/05/31 13:54:03  charlesr
// Added support for VB.net code generation.
//
// Revision 1.0  2003/12/14 21:03:28  charlesr
// Initial revision
//