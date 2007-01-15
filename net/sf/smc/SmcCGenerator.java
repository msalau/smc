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
// Copyright (C) 2005. Charles W. Rapp.
// All Rights Reserved.
// 
// Contributor(s):
//   Eitan Suez contributed examples/Ant.
//   (Name withheld) contributed the C# code generation and
//   examples/C#.
//   Francois Perrad contributed the Python code generation and
//   examples/Python.
//   Chris Liscio contributed the Objective-C code generation
//   and examples/ObjC.
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
import java.util.Iterator;
import java.util.List;

/**
 * Visits the abstract syntax tree emitting C++ code.
 * @see SmcElement
 * @see SmcCodeGenerator
 * @see SmcVisitor
 *
 * @author Francois Perrad
 */

public final class SmcCGenerator
    extends SmcCodeGenerator
{
//---------------------------------------------------------------
// Member methods
//

    public SmcCGenerator(PrintStream source, String srcfileBase)
    {
        super (source, srcfileBase);

        _indent = "";
    }

    // This method generates the following code:
    //
    // %{ %} raw source code - if any
    //
    // #include <%include header file>
    // #include "<context>_sm.h"
    // (If the -headerd option is used, then this is generated:
    // #include "<header dir>/<context>_sm.h")
    //
    public void visit(SmcFSM fsm)
    {
        String srcDirectory = Smc.outputDirectory();
        String headerDirectory = Smc.headerDirectory();
        String packageName = fsm.getPackage();
        String rawSource = fsm.getSource();
        String context = fsm.getContext();
        String mapName;
        String startStateName = fsm.getStartState();
        List transList;
        String separator;
        List params;
        Iterator it;
        Iterator mapIt;
        Iterator stateIt;
        Iterator transIt;
        Iterator pit;
        String declaration;
        String cState;
        int packageDepth = 0;
        SmcMap map;
        SmcState state;
        SmcTransition trans;
        SmcParameter param;
        int index;

        // If a package has been specified, 
        if (packageName != null && packageName.length() > 0)
        {
            context = packageName + "_" + context;
            startStateName = packageName + "_" + startStateName;
        }

        // Dump out the raw source code, if any.
        if (rawSource != null && rawSource.length() > 0)
        {
            _source.println(rawSource);
            _source.println();
        }

        _source.println("#include <assert.h>");

        // Generate #includes.
        for (it = fsm.getIncludes().iterator();
             it.hasNext() == true;
            )
        {
            _source.print("#include ");
            _source.println(((String) it.next()));
        }

        // Include the context file last.
        // Is the header file included in a different directory
        // than the source file?
        _source.print("#include \"");
        if ((srcDirectory == null && headerDirectory != null) ||
            (srcDirectory != null &&
             srcDirectory.equals(headerDirectory) == false))
        {
            // They are in different directories. Prepend the
            // header directory to the file name.
            _source.print(headerDirectory);
        }
        // Else they are in the same directory.
        _source.print(_srcfileBase);
        _source.println("_sm.h\"");

        // Print out the default definitions for all the
        // transitions. First, get the transitions list.
        transList = fsm.getTransitions();

        _source.println();
        _source.println("#define getOwner(fsm) \\");
        _source.println("    (fsm)->_owner");
        _source.println();

        _source.println("#define POPULATE_STATE(state) \\");
        _source.println("    state##_Entry, \\");
        _source.println("    state##_Exit, \\");
        for (transIt = transList.iterator();
             transIt.hasNext() == true;
            )
        {
            trans = (SmcTransition) transIt.next();

            if (trans.getName().equals("Default") == false)
            {
                _source.print("    state##_");
                _source.print(trans.getName());
                _source.println(", \\");
            }
        }
        _source.println("    state##_Default");

        // Output the default transition definitions.
        for (transIt = transList.iterator();
             transIt.hasNext() == true;
            )
        {
            trans = (SmcTransition) transIt.next();

            if (trans.getName().equals("Default") == false)
            {
                _source.println();
                _source.print("static void ");
                _source.print(context);
                _source.print("State_");
                _source.print(trans.getName());
                _source.print("(struct ");
                _source.print(context);
                _source.print("Context *fsm");

                params = trans.getParameters();
                for (pit = params.iterator();
                     pit.hasNext() == true;
                    )
                {
                    _source.print(", ");
                    ((SmcParameter) pit.next()).accept(this);
                }

                _source.println(")");
                _source.println("{");
                _source.println(
                    "    getState(fsm)->Default(fsm);");
                _source.println("}");
            }
        }

        _source.println();
        _source.print("static void ");
        _source.print(context);
        _source.print("State_Default(struct ");
        _source.print(context);
        _source.println("Context *fsm)");
        _source.println("{");

        // Print the transition out to the verbose log.
        if (Smc.isDebug() == true)
        {
            _source.println("    if (getDebugFlag(fsm) != 0)");
            _source.println("    {");

            // The TRACE macro.
            _source.print("        TRACE(");
            _source.print("\"TRANSITION   : %s.%s\\n\\r\", ");
            _source.println(
                "getName(getState(fsm)), getTransition(fsm));");

            _source.println("    }");
        }
        _source.println("    State_Default(fsm);");
        _source.println("}");

        // Have each map print out its source code now.
        for (mapIt = fsm.getMaps().iterator();
             mapIt.hasNext() == true;
            )
        {
            map = (SmcMap) mapIt.next();
            mapName = map.getName();
            if (packageName != null && packageName.length() > 0)
            {
                mapName = packageName + "_" + mapName;
            }
            _source.println();

            for (stateIt = map.getStates().iterator();
                 stateIt.hasNext() == true;
                )
            {
                state = (SmcState) stateIt.next();

                for (transIt = transList.iterator();
                     transIt.hasNext() == true;
                    )
                {
                    trans = (SmcTransition) transIt.next();

                    if (trans.getName().equals(
                            "Default") == false)
                    {
                        _source.print("#define ");
                        _source.print(mapName); 
                        _source.print("_"); 
                        _source.print(state.getClassName()); 
                        _source.print("_"); 
                        _source.print(trans.getName()); 
                        _source.print(" "); 
                        _source.print(context); 
                        _source.print("State_"); 
                        _source.println(trans.getName());
                    }
                }
                
                _source.print("#define ");
                _source.print(mapName); 
                _source.print("_"); 
                _source.print(state.getClassName()); 
                _source.print("_Default "); 
                _source.print(context); 
                _source.println("State_Default");
                _source.print("#define ");
                _source.print(mapName); 
                _source.print("_"); 
                _source.print(state.getClassName()); 
                _source.println("_Entry NULL");
                _source.print("#define ");
                _source.print(mapName); 
                _source.print("_"); 
                _source.print(state.getClassName()); 
                _source.println("_Exit NULL");
            }
            
            for (transIt = transList.iterator();
                 transIt.hasNext() == true;
                )
            {
                trans = (SmcTransition) transIt.next();

                if (trans.getName().equals("Default") == false)
                {
                    _source.print("#define ");
                    _source.print(mapName); 
                    _source.print("_Default_"); 
                    _source.print(trans.getName()); 
                    _source.print(" "); 
                    _source.print(context); 
                    _source.print("State_"); 
                    _source.println(trans.getName());
                }
            }
            
            map.accept(this);
        }

        // The state name "map::state" must be changed to
        // "map_state".
        if ((index = startStateName.indexOf("::")) >= 0)
        {
            cState =
                    startStateName.substring(0, index) +
                    "_" +
                startStateName.substring(index + 2);
        }
        else
        {
            cState = startStateName;
        }

        _source.println();
        _source.print("void ");
        _source.print(context);
        _source.print("Context_Init");
        _source.print("(struct ");
        _source.print(context);
        _source.print("Context* fsm, struct ");
        _source.print(context);
        _source.println("* owner)");
        _source.println("{");
        _source.println("    FSM_INIT(fsm);");
        _source.println("    fsm->_owner = owner;");
        _source.print("    setState(fsm, &");
        _source.print(cState);
        _source.println(");");
        _source.print("    if (");
        _source.print(cState);
        _source.println(".Entry != NULL) {");
        _source.print("        ");
        _source.print(cState);
        _source.println(".Entry(fsm);");
        _source.println("    }");
        _source.println("}");

        // Generate the context class.
        // Generate a method for every transition in every map
        // *except* the default transition.
        for (transIt = transList.iterator();
             transIt.hasNext() == true;
            )
        {
            trans = (SmcTransition) transIt.next();
            if (trans.getName().equals("Default") == false)
            {
                _source.println();
                _source.print("void ");
                _source.print(context);
                _source.print("Context_");
                _source.print(trans.getName());
                _source.print("(struct ");
                _source.print(context);
                _source.print("Context* fsm");

                params = trans.getParameters();
                for (pit = params.iterator();
                     pit.hasNext() == true;
                    )
                {
                    param = (SmcParameter) pit.next();

                    _source.print(", ");
                    _source.print(param.getType());
                    _source.print(" ");
                    _source.print(param.getName());
                }
                _source.println(")");
                _source.println("{");

                _source.print("    const struct ");
                _source.print(context);
                _source.println("State* state = getState(fsm);");
                _source.println();

                _source.println("    assert(state != NULL);");
                _source.print("    setTransition(fsm, \"");
                _source.print(trans.getName());
                _source.println("\");");
                _source.print("    state->");
                _source.print(trans.getName());
                _source.print("(fsm");
                for (pit = params.iterator();
                     pit.hasNext() == true;
                    )
                {
                    param = (SmcParameter) pit.next();

                    _source.print(", ");
                    _source.print(param.getName());
                }
                _source.println(");");
                _source.println("    setTransition(fsm, NULL);");

                _source.println("}");
            }
        }

        return;
    }

    public void visit(SmcMap map)
    {
        String packageName = map.getFSM().getPackage();
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        String transName;
        String stateName;
        Iterator it;
        Iterator stateIt;
        SmcTransition trans;
        SmcState state;

        // If a package has been specified, 
        if (packageName != null && packageName.length() > 0)
        {
            context = packageName + "_" + context;
            mapName = packageName + "_" + mapName;
        }

        // Declare the user-defined default transitions first.
        if (map.hasDefaultState() == true)
        {
            SmcState defaultState = map.getDefaultState();

            for (it = defaultState.getTransitions().iterator();
                 it.hasNext() == true;
                )
            {
                trans = (SmcTransition) it.next();
                transName = trans.getName();

                _source.println();

                for (stateIt = map.getStates().iterator();
                     stateIt.hasNext() == true;
                    )
                {
                    state = (SmcState) stateIt.next();
                    stateName = state.getClassName();

                    _source.print("#undef ");
                    _source.print(mapName); 
                    _source.print("_");
                    _source.print(stateName);
                    _source.print("_");
                    _source.println(transName);
                    _source.print("#define ");
                    _source.print(mapName); 
                    _source.print("_");
                    _source.print(stateName);
                    _source.print("_");
                    _source.print(transName);
                    _source.print(" ");
                    _source.print(mapName); 
                    _source.print("_Default_");
                    _source.println(transName);
                }

                _source.print("#undef ");
                _source.print(mapName); 
                _source.print("_Default_");
                _source.println(transName);

                trans.accept(this);
            }
        }

        // Have each state now generate its code.
        for (it = map.getStates().iterator();
             it.hasNext() == true;
            )
        {
            state = (SmcState) it.next();
            state.accept(this);

            _source.println();
            _source.print("const struct ");
            _source.print(context);
            _source.print("State ");
            _source.print(mapName);
            _source.print("_");
            _source.print(state.getClassName());
            _source.print(" = { POPULATE_STATE(");
            _source.print(mapName);
            _source.print("_");
            _source.print(state.getClassName());
            _source.print("), \"");
            _source.print(mapName);
            _source.print("_");
            _source.print(state.getClassName());
            _source.print("\", ");
            _source.print(
                Integer.toString(map.getNextStateId()));
            _source.println(" };");
        }

        return;
    }

    public void visit(SmcState state)
    {
        SmcMap map = state.getMap();
        String packageName = map.getFSM().getPackage();
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        String className = state.getClassName();
        String indent2;
        List actions;
        Iterator it;

        // If a package has been specified, 
        if (packageName != null && packageName.length() > 0)
        {
            context = packageName + "_" + context;
            mapName = packageName + "_" + mapName;
        }

        _context = context;

        // Add the Entry() and Exit() methods if this state
        // defines them.
        actions = state.getEntryActions();
        if (actions != null && actions.size() > 0)
        {
            _source.println();
            _source.print("#undef ");
            _source.print(mapName);
            _source.print("_");
            _source.print(className);
            _source.println("_Entry");
            _source.print("void ");
            _source.print(mapName);
            _source.print("_");
            _source.print(className);
            _source.print("_Entry(struct ");
            _source.print(context);
            _source.println("Context *fsm)");
            _source.println("{");

            // Declare the "ctxt" local variable.
            _source.print("    struct ");
            _source.print(context);
            _source.println(" *ctxt = getOwner(fsm);");
            _source.println();

            // Generate the actions associated with this code.
            indent2 = _indent;
            _indent = _indent + "    ";
            for (it = actions.iterator(); it.hasNext() == true;)
            {
                ((SmcAction) it.next()).accept(this);
            }
            _indent = indent2;

            // End the Entry() method.
            _source.println("}");
        }

        actions = state.getExitActions();
        if (actions != null && actions.size() > 0)
        {
            _source.println();
            _source.print("#undef ");
            _source.print(mapName);
            _source.print("_");
            _source.print(className);
            _source.println("_Exit");
            _source.print("void ");
            _source.print(mapName);
            _source.print("_");
            _source.print(className);
            _source.print("_Exit(struct ");
            _source.print(context);
            _source.println("Context *fsm)");
            _source.println("{");

            // Declare the "ctxt" local variable.
            _source.print("    struct ");
            _source.print(context);
            _source.println(" *ctxt = getOwner(fsm);");
            _source.println();

            // Generate the actions associated with this code.
            indent2 = _indent;
            _indent = _indent + "    ";
            for (it = actions.iterator(); it.hasNext() == true;)
            {
                ((SmcAction) it.next()).accept(this);
            }
            _indent = indent2;

            // End the Entry() method.
            _source.println("}");
        }

        // Have the transitions generate their code.
        for (it = state.getTransitions().iterator();
             it.hasNext() == true;
            )
        {
            ((SmcTransition) it.next()).accept(this);
        }

        return;
    }

    public void visit(SmcTransition transition)
    {
        SmcState state = transition.getState();
        SmcMap map = state.getMap();
        String packageName = map.getFSM().getPackage();
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        String stateName = state.getClassName();
        String transName = transition.getName();
        boolean defaultFlag = false;
        boolean nullCondition = false;
        List guards = transition.getGuards();
        Iterator git;
        SmcGuard guard;
        SmcParameter param;
        Iterator pit;

        // If a package has been specified, 
        if (packageName != null && packageName.length() > 0)
        {
            context = packageName + "_" + context;
            mapName = packageName + "_" + mapName;
        }

        // Set a flag to denote if this is a Default state
        // transition.
        if (stateName.equals("Default") == true)
        {
            defaultFlag = true;
        }

        if (defaultFlag == false) 
        {
            _source.println();
            _source.print("#undef ");
            _source.print(mapName);
            _source.print("_");
            _source.print(stateName);
            _source.print("_");
            _source.println(transName);
        }

        _source.print("static void ");
        _source.print(mapName);
        _source.print("_");
        _source.print(stateName);
        _source.print("_");
        _source.print(transName);
        _source.print("(struct ");
        _source.print(context);
        _source.print("Context *fsm");

        // Add user-defined parameters.
        for (pit = transition.getParameters().iterator();
             pit.hasNext() == true;
            )
        {
            _source.print(", ");
            ((SmcParameter) pit.next()).accept(this);
        }

        _source.println(")");
        _source.println("{");

        // All transitions have a "ctxt" local variable.
        // 8/14/2003:
        // Do this only if there are any transition actions or
        // guard conditions which reference it.
        if (transition.hasCtxtReference() == true)
        {
            _source.print("    struct ");
            _source.print(context);
            _source.println("* ctxt = getOwner(fsm);");
        }

        if (defaultFlag == true)
        {
            _source.println("    int loopbackFlag = 0;");
        }

        // ANSI C requires all local variables be declared
        // at the code block's start before any control
        // statements. If this transition appears only once
        // in the state, has at least one action and it is a
        // loopback and debugging is on, then visit(SmcGuard)
        // will generate a local variable declaration after the
        // debug if clause - an ANSI syntax error.
        // So we need to check if this transition meets that
        // condition and generate the local variable declaration
        // here rather than in visit(SmcGuard).
        //
        // Note: when guard count is > 1, then the guard code
        // is placed into an if or else block - and so the
        // end state variable will appear at the start of that
        // block, nullifying the debug if clauses affect.
        _guardCount = guards.size();
        if (_guardCount == 1)
        {
            guard = (SmcGuard) guards.get(0);

            if (guard.getActions().isEmpty() == false &&
                isLoopback(guard.getTransType(),
                           stateName,
                           guard.getEndState()) == true)
            {
                _source.print(_indent);
                _source.print("const struct ");
                _source.print(context);
                _source.println(
                    "State* EndStateName = getState(fsm);");
            }
        }

        _source.println();

        // Print the transition to the verbose log.
        if (Smc.isDebug() == true)
        {
            String sep;

            _source.println("    if (getDebugFlag(fsm) != 0)");
            _source.println("    {");
            _source.print("        TRACE(\"TRANSITION   : ");
            _source.print(mapName);
            _source.print("_");
            _source.print(stateName);
            _source.print(".");
            _source.print(transName);
            _source.print("(");

            for (pit = transition.getParameters().iterator(),
                     sep = "";
                 pit.hasNext() == true;
                 sep = ", ")
            {
                param = (SmcParameter) pit.next(); 
                _source.print(sep);
                _source.print(param.getName());
            }

            _source.println(")\\n\\r\");");

            _source.println("    }");
        }

        // Loop through the guards and print each one.
        for (git = guards.iterator(), _guardIndex = 0;
             git.hasNext() == true;
             ++_guardIndex)
        {
            guard = (SmcGuard) git.next();

            // Count up the number of guards with no condition.
            if (guard.getCondition().length() == 0)
            {
                nullCondition = true;
            }

            guard.accept(this);
        }

        // If all guards have a condition, then create a final
        // "else" clause which passes control to the default
        // transition.
        if (_guardIndex > 0 && nullCondition == false)
        {
            // If there is only one transition definition, then
            // close off the guard.
            if (_guardCount == 1)
            {
                _source.println("    }");
            }

            _source.println(" else {");
            _source.print("        ");
            _source.print(mapName);
            _source.print("_Default_");
            _source.print(transName);
            _source.print("(fsm");

            // Output user-defined parameters.
            for (pit = transition.getParameters().iterator();
                 pit.hasNext() == true;
                )
            {
                _source.print(", ");
                _source.print(
                    ((SmcParameter) pit.next()).getName());
            }
            _source.println(");");
            _source.println("    }");
        }
        else if (_guardCount > 1)
        {
            _source.println();
        }

        _source.println("}");

        return;
    }

    public void visit(SmcGuard guard)
    {
        SmcTransition transition = guard.getTransition();
        SmcState state = transition.getState();
        SmcMap map = state.getMap();
        String packageName = map.getFSM().getPackage();
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        String stateName = state.getClassName();
        int transType = guard.getTransType();
        boolean defaultFlag =
            stateName.equalsIgnoreCase("Default");
        boolean loopbackFlag = false;
        String indent2;
        String indent3;
        String indent4 = "";
        String endStateName = guard.getEndState();
        String fqEndStateName = "";
        String pushStateName = guard.getPushState();
        String condition = guard.getCondition();
        List actions = guard.getActions();

        // If a package has been specified, 
        if (packageName != null && packageName.length() > 0)
        {
            context = packageName + "_" + context;
            mapName = packageName + "_" + mapName;
        }

        _context = context;

        // If this guard's end state is not of the form
        // "map::state", then prepend the map name to the state
        // name.
        // DON'T DO THIS IF THIS IS A POP TRANSITION!
        // The "state" is actually a transition name.
        if (transType != Smc.TRANS_POP &&
            endStateName.length () > 0 &&
            endStateName.equals(NIL_STATE) == false)
        {
            endStateName =
                "&" + scopeStateName(endStateName, mapName);
        }

        // Qualify the state and push state names as well.
        stateName = "&" + scopeStateName(stateName, mapName);
        pushStateName = scopeStateName(pushStateName, mapName);
        if (packageName != null && packageName.length() > 0)
        {
            pushStateName =
                "&" + packageName + "_" + pushStateName;
        }
        else 
        {
            pushStateName = "&" + pushStateName;
        }

        loopbackFlag =
            isLoopback(transType, stateName, endStateName);

        // The guard code generation is a bit tricky. The first
        // question is how many guards are there? If there are
        // more than one, then we will need to generate the
        // proper "if-then-else" code.
        if (_guardCount > 1)
        {
            indent2 = _indent + "        ";

            // More than one guard. Is this the first guard?
            if (_guardIndex == 0 && condition.length() > 0)
            {
                // Yes, this is the first. This means an
                // "if" should be used for this condition.
                _source.print(_indent);
                _source.print("    if (");
                _source.print(condition);
                _source.println(") {");
            }
            else if (condition.length() > 0)
            {
                // No, this is not the first transition but it
                // does have a condition. Use an "else if" for
                // the condition.
                _source.print(" else if (");
                _source.print(condition);
                _source.println(") {");
            }
            else
            {
                // This is not the first transition and it has
                // no condition.
                _source.println(" else {");
            }
        }
        else
        {
            // There is only one guard. Does this guard have a
            // condition.
            if (condition.length() == 0)
            {
                // Actually, this is a plain, old, vaniila
                // transition.
                indent2 = _indent + "    ";
            }
            else
            {
                // Yes, there is a condition.
                _source.print(_indent);
                _source.print("    if (");
                _source.print(condition);
                _source.println(") {");
                indent2 = _indent + "        ";
            }
        }

        // Now that the necessary conditions are in place, it's
        // time to dump out the transitions actions. First, do
        // the proper handling of the state change. If this
        // transition has no actions, then set the end state
        // immediately. Otherwise, unset the current state so
        // that if an action tries to issue a transition, it will
        // fail.
        if (actions.size() == 0 && endStateName.length() > 0)
        {
            fqEndStateName = endStateName;
        }
        else if (actions.size() > 0)
        {
            // Save away the current state if this is a loopback
            // transition. Storing current state allows the
            // current to be cleared before any actions are
            // executed. Remember: actions are not allowed to
            // issue transitions and clearing the current state
            // prevents them from doing so.
            if (loopbackFlag == true)
            {
                fqEndStateName = "EndStateName";

                // Generate this declaration only if this
                // transition has multiple guards. If this
                // is the only guard, then this local variable
                // declaration will appear after the first
                // control statement - which is an ANSI C
                // syntax error.
                // If there is only one guard, then this code
                // is generated by visit(SmcTransition) before
                // the debug code is generated.
                // If there are multiple guards, then this code
                // appears at the start of an if, else if or else
                // code block which is acceptable ANSI C.
                if (_guardCount > 1)
                {
                    _source.print(indent2);
                    _source.print("const struct ");
                    _source.print(context);
                    _source.print("State* ");
                    _source.print(fqEndStateName);
                    _source.println(" = getState(fsm);");
                    _source.println();
                }
            }
            else
            {
                fqEndStateName = endStateName;
            }
        }

        // Decide if runtime loopback checking must be done.
        if (defaultFlag == true &&
            transType == Smc.TRANS_SET &&
            loopbackFlag == false)
        {
            _source.print(_indent);
            _source.print(
                "    if (strcmp(getName(getState(fsm)), ");
            _source.print("getName(");
            _source.print(endStateName);
            _source.println(")) == 0) {");
            _source.print(_indent);
            _source.println("        loopbackFlag = 1;");
            _source.print(_indent);
            _source.println("    }");
        }

        // Before doing anything else, perform the current
        // state's exit actions.
        // v. 1.0, beta 3: Not any more. The exit actions are
        // executed only if 1) this is a standard, non-loopback
        // transition or a pop transition.
        if (transType == Smc.TRANS_POP ||
            loopbackFlag == false)
        {
            indent4 = indent2;

            // If this is a non-loopback, generic transition,
            // do runtime loopback checking.
            if (transType == Smc.TRANS_SET &&
                defaultFlag == true)
            {
                indent4 = indent2 + "    ";
                _source.print(indent2);
                _source.println(
                    "if (loopbackFlag == 0) {");
            }

            _source.print(indent4);
            _source.println(
                "if (getState(fsm)->Exit != NULL) {");
            _source.print(indent4);
            _source.println(
                "    getState(fsm)->Exit(fsm);");
            _source.print(indent4);
            _source.println("}");

            if (transType == Smc.TRANS_SET &&
                defaultFlag == true)
            {
                _source.print(indent2);
                _source.println("}");
            }
        }

        if (actions.size() > 0)
        {
            // Now that we are in the transition, clear the
            // current state.
            _source.print(indent2);
            _source.println("clearState(fsm);");
        }

        // Dump out this transition's actions.
        if (actions.isEmpty() == true)
        {
            if (condition.length() > 0)
            {
                _source.print(indent2);
                _source.println("/* No actions. */");
            }
        }
        else
        {
            Iterator ait;

            indent4 = _indent;
            _indent = indent2;
            for (ait = actions.iterator();
                 ait.hasNext() == true;
                )
            {
                ((SmcAction) ait.next()).accept(this);
            }
            _indent = indent4;
        }
        indent3 = indent2;

        // Print the setState() call, if necessary. Do NOT
        // generate the set state if:
        // 1. The transition has no actions AND is a loopback OR
        // 2. This is a push or pop transition.
        if (transType == Smc.TRANS_SET &&
            (actions.isEmpty() == false ||
             loopbackFlag == false))
        {
            _source.print(indent3);
            _source.print("setState(fsm, ");
            _source.print(fqEndStateName);
            _source.println(");");
        }
        else if (transType == Smc.TRANS_PUSH)
        {
            // Set the end state so that it can be pushed
            // onto the state stack. But only do so if a clear
            // state was done.
            if (loopbackFlag == false ||
                actions.isEmpty() == false)
            {
                _source.print(indent3);
                _source.print("setState(fsm, ");
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
                    _source.println("if (loopbackFlag == 0) {");
                }
                else
                {
                    indent4 = indent3;
                }

                _source.print(indent4);
                _source.println(
                    "if (getState(fsm)->Entry != NULL) {");
                _source.print(indent4);
                _source.println(
                    "    getState(fsm)->Entry(fsm);");
                _source.print(indent4);
                _source.println("}");

                if (defaultFlag == true)
                {
                    _source.print(indent3);
                    _source.println("}");
                }
            }

            _source.print(indent3);
            _source.print("pushState(fsm, ");
            _source.print(pushStateName);
            _source.println(");");
        }
        else if (transType == Smc.TRANS_POP)
        {
            _source.print(indent3);
            _source.println("popState(fsm);");
        }

        // Perform the new state's entry actions.
        // v. 1.0, beta 3: Not any more. The entry actions are
        // executed only if 1) this is a standard, non-loopback
        // transition or a push transition.
        if ((transType == Smc.TRANS_SET &&
             loopbackFlag == false) ||
             transType == Smc.TRANS_PUSH)
        {
            // If this is a non-loopback, generic transition,
            // do runtime loopback checking.
            if (transType == Smc.TRANS_SET &&
                defaultFlag == true)
            {
                indent4 = indent2 + "    ";

                _source.print(indent2);
                _source.println("if (loopbackFlag == 0) {");
            }
            else
            {
                indent4 = indent2;
            }

            _source.print(indent4);
            _source.println(
                "if (getState(fsm)->Entry != NULL) {");
            _source.print(indent4);
            _source.println(
                "    getState(fsm)->Entry(fsm);");
            _source.print(indent4);
            _source.println("}");

            if (transType == Smc.TRANS_SET &&
                defaultFlag == true)
            {
                _source.print(indent2);
                _source.println("}");
            }
        }

        // If there is a transition associated with the pop, then
        // issue that transition here.
        if (transType == Smc.TRANS_POP &&
            endStateName.equals(NIL_STATE) == false &&
            endStateName.length() > 0)
        {
            String popArgs = guard.getPopArgs();

            _source.print(indent2);
            _source.print(context);
            _source.print("Context_");
            _source.print(endStateName);
            _source.print("(fsm");

            // Output any and all pop arguments.
            if (popArgs.length() > 0)
            {
                _source.print(", ");
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

        // Need to distinguish between FSMContext actions and
        // application class actions. If the action is
        // "emptyStateStack", then pass it to the context.
        // Otherwise, let the application class handle it.
        _source.print(_indent);
        if (name.equals("emptyStateStack") == true)
        {
            _source.print(name);
            _source.print("(fsm");
        }
        else
        {
            _source.print(_context);
            _source.print("_");
            _source.print(name);
            _source.print("(ctxt");
        }

        for (it = action.getArguments().iterator();
             it.hasNext() == true;
             )
        {
            String arg = (String)it.next();
            if (arg.equals("") == false)
            {
                _source.print(", ");
                _source.print(arg);
            }
        }

        _source.println(");");

        return;
    }

    public void visit(SmcParameter parameter)
    {
        _source.print(parameter.getType());
        _source.print(" ");
        _source.print(parameter.getName());

        return;
    }

    // Scope the state name. If the state is unscoped, then
    // return "<mapName>.<stateName>". If the state named
    // contains the scope string "::", replace that with a "_".
    protected String scopeStateName(String stateName,
                                    String mapName)
    {
        int index;
        StringWriter retval = new StringWriter();

        index = stateName.indexOf("::");
        if (index < 0)
        {
            retval.write(mapName);
            retval.write("_");
            retval.write(stateName);
        }
        else
        {
            retval.write(stateName.substring(0, index));
            retval.write('_');
            retval.write(stateName.substring(index + 2));
        }

        return (retval.toString());
    }

//---------------------------------------------------------------
// Member data
//
    String _context;
}

//
// CHANGE LOG
// $Log$
// Revision 1.6  2007/01/15 00:23:50  cwrapp
// Release 4.4.0 initial commit.
//
// Revision 1.5  2006/09/16 15:04:28  cwrapp
// Initial v. 4.3.3 check-in.
//
// Revision 1.4  2006/07/11 18:11:41  cwrapp
// Added support for new -headerd command line option.
//
// Revision 1.3  2006/04/22 12:45:26  cwrapp
// Version 4.3.1
//
// Revision 1.2  2005/11/07 19:34:54  cwrapp
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
// Revision 1.1  2005/06/16 18:11:01  fperrad
// Added C, Perl & Ruby generators.
//
//
