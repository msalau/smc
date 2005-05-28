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
// Copyright (C) 2005. Charles W. Rapp.
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Visits the abstract syntax tree, emitting Java code.
 * @see SmcElement
 * @see SmcCodeGenerator
 * @see SmcVisitor
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcJavaGenerator
    extends SmcCodeGenerator
{
//---------------------------------------------------------------
// Member methods
//

    public SmcJavaGenerator(PrintStream source,
                            String srcfileBase)
    {
        super (source, srcfileBase);
    }

    public void visit(SmcFSM fsm)
    {
        String rawSource = fsm.getSource();
        String packageName = fsm.getPackage();
        String context = fsm.getContext();
        String startState = fsm.getStartState();
        List maps = fsm.getMaps();
        List transitions;
        Iterator it;
        Iterator it2;
        SmcMap map;
        SmcTransition trans;
        String transName;
        SmcParameter param;
        String javaState;
        String separator;
        int index;
        List params;

        // Dump out the raw source code, if any.
        if (rawSource != null && rawSource.length() > 0)
        {
            _source.println(rawSource);
            _source.println();
        }

        // If a package has been specified, generate the
        // package statement now.
        if (packageName != null && packageName.length() > 0)
        {
            _source.print("package ");
            _source.print(packageName);
            _source.println(";");
            _source.println();
        }

        // Do user-specified imports now.
        for (it = fsm.getImports().iterator();
             it.hasNext() == true;
            )
        {
            _source.print("import ");
            _source.print(it.next());
            _source.println(";");
        }

        // If the -g option was specified, then import the
        // PrintStream class.
        if (Smc.isDebug() == true)
        {
            _source.println("import java.io.PrintStream;");
            _source.println();
        }
        else if (fsm.getImports().size() != 0)
        {
            _source.println();
        }

        // The context clas contains all the state classes as
        // inner classes, so generate the context first rather
        // than last.
        _source.print("public final class ");
        _source.print(context);
        _source.println("Context");
        _source.println("    extends statemap.FSMContext");

        if (Smc.isSerial() == true)
        {
            _source.println(
                "    implements java.io.Serializable");
        }

        _source.println("{");
        _source.println("// Member methods.");
        _source.println();

        // Generate the context class' constructor.
        _source.print("    public ");
        _source.print(context);
        _source.print("Context(");
        _source.print(context);
        _source.println(" owner)");
        _source.println("    {");
        _source.println("        super();");
        _source.println();
        _source.println("        _owner = owner;");

        // The state name "map::state" must be changed to
        // "map.state".
        if ((index = startState.indexOf("::")) >= 0)
        {
            javaState =
                startState.substring(0, index) +
                "." +
                startState.substring(index + 2);
        }
        else
        {
            javaState = startState;
        }

        _source.print("        setState(");
        _source.print(javaState);
        _source.println(");");

        // Execute the start state's entry actions.
        _source.print("        ");
        _source.print(javaState);
        _source.println(".Entry(this);");

        _source.println("    }");
        _source.println();

        // Generate the default transition methods.
        // Get the transition list.
        transitions = (List) new ArrayList();
        for (it = maps.iterator(); it.hasNext() == true;)
        {
            map = (SmcMap) it.next();

            // Merge the new transitions into the current set.
            transitions =
                Smc.merge(
                    map.getTransitions(),
                    transitions,
                    new Comparator()
                    {
                        public int compare(Object o1,
                                           Object o2)
                        {
                            return (
                                ((SmcTransition) o1).compareTo(
                                    (SmcTransition) o2));
                        }
                    });
        }

        // Generate the transition methods.
        for (it = transitions.iterator(); it.hasNext() == true;)
        {
            trans = (SmcTransition) it.next();

            if (trans.getName().equals("Default") == false)
            {
                _source.print("    public ");

                // If the -sync flag was specified, then output
                // the "synchronized" keyword.
                if (Smc.isSynchronized() == true)
                {
                    _source.print("synchronized ");
                }

                _source.print("void ");
                _source.print(trans.getName());
                _source.print("(");

                params = trans.getParameters();
                for (it2 = params.iterator(), separator = "";
                     it2.hasNext() == true;
                     separator = ", ")
                {
                    _source.print(separator);
                    ((SmcParameter) it2.next()).accept(this);
                }
                _source.println(")");
                _source.println("    {");

                // Save away the transition name in case it is
                // need in an UndefinedTransitionException.
                _source.print("        _transition = \"");
                _source.print(trans.getName());
                _source.println("\";");

                _source.print("        getState().");
                _source.print(trans.getName());
                _source.print("(this");

                for (it2 = params.iterator();
                     it2.hasNext() == true;
                    )
                {
                    param = (SmcParameter) it2.next();

                    _source.print(", ");
                    _source.print(param.getName());
                }
                _source.println(");");
                _source.println("        _transition = \"\";");

                _source.println("        return;");
                _source.println("    }");
                _source.println();
            }
        }

        // If serialization is turned on, then generate a
        // setOwner method which allows the application class
        // to restore its ownership of the FSM.
        if (Smc.isSerial() == true)
        {
            _source.print("    public void setOwner(");
            _source.print(context);
            _source.println(" owner)");
            _source.println("    {");
            _source.println("        if (owner == null)");
            _source.println("        {");
            _source.println(
                "            throw (new NullPointerException());");
            _source.println("        }");
            _source.println();
            _source.println("        _owner = owner;");
            _source.println("        return;");
            _source.println("    }");
            _source.println();
        }

        // getState() method.
        _source.print("    protected ");
        _source.print(context);
        _source.println("State getState()");
        _source.println(
            "        throws statemap.StateUndefinedException");
        _source.println("    {");
        _source.println("        if (_state == null)");
        _source.println("        {");
        _source.println(
            "            throw(");
        _source.println(
            "                new statemap.StateUndefinedException());");
        _source.println("        }");
        _source.println();
        _source.print("        return ((");
        _source.print(context);
        _source.println("State) _state);");
        _source.println("    }");
        _source.println();

        // getOwner() method.
        _source.print("    protected ");
        _source.print(context);
        _source.println(" getOwner()");
        _source.println("    {");
        _source.println("        return (_owner);");
        _source.println("    }");
        _source.println();

        // If serialization is turned on, then output the
        // writeObject and readObject methods.
        if (Smc.isSerial() == true)
        {
            _source.print(
                "    private void writeObject(");
            _source.println(
                "java.io.ObjectOutputStream ostream)");
            _source.println(
                "        throws java.io.IOException");
            _source.println("    {");
            _source.println(
                "        int size =");
            _source.print("            ");
            _source.println(
                "(_stateStack == null ? 0 : _stateStack.size());");
            _source.println("        int i;");
            _source.println();
            _source.println(
                "        ostream.writeInt(size);");
            _source.println();
            _source.println(
                "        for (i = 0; i < size; ++i)");
            _source.println("        {");
            _source.println("            ostream.writeInt(");
            _source.print("                ((");
            _source.print(context);
            _source.println(
                "State) _stateStack.get(i)).getId());");
            _source.println("        }");
            _source.println();
            _source.println(
                "        ostream.writeInt(_state.getId());");
            _source.println();
            _source.println("        return;");
            _source.println("    }");
            _source.println();
            _source.print("    private void readObject(");
            _source.println(
                "java.io.ObjectInputStream istream)");
            _source.println(
                "        throws java.io.IOException");
            _source.println("    {");
            _source.println("        int size;");
            _source.println();
            _source.println("        size = istream.readInt();");
            _source.println();
            _source.println("        if (size == 0)");
            _source.println("        {");
            _source.println("            _stateStack = null;");
            _source.println("        }");
            _source.println("        else");
            _source.println("        {");
            _source.println("            int i;");
            _source.println();
            _source.println(
                "            _stateStack = new java.util.Stack();");
            _source.println();
            _source.println(
                "            for (i = 0; i < size; ++i)");
            _source.println("            {");
            _source.print(
                "                _stateStack.add(i, _States[");
            _source.println("istream.readInt()]);");
            _source.println("            }");
            _source.println("        }");
            _source.println();
            _source.println(
                "        _state = _States[istream.readInt()];");
            _source.println();
            _source.println("        return;");
            _source.println("    }");
            _source.println();
        }

        // Declare member data.
        _source.println("// Member data.");
        _source.println();
        _source.print("    transient private ");
        _source.print(context);
        _source.println(" _owner;");

        // If serialization support is on, then create the state
        // array.
        if (Smc.isSerial() == true)
        {
            String mapName;
            Iterator stateIt;
            SmcState state;

            _source.print("    transient private static ");
            _source.print(context);
            _source.println("State[] _States =");
            _source.println("    {");

            for (it = fsm.getMaps().iterator(),
                     separator = "";
                 it.hasNext() == true;
                )
            {
                map = (SmcMap) it.next();
                mapName = map.getName();
                for (stateIt = map.getStates().iterator();
                     stateIt.hasNext() == true;
                     separator = ",\n")
                {
                    state = (SmcState) stateIt.next();
                    _source.print(separator);
                    _source.print("        ");
                    _source.print(mapName);
                    _source.print(".");
                    _source.print(state.getClassName());
                }
            }

            _source.println();
            _source.println("    };");
        }

        // Declare the inner state class.
        _source.println();
        _source.print("    protected static abstract class ");
        _source.print(context);
        _source.println("State");
        _source.println("        extends statemap.State");
        _source.println("    {");

        // Constructor.
        _source.print("        protected ");
        _source.print(context);
        _source.println("State(String name, int id)");
        _source.println("        {");
        _source.println("            super (name, id);");
        _source.println("        }");
        _source.println();
        _source.print("        protected void Entry(");
        _source.print(context);
        _source.println("Context context) {}");
        _source.print("        protected void Exit(");
        _source.print(context);
        _source.println("Context context) {}");
        _source.println();

        // Generate the default transition definitions.
        for (it = transitions.iterator(); it.hasNext() == true;)
        {
            trans = (SmcTransition) it.next();
            transName = trans.getName();

            // Don't generate the Default transition here.
            if (transName.equals("Default") == false)
            {
                _source.print("        protected void ");
                _source.print(transName);
                _source.print("(");
                _source.print(context);
                _source.print("Context context");

                params = trans.getParameters();
                for (it2 = params.iterator();
                     it2.hasNext() == true;
                    )
                {
                    _source.print(", ");
                    ((SmcParameter) it2.next()).accept(this);
                }

                _source.println(")");
                _source.println("        {");

                // If this method is reached, that means that this
                // transition was passed to a state which does not
                // define the transition. Call the state's default
                // transition method.
                _source.println("            Default(context);");

                _source.println("        }");
                _source.println();
            }
        }

        // Generate the overall Default transition for all maps.
        _source.print("        protected void Default(");
        _source.print(context);
        _source.println("Context context)");
        _source.println("        {");

        if (Smc.isDebug() == true)
        {
            _source.println(
                "            if (context.getDebugFlag() == true)");
            _source.println("            {");
            _source.println(
                "                PrintStream str = ");
            _source.println(
                "                    context.getDebugStream();");
            _source.println();
            _source.println(
                "                str.println(");
            _source.println(
                "                    \"TRANSITION   : Default\");");
            _source.println("            }");
            _source.println();
        }

        _source.println("            throw (");
        _source.println(
            "                new statemap.TransitionUndefinedException(");
        _source.println(
            "                    \"State: \" +");
        _source.println(
            "                    context.getState().getName() +");
        _source.println(
            "                    \", Transition: \" +");
        _source.println(
            "                    context.getTransition()));");
        _source.println("        }");

        // End of state class.
        _source.println("    }");

        // Have each map print out its source code now.
        for (it = maps.iterator(); it.hasNext();)
        {
            ((SmcMap) it.next()).accept(this);
        }

        // End of context class.
        _source.println("}");

        return;
    }

    public void visit(SmcMap map)
    {
        List definedDefaultTransitions;
        SmcState defaultState = map.getDefaultState();
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        List states = map.getStates();
        Iterator it;
        SmcState state;

        // Initialize the default transition list to all the
        // default state's transitions.
        if (defaultState != null)
        {
            definedDefaultTransitions =
                defaultState.getTransitions();
        }
        else
        {
            definedDefaultTransitions = (List) new ArrayList();
        }

        // Declare the map class. Declare it abstract to prevent
        // its instantiation.
        _source.println();
        _source.print(
            "    /* package */ static abstract class ");
        _source.println(mapName);
        _source.println("    {");

        // Declare each of the state class member data.
        for (it = states.iterator(); it.hasNext() == true;)
        {
            state = (SmcState) it.next();

            _source.print("        /* package */ static ");
            _source.print(mapName);
            _source.print("_Default.");
            _source.print(mapName);
            _source.print('_');
            _source.print(state.getClassName());
            _source.print(' ');
            _source.print(state.getInstanceName());
            _source.println(';');
        }

        // Create a default state as well.
        _source.print("        private static ");
        _source.print(mapName);
        _source.println("_Default Default;");
        _source.println();

        // Declare the static block.
        _source.println("        static");
        _source.println("        {");

        // Initialize the static state objects.
        for (it = states.iterator(); it.hasNext() == true;)
        {
            state = (SmcState) it.next();

            _source.print("            ");
            _source.print(state.getInstanceName());
            _source.print(" = new ");
            _source.print(mapName);
            _source.print("_Default.");
            _source.print(mapName);
            _source.print('_');
            _source.print(state.getClassName());
            _source.print("(\"");
            _source.print(mapName);
            _source.print('.');
            _source.print(state.getClassName());
            _source.print("\", ");
            _source.print(map.getNextStateId());
            _source.println(");");
        }

        // Instantiate a default state as well.
        _source.print("            Default = new ");
        _source.print(mapName);
        _source.print("_Default(\"");
        _source.print(mapName);
        _source.println(".Default\", -1);");

        // End of static block.
        _source.println("        }");
        _source.println();

        // End of the map class.
        _source.println("    }");
        _source.println();

        // Declare the map default state class.
        _source.print("    protected static class ");
        _source.print(mapName);
        _source.println("_Default");
        _source.print("        extends ");
        _source.print(context);
        _source.println("State");
        _source.println("    {");

        // Generate the constructor.
        _source.print("        protected ");
        _source.print(mapName);
        _source.println("_Default(String name, int id)");
        _source.println("        {");
        _source.println("            super (name, id);");
        _source.println("        }");

        // Declare the user-defined default transitions first.
        _indent = "        ";
        for (it = definedDefaultTransitions.iterator();
             it.hasNext() == true;
            )
        {
            ((SmcTransition) it.next()).accept(this);
        }

        // Have each state now generate its code. Each state
        // class is an inner class.
        for (it = states.iterator(); it.hasNext() == true;)
        {
            ((SmcState) it.next()).accept(this);
        }

        // The map class has been defined.
        _source.println("    }");

        return;
    }

    public void visit(SmcState state)
    {
        SmcMap map = state.getMap();
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        String stateName = state.getClassName();
        List actions;
        String indent2;
        Iterator it;

        // Declare the inner state class.
        _source.println();
        _source.print("        private static final class ");
        _source.print(mapName);
        _source.print('_');
        _source.println(stateName);
        _source.print("            extends ");
        _source.print(mapName);
        _source.println("_Default");
        _source.println("        {");

        // Add the constructor.
        _source.print("            private ");
        _source.print(mapName);
        _source.print('_');
        _source.print(stateName);
        _source.println("(String name, int id)");
        _source.println("            {");
        _source.println("                super (name, id);");
        _source.println("            }");

        // Add the Entry() and Exit() member functions if this
        // state defines them.
        actions = state.getEntryActions();
        if (actions != null && actions.size() > 0)
        {
            _source.println();
            _source.print("            protected void Entry(");
            _source.print(context);
            _source.println("Context context)");
            _source.println("            {");

            // Declare the "ctxt" local variable.
            _source.print("                ");
            _source.print(context);
            _source.println(" ctxt = context.getOwner();");
            _source.println();

            // Generate the actions associated with this code.
            indent2 = _indent;
            _indent = "                ";
            for (it = actions.iterator(); it.hasNext() == true;)
            {
                ((SmcAction) it.next()).accept(this);
            }

            _indent = indent2;

            // End the Entry() member function with a return.
            _source.println("                return;");
            _source.println("            }");
        }

        actions = state.getExitActions();
        if (actions != null && actions.size() > 0)
        {
            _source.println();
            _source.print("            protected void Exit(");
            _source.print(context);
            _source.println("Context context)");
            _source.println("            {");

            // Declare the "ctxt" local variable.
            _source.print("                ");
            _source.print(context);
            _source.println(" ctxt = context.getOwner();");
            _source.println();

            // Generate the actions associated with this code.
            indent2 = _indent;
            _indent = "                ";
            for (it = actions.iterator(); it.hasNext() == true;)
            {
                ((SmcAction) it.next()).accept(this);
            }

            // End the Exit() member function with a return.
            _source.println("                return;");
            _source.println("            }");
        }

        // Have each transition generate its code.
        _indent = "            ";
        for (it = state.getTransitions().iterator();
             it.hasNext() == true;
            )
        {
            ((SmcTransition) it.next()).accept(this);
        }

        // End of this state class declaration.
        _source.println("        }");

        return;
    }

    public void visit(SmcTransition transition)
    {
        SmcState state = transition.getState();
        SmcMap map = state.getMap();
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        String stateName = state.getClassName();
        String transName = transition.getName();
        List parameters = transition.getParameters();
        List guards = transition.getGuards();
        boolean nullCondition = false;
        Iterator it;
        SmcGuard guard;
        SmcParameter param;

        _source.println();
        _source.print(_indent);
        _source.print("protected void ");
        _source.print(transName);
        _source.print("(");
        _source.print(context);
        _source.print("Context context");

        // Add user-defined parameters.
        for (it = parameters.iterator(); it.hasNext() == true;)
        {
            _source.print(", ");
            ((SmcParameter) it.next()).accept(this);
        }
        _source.println(")");

        _source.print(_indent);
        _source.println("{");

        // All transitions have a "ctxt" local variable.
        // 8/14/2003:
        // Do this only if there are any transition actions or
        // guard conditions which reference it.
        if (transition.hasCtxtReference() == true)
        {
            _source.print(_indent);
            _source.print("    ");
            _source.print(context);
            _source.println(" ctxt = context.getOwner();");
        }

        _source.println();

        // Output transition to debug stream.
        if (Smc.isDebug() == true)
        {
            String sep;

            _source.print(_indent);
            _source.println(
                "    if (context.getDebugFlag() == true)");
            _source.print(_indent);
            _source.println("    {");
            _source.print(_indent);
            _source.print("        PrintStream str = ");
            _source.println("context.getDebugStream();");
            _source.println();
            _source.print(_indent);
            _source.print(
                "        str.println(\"TRANSITION   : ");
            _source.print(mapName);
            _source.print('.');
            _source.print(stateName);
            _source.print('.');
            _source.print(transName);

            _source.print('(');
            for (it = parameters.iterator(), sep = "";
                 it.hasNext() == true;
                 sep = ", ")
            {
                _source.print(sep);
                ((SmcParameter) it.next()).accept(this);
            }
            _source.print(')');

            _source.println("\");");
            _source.print(_indent);
            _source.println("    }");
            _source.println();
        }

        // Loop through the guards and print each one.
        for (it = guards.iterator(),
                 _guardIndex = 0,
                 _guardCount = guards.size();
             it.hasNext() == true;
             ++_guardIndex)
        {
            guard = (SmcGuard) it.next();

            // Count up the guards with no condition.
            if (guard.getCondition().length() == 0)
            {
                nullCondition = true;
            }

            guard.accept(this);
        }

        // If all guards have a condition, then create a final
        // "else" clause which passes control to the default
        // transition. Pass all arguments into the default
        // transition.
        if (_guardIndex > 0 && nullCondition == false)
        {
            if (_guardCount == 1)
            {
                _source.print(_indent);
                _source.println("    }");
            }

            _source.print(_indent);
            _source.println("    else");
            _source.print(_indent);
            _source.println("    {");

            // Call the super class' transition method using
            // the "super" keyword and not the class name.
            _source.print(_indent);
            _source.print("        super.");
            _source.print(transName);
            _source.print("(context");

            for (it = parameters.iterator();
                 it.hasNext() == true;
                )
            {
                _source.print(", ");
                _source.print(
                    ((SmcParameter) it.next()).getName());
            }

            _source.println(");");
            _source.print(_indent);
            _source.println("    }");
            _source.println();
        }
        // Need to add a final newline after a multiguard block.
        else if (_guardCount > 1)
        {
            _source.println();
            _source.println();
        }

        _source.print(_indent);
        _source.println("    return;");
        _source.print(_indent);
        _source.println("}");

        return;
    }

    public void visit(SmcGuard guard)
    {
        SmcTransition transition = guard.getTransition();
        SmcState state = transition.getState();
        SmcMap map = state.getMap();
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        String stateName = state.getClassName();
        int transType = guard.getTransType();
        boolean defaultFlag =
            stateName.equalsIgnoreCase("Default");
        boolean loopbackFlag = false;
        String indent2;
        String indent3;
        String indent4;
        String endStateName = guard.getEndState();
        String fqEndStateName = "";
        String pushStateName = guard.getPushState();
        String condition = guard.getCondition();
        List actions = guard.getActions();

        // If this guard's end state is not of the form
        // "map::state", then prepend the map name to the
        // state name.
        // DON'T DO THIS IF THIS IS A POP TRANSITION!
        // The "state" is actually a transition name.
        if (transType != Smc.TRANS_POP &&
            endStateName.length () > 0 &&
            endStateName.equals("nil") == false)
        {
            endStateName = scopeStateName(endStateName, mapName);
        }

        // Qualify the state and push state names as well.
        stateName = scopeStateName(stateName, mapName);
        pushStateName = scopeStateName(pushStateName, mapName);

        loopbackFlag =
            isLoopback(transType, stateName, endStateName);

        // The guard code generation is a bit tricky. The first
        // question is how many guards are there? If there are
        // more than one, then we will need to generate the
        // proper "if-then-else" code.
        if (_guardCount > 1)
        {
            indent2 = _indent + "        ";

            // There are multiple guards. Is this the first guard?
            if (_guardIndex == 0 && condition.length() > 0)
            {
                // Yes, this is the first. This means an "if"
                // should be used.
                _source.print(_indent);
                _source.print("    if (");
                _source.print(condition);
                _source.println(")");
                _source.print(_indent);
                _source.println("    {");
            }
            else if (condition.length() > 0)
            {
                // No, this is not the first transition but it
                // does have a condition. Use an "else if".
                _source.println();
                _source.print(_indent);
                _source.print("    else if (");
                _source.print(condition);
                _source.println(")");
                _source.print(_indent);
                _source.println("    {");
            }
            else
            {
                // This is not the first transition and it has
                // no condition.
                _source.println();
                _source.print(_indent);
                _source.println("    else");
                _source.print(_indent);
                _source.println("    {");
            }
        }
        // There is only one guard. Does this guard have a
        // condition?
        else if (condition.length() == 0)
        {
            // No. This is a plain, old. vanilla transition.
            indent2 = _indent + "    ";
        }
        else
        {
            // Yes there is a condition.
            indent2 = _indent + "        ";

            _source.print(_indent);
            _source.print("    if (");
            _source.print(condition);
            _source.println(")");
            _source.print(_indent);
            _source.println("    {");
        }

        // Now that the necessary conditions are in place, it's
        // time to dump out the transition's actions. First, do
        // the proper handling of the state change. If this
        // transition has no actions, then set the end state
        // immediately. Otherwise, unset the current state so
        // that if an action tries to issue a transition, it will
        // fail.
        if (actions.size() == 0)
        {
            fqEndStateName = endStateName;
        }
        // Save away the current state if this is a loopback
        // transition. Storing current state allows the
        // current state to be cleared before any actions are
        // executed. Remember: actions are not allowed to
        // issue transitions and clearing the current state
        // prevents them from doing do.
        else if (loopbackFlag == true)
        {
            fqEndStateName = "endState";

            _source.print(indent2);
            _source.print(context);
            _source.print("State ");
            _source.print(fqEndStateName);
            _source.println(" = context.getState();");
            _source.println();
        }
        else
        {
            fqEndStateName = endStateName;
        }

        // Decide if runtime loopback checking must be done.
        if (defaultFlag == true &&
            transType != Smc.TRANS_POP &&
            loopbackFlag == false)
        {
            _source.print(indent2);
            _source.println("boolean loopbackFlag =");
            _source.print(indent2);
            _source.println(
                "    context.getState().getName().equals(");
            _source.print(indent2);
            _source.print("        ");
            _source.print(fqEndStateName);
            _source.println(".getName());");
            _source.println();
        }

        // Dump out the exit actions - but only for the first guard.
        // v. 1.0, beta 3: Not any more. The exit actions are
        // executed only if 1) this is a standard, non-loopback
        // transition or a pop transition.
        if (transType == Smc.TRANS_POP || loopbackFlag == false)
        {
            indent4 = indent2;

            // If this is a non-loopback, generic transition,
            // do runtime loopback checking.
            if (transType == Smc.TRANS_SET &&
                defaultFlag == true)
            {
                indent4 = indent2 + "    ";

                _source.print(indent2);
                _source.println("if (loopbackFlag == false)");
                _source.print(indent2);
                _source.println('{');
            }

            _source.print(indent4);
            _source.println(
                "(context.getState()).Exit(context);");

            if (transType == Smc.TRANS_SET &&
                defaultFlag == true)
            {
                _source.print(indent2);
                _source.println('}');
                _source.println();
            }
        }

        // Dump out this transition's actions.
        if (actions.size() == 0)
        {
            if (condition.length() > 0)
            {
                _source.print(indent2);
                _source.println("// No actions.");
            }

            indent3 = indent2;
        }
        else
        {
            Iterator it;

            // Now that we are in the transition, clear the
            // current state.
            _source.print(indent2);
            _source.println("context.clearState();");

            // v. 2.0.0: Place the actions inside a try/finally
            // block. This way the state will be set before an
            // exception leaves the transition method.
            // v. 2.2.0: Check if the user has turned off this
            // feature first.
            if (Smc.isNoCatch() == false)
            {
                _source.print(indent2);
                _source.println("try");
                _source.print(indent2);
                _source.println('{');

                indent3 = indent2 + "    ";
            }
            else
            {
                indent3 = indent2;
            }

            indent4 = _indent;
            _indent = indent3;

            for (it = actions.iterator(); it.hasNext() == true;)
            {
                ((SmcAction) it.next()).accept(this);
            }

            _indent = indent4;

            // v. 2.2.0: Check if the user has turned off this
            // feature first.
            if (Smc.isNoCatch() == false)
            {
                _source.print(indent2);
                _source.println('}');
                _source.print(indent2);
                _source.println("finally");
                _source.print(indent2);
                _source.println('{');
            }
        }

        // Print the setState() call, if necessary. Do NOT
        // generate the set state it:
        // 1. The transition has no actions AND is a loopback OR
        // 2. This is a push or pop transition.
        if (transType == Smc.TRANS_SET &&
            (actions.size() > 0 || loopbackFlag == false))
        {
            _source.print(indent3);
            _source.print("context.setState(");
            _source.print(fqEndStateName);
            _source.println(");");
        }
        else if (transType == Smc.TRANS_PUSH)
        {
            // Set the next state so this it can be pushed
            // onto the state stack. But only do so if a clear
            // state was done.
            if (loopbackFlag == false || actions.size() > 0)
            {
                _source.print(indent3);
                _source.print("context.setState(");
                _source.print(fqEndStateName);
                _source.println(");");
            }

            // Before doing the push, execute the end state's
            // entry actions (if any) if this is not a loopback.
            if (loopbackFlag == false)
            {
                if (defaultFlag == true)
                {
                    indent4 = indent3 + "    ";

                    _source.println();
                    _source.print(indent3);
                    _source.println(
                        "if (loopbackFlag == false)");
                    _source.print(indent3);
                    _source.println('{');
                }
                else
                {
                    indent4 = indent3;
                }

                _source.print(indent4);
                _source.println(
                    "(context.getState()).Entry(context);");

                if (defaultFlag == true)
                {
                    _source.print(indent3);
                    _source.println('}');
                }
            }

            _source.print(indent3);
            _source.print("context.pushState(");
            _source.print(pushStateName);
            _source.println(");");
        }
        else if (transType == Smc.TRANS_POP)
        {
            _source.print(indent3);
            _source.println("context.popState();");
        }

        // Perform the new state's enty actions.
        // v. 1.0, beta 3: Not any more. The entry actions are
        // executed only if 1) this is a standard, non-loopback
        // transition or a push transition.
        if ((transType == Smc.TRANS_SET &&
             loopbackFlag == false) ||
             transType == Smc.TRANS_PUSH)
        {
            indent4 = indent3;

            // If this is a non-loopback, generic transition,
            // do runtime loopback checking.
            if (transType == Smc.TRANS_SET &&
                defaultFlag == true)
            {
                indent4 = indent3 + "    ";

                _source.println();
                _source.print(indent3);
                _source.println("if (loopbackFlag == false)");
                _source.print(indent3);
                _source.println('{');
            }

            _source.print(indent4);
            _source.println(
                "(context.getState()).Entry(context);");

            if (transType == Smc.TRANS_SET &&
                defaultFlag == true)
            {
                _source.print(indent3);
                _source.println('}');
                _source.println();
            }
        }

        // If there was a try/finally, then put the closing
        // brace on the finally block.
        // v. 2.2.0: Check if the user has turned off this
        // feature first.
        if (actions.size() > 0 && Smc.isNoCatch() == false)
        {
            _source.print(indent2);
            _source.println('}');
        }

        // If there is a transition associated with the pop, then
        // issue that transition here.
        if (transType == Smc.TRANS_POP &&
            endStateName.equals(NIL_STATE) == false &&
            endStateName.length() > 0)
        {
            String popArgs = guard.getPopArgs();

            _source.println();
            _source.print(indent2);
            _source.print("context.");
            _source.print(endStateName);
            _source.print("(");

            // Output any and all pop arguments.
            if (popArgs.length() > 0)
            {
                _source.print(popArgs);
            }
            _source.println(");");
        }

        // If this is a guarded transition, it will be necessary
        // to close off the "if" body. DON'T PRINT A NEW LINE!
        // Why? Because an "else" or "else if" may follow and we
        // won't know until we go back to the transition source
        // generator whether all clauses have been done.
        if (_guardCount > 1)
        {
            _source.print(_indent);
            _source.print("    }");
        }

        return;
    }

    public void visit(SmcAction action)
    {
        String name = action.getName();
        Iterator it;
        String sep;

        // Need to distinguish between FSMContext actions and
        // application class actions. If the action is
        // "emptyStateStack", then pass it to the context.
        // Otherwise, let the application class handle it.
        _source.print(_indent);
        if (name.equals("emptyStateStack") == true)
        {
            _source.print("context.");
        }
        else
        {
            _source.print("ctxt.");
        }
        _source.print(name);
        _source.print("(");

        for (it = action.getArguments().iterator(), sep = "";
             it.hasNext() == true;
             sep = ", ")
        {
            _source.print(sep);
            _source.print((String) it.next());
        }

        _source.println(");");

        return;
    }

    public void visit(SmcParameter parameter)
    {
        _source.print(parameter.getType());
        _source.print(' ');
        _source.print(parameter.getName());

        return;
    }

//---------------------------------------------------------------
// Member data
//
}

//
// CHANGE LOG
// $Log$
// Revision 1.1  2005/05/28 19:28:42  cwrapp
// Moved to visitor pattern.
//
// Revision 1.2  2005/02/21 15:35:45  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.1  2005/02/21 15:18:32  charlesr
// Modified isLoopback() to new signature due to moving method from
// SmcGuard to SmcCodeGenerator.
// Corrected indentation for "loopbackFlag =" statement.
// Declaring "boolean loopbackFlag" only if and where it is needed.
//
// Revision 1.0  2005/02/03 17:11:27  charlesr
// Initial revision
//