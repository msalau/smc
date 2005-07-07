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
// Port to Python by Francois Perrad, francois.perrad@gadz.org
// Copyright 2004, Francois Perrad.
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
import java.util.StringTokenizer;

/**
 * Visits the abstract syntax tree, emitting Ruby code.
 * @see SmcElement
 * @see SmcCodeGenerator
 * @see SmcVisitor
 *
 * @author Francois Perrad
 */

public final class SmcRubyGenerator
    extends SmcCodeGenerator
{
//---------------------------------------------------------------
// Member methods
//

    public SmcRubyGenerator(PrintStream source,
                              String srcfileBase)
    {
        super (source, srcfileBase);

        _indent = "";
    }

    public void visit(SmcFSM fsm)
    {
        String packageName = fsm.getPackage();
        String context = fsm.getContext();
        String rawSource = fsm.getSource();
        String startState = fsm.getStartState();
        List maps = fsm.getMaps();
        List transitions;
        List params;
        SmcMap map;
        SmcTransition trans;
        SmcParameter param;
        String transName;
        int packageDepth = 0;
        int index;
        Iterator it;
        Iterator it2;
        Comparator comparator =
            new Comparator() {
                public int compare(Object o1,
                                   Object o2)
                {
                    return(
                        ((SmcTransition) o1).compareTo(
                            (SmcTransition) o2));
                }
            };

        // Dump out the raw source code, if any.
        if (rawSource != null && rawSource.length () > 0)
        {
            _source.println(rawSource);
            _source.println();
        }

        // If a package has been specified, then output that
        // module now. If the package name is "a::b::c", then
        // this must be converted to:
        // module a 
        //   module b 
        //     module c
        //       ...
        //     end
        //   end
        // end
        _indent = "";
        if (packageName != null && packageName.length() > 0)
        {
            StringTokenizer tokenizer =
                new StringTokenizer(packageName, "::");
            String token;

            while (tokenizer.hasMoreTokens() == true)
            {
                token = tokenizer.nextToken();
                ++packageDepth;

                _source.print(_indent);
                _source.print("module ");
                _source.println(token);
                _source.println();
                _indent += "    ";
            }
        }
        _source.print(_indent);
        _source.println("require 'statemap'");

        // Do user-specified imports now.
        for (it = fsm.getImports().iterator();
             it.hasNext() == true;
            )
        {
            _source.print(_indent);
            _source.print("require '");
            _source.print(it.next());
            _source.println("'");
        }

        // Declare the inner state class.
        _source.println();
        _source.print(_indent);
        _source.print("class ");
        _source.print(context);
        _source.println("State < Statemap::State");
        _source.println();

        _source.print(_indent);
        _source.println("    def Entry(fsm) end");
        _source.println();
        _source.print(_indent);
        _source.println("    def Exit(fsm) end");
        _source.println();

        // Get the transition list.
        // Generate the default transition definitions.
        transitions = (List) new ArrayList();
        for (it = maps.iterator(); it.hasNext() == true;)
        {
            map = (SmcMap) it.next();

            // Merge the new transitions into the current set.
            transitions =
                Smc.merge(map.getTransitions(),
                          transitions,
                          comparator);
        }

        for (it = transitions.iterator(); it.hasNext() == true;)
        {
            trans = (SmcTransition) it.next();
            params = trans.getParameters();

            // Don't generate the Default transition here.
            if (trans.getName().equals("Default") == false)
            {
                _source.print(_indent);
                _source.print("    def ");
                _source.print(trans.getName());
                _source.print("(fsm");

                for (it2 = params.iterator();
                     it2.hasNext() == true;
                    )
                {
                    param = (SmcParameter) it2.next(); 
                    _source.print(", ");
                    _source.print(param.getName());
                }

                _source.println(")");

                // If this method is reached, that means that this
                // transition was passed to a state which does not
                // define the transition. Call the state's default
                // transition method.
                _source.print(_indent);
                _source.println("        Default(fsm)");

                _source.print(_indent);
                _source.println("    end");
                _source.println();
            }
        }

        // Generate the overall Default transition for all maps.
        _source.print(_indent);
        _source.println("    def Default(fsm)");

        if (Smc.isDebug() == true)
        {
            _source.print(_indent);
            _source.println(
                "        if fsm.getDebugFlag then");
            _source.print(_indent);
            _source.println(
                "            fsm.getDebugStream.write(\"TRANSITION   : Default\\n\")");
            _source.print(_indent);
            _source.println("        end");
        }

        _source.print(_indent);
        _source.println(
            "        msg = \"\\nState: \" + fsm.getState.getName +");
        _source.print(_indent);
        _source.println(
            "            \"\\nTransition: \" + fsm.getTransition + \"\\n\"");
        _source.print(_indent);
        _source.println(
            "        raise Statemap::TransitionUndefinedException, msg");
        _source.print(_indent);
        _source.println("    end");

        // End of context class.
        _source.println();
        _source.print(_indent);
        _source.println("end");

        // Have each map print out its source code now.
        for (it = maps.iterator();  it.hasNext() == true;)
        {
            ((SmcMap) it.next()).accept(this);
        }

        // The context class contains all the state classes as
        // inner classes, so generate the context first rather
        // than last.
        _source.println();
        _source.print(_indent);
        _source.print("class ");
        _source.print(context);
        _source.println("_sm < Statemap::FSMContext");

        // Generate the context class' constructor.
        _source.println();
        _source.print(_indent);
        _source.println("    def initialize(owner)");
        _source.print(_indent);
        _source.println("        super()");
        _source.print(_indent);
        _source.println("        @_owner = owner");

        _source.print(_indent);
        _source.print("        setState(");
        _source.print(startState);
        _source.println(")");

        // Execute the start state's entry actions.
        _source.print(_indent);
        _source.print("        ");
        _source.print(startState);
        _source.println(".Entry(self)");

        _source.print(_indent);
        _source.println("    end");
        _source.println();

        // Generate the transition methods.
        for (it = transitions.iterator(); it.hasNext() == true;)
        {
            trans = (SmcTransition) it.next();
            transName = trans.getName();
            params = trans.getParameters();

            if (transName.equals("Default") == false)
            {
                _source.print(_indent);
                _source.print("    def ");
                _source.print(transName);
                if (params.size() != 0)
                {
                    _source.println("(*arglist)");    
                }
                else
                {
                    _source.println("()");    
                }

                // Save away the transition name in case it is
                // need in an UndefinedTransitionException.
                _source.print(_indent);
                _source.print("        @_transition = '");
                _source.print(transName);
                _source.println("'");

                _source.print(_indent);
                _source.print("        getState.");
                _source.print(transName);
                _source.print("(self");

                if (params.size() != 0)
                {
                    _source.print(", *arglist");    
                }
                _source.println(")");
                _source.print(_indent);
                _source.println("        @_transition = nil");

                _source.print(_indent);
                _source.println("    end");
                _source.println();
            }
        }

        // getState() method.
        _source.print(_indent);
        _source.println("    def getState()");
        _source.print(_indent);
        _source.println("        if @_state.nil? then");
        _source.print(_indent);
        _source.println(
            "            raise Statemap::StateUndefinedException");
        _source.print(_indent);
        _source.println("        end");
        _source.print(_indent);
        _source.println("        return @_state");
        _source.print(_indent);
        _source.println("    end");
        _source.println();

        // getOwner() method.
        _source.print(_indent);
        _source.println("    def getOwner()");
        _source.print(_indent);
        _source.println("        return @_owner");
        _source.print(_indent);
        _source.println("    end");
        _source.println();

        _source.print(_indent);
        _source.println("end");

        // If necessary, place an end for the module.
        if (packageName != null && packageName.length() > 0)
        {
            int i;
            int j;

            for (i = (packageDepth - 1); i >= 0; --i)
            {
                _source.println();
                // Output the proper indent.
                for (j = 0; j < i; ++j)
                {
                    _source.print("    ");
                }

                _source.println("end");
            }
        }

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
        String indent2;

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

        // Declare the map default state class.
        _source.println();           
        _source.print(_indent);
        _source.print("class ");
        _source.print(mapName);
        _source.print("_Default < ");
        _source.print(context);
        _source.println("State");

        // Declare the user-defined default transitions first.
        indent2 = _indent;
        _indent = _indent + "    ";
        for (it = definedDefaultTransitions.iterator();
             it.hasNext() == true;
            )
        {
            ((SmcTransition) it.next()).accept(this);
        }
        _indent = indent2;

        _source.println();
        _source.print(_indent);
        _source.println("end");

        // Have each state now generate its code. Each state
        // class is an inner class.
        for (it = states.iterator(); it.hasNext() == true;)
        {
            ((SmcState) it.next()).accept(this);
        }

        // Initialize the map.
        _source.println();
        _source.print(_indent);
        _source.print("module ");
        _source.println(mapName);
        _source.println();

        for (it = states.iterator(); it.hasNext() == true;)
        {
            state = (SmcState) it.next();

            _source.print(_indent);
            _source.print("    ");
            _source.print(state.getInstanceName());
            _source.print(" = ");
            _source.print(mapName);
            _source.print('_');
            _source.print(state.getClassName());
            _source.print("::new('");
            _source.print(mapName);
            _source.print('.');
            _source.print(state.getClassName());
            _source.print("', ");
            _source.print(map.getNextStateId());
            _source.println(").freeze");
        }

        // Instantiate a default state as well.
        _source.print(_indent);
        _source.print("    Default = ");
        _source.print(mapName);
        _source.print("_Default::new('");
        _source.print(mapName);
        _source.println(".Default', -1).freeze");

        _source.println();
        _source.print(_indent);
        _source.println("end");

        return;
    }

    public void visit(SmcState state)
    {
        SmcMap map = state.getMap();
        String mapName = map.getName();
        String stateName = state.getClassName();
        List actions;
        String indent2;
        Iterator it;

        // Declare the inner state class.
        _source.println();
        _source.print(_indent);
        _source.print("class ");
        _source.print(mapName);
        _source.print('_');
        _source.print(stateName);
        _source.print(" < ");
        _source.print(mapName);
        _source.println("_Default");

        // Add the Entry() and Exit() member functions if this
        // state defines them.
        actions = state.getEntryActions();
        if (actions != null && actions.size() > 0)
        {
            _source.println();
            _source.print(_indent);
            _source.println("    def Entry(fsm)");

            // Declare the "ctxt" local variable.
            _source.print(_indent);
            _source.println("        ctxt = fsm.getOwner");

            // Generate the actions associated with this code.
            indent2 = _indent;
            _indent = _indent + "        ";
            for (it = actions.iterator(); it.hasNext() == true;)
            {
                ((SmcAction) it.next()).accept(this);
            }
            _indent = indent2;

            // End the Entry() member function with a return.
            _source.print(_indent);
            _source.println("    end");
        }

        actions = state.getExitActions();
        if (actions != null && actions.size() > 0)
        {
            _source.println();
            _source.print(_indent);
            _source.println("    def Exit(fsm)");

            // Declare the "ctxt" local variable.
            _source.print(_indent);
            _source.println("        ctxt = fsm.getOwner");

            // Generate the actions associated with this code.
            indent2 = _indent;
            _indent = _indent + "        ";
            for (it = actions.iterator(); it.hasNext() == true;)
            {
                ((SmcAction) it.next()).accept(this);
            }
            _indent = indent2;

            // End the Exit() member function with a return.
            _source.print(_indent);
            _source.println("    end");
        }

        // Have each transition generate its code.
        indent2 = _indent;
        _indent = _indent + "    ";
        for (it = state.getTransitions().iterator();
             it.hasNext() == true;
            )
        {
            ((SmcTransition) it.next()).accept(this);
        }
        _indent = indent2;

        // End of this state class declaration.
        _source.println();
        _source.print(_indent);
        _source.println("end");

        return;
    }

    public void visit(SmcTransition transition)
    {
        SmcState state = transition.getState();
        SmcMap map = state.getMap();
        String packageName = map.getFSM().getPackage();
        String mapName = map.getName();
        String stateName = state.getClassName();
        String transName = transition.getName();
        List parameters = transition.getParameters();
        List guards = transition.getGuards();
        boolean nullCondition = false;
        Iterator it;
        SmcGuard guard;
        SmcParameter param;
        String indent2;

        _source.println();
        _source.print(_indent);
        _source.print("def ");
        _source.print(transName);
        _source.print("(fsm");

        // Add user-defined parameters.
        for (it = parameters.iterator(); it.hasNext() == true;)
        {
            param = (SmcParameter) it.next();
            _source.print(", ");
            _source.print(param.getName());
        }
        _source.println(")");

        // All transitions have a "ctxt" local variable.
        // 8/14/2003:
        // Do this only if there are any transition actions or
        // guard conditions which reference it.
        if (transition.hasCtxtReference() == true)
        {
            _source.print(_indent);
            _source.println("    ctxt = fsm.getOwner");
        }

        // Output transition to debug stream.
        if (Smc.isDebug() == true)
        {
            String sep;

            _source.print(_indent);
            _source.println(
                "    if fsm.getDebugFlag then");
            _source.print(_indent);
            _source.print(
                "        fsm.getDebugStream.write(\"TRANSITION   : ");
            if (packageName != null && packageName.length() > 0)
            {
                _source.print(packageName);
                _source.print("::");
            }
            _source.print(mapName);
            _source.print("::");
            _source.print(stateName);
            _source.print(".");
            _source.print(transName);

            if (parameters.size() != 0)
            {
                _source.print("(");
                for (it = parameters.iterator(), sep = "";
                     it.hasNext() == true;
                     sep = ", ")
                {
                    param = (SmcParameter) it.next(); 
                    _source.print(sep);
                    _source.print(param.getName());
                }
                _source.print(")");
            }

            _source.println("\\n\")");
            _source.print(_indent);
            _source.println("    end");
        }

        // Loop through the guards and print each one.
        indent2 = _indent;
        _indent = _indent + "    ";
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
        _indent = indent2;

        // If all guards have a condition, then create a final
        // "else" clause which passes control to the default
        // transition. Pass all arguments into the default
        // transition.
        if (_guardIndex > 0 && nullCondition == false)
        {
            _source.print(_indent);
            _source.println("    else");

            // Call the super class' transition method using
            // the "super" keyword and not the class name.
            _source.print(_indent);
            _source.println("        super");
            _source.print(_indent);
            _source.println("    end");
        }
        // Need to add a final newline after a multiguard block.
        else if (_guardCount > 1)
        {
            _source.print(_indent);
            _source.println("    end");
        }

        _source.print(_indent);
        _source.println("end");

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
            endStateName.equals(NIL_STATE) == false &&
            endStateName.indexOf("::") < 0)
        {
            endStateName = mapName + "::" + endStateName;
        }

        // Qualify the state and push state names as well.
        if (stateName.indexOf("::") < 0)
        {
            stateName = mapName + "::" + stateName;
        }

        // v. 2.0.2: If the push state is not fully-qualified,
        // then prepend the current map's name and make if
        // fully-qualified.
        if (pushStateName != null &&
            pushStateName.length() > 0)
        {
            if (pushStateName.indexOf("::") < 0) 
            {
                pushStateName = mapName + "::" + pushStateName;
            }
            else if (packageName != null && packageName.length() > 0)
            {
                pushStateName = packageName + "::" + pushStateName;
            }
        }

        loopbackFlag =
            isLoopback(transType, stateName, endStateName);

        // The guard code generation is a bit tricky. The first
        // question is how many guards are there? If there are
        // more than one, then we will need to generate the
        // proper "if-then-else" code.
        if (_guardCount > 1)
        {
            indent2 = _indent + "    ";

            // There are multiple guards. Is this the first guard?
            if (_guardIndex == 0 && condition.length() > 0)
            {
                // Yes, this is the first. This means an "if"
                // should be used.
                _source.print(_indent);
                _source.print("if ");
                _source.print(condition);
                _source.println(" then");
            }
            else if (condition.length() > 0)
            {
                // No, this is not the first transition but it
                // does have a condition. Use an "else if".
                _source.print(_indent);
                _source.print("elsif ");
                _source.print(condition);
                _source.println(" then");
            }
            else
            {
                // This is not the first transition and it has
                // no condition.
                _source.print(_indent);
                _source.println("else");
            }
        }
        // There is only one guard. Does this guard have
        // a condition?
        else if (condition.length() == 0)
        {
            // No. This is a plain, old. vanilla transition.
            indent2 = _indent;
        }
        else
        {
            // Yes there is a condition.
            indent2 = _indent + "        ";

            _source.print(_indent);
            _source.print("    if ");
            _source.print(condition);
            _source.println(" then");
        }

        // Now that the necessary conditions are in place, it's
        // time to dump out the transition's actions. First, do
        // the proper handling of the state change. If this
        // transition has no actions, then set the end state
        // immediately. Otherwise, unset the current state so
        // that if an action tries to issue a transition, it will
        // fail.
        if (actions.size() == 0 && endStateName.length() != 0)
        {
            fqEndStateName = endStateName;
        }
        else if (actions.size() > 0)
        {
            // Save away the current state if this is a loopback
            // transition. Storing current state allows the
            // current state to be cleared before any actions are
            // executed. Remember: actions are not allowed to
            // issue transitions and clearing the current state
            // prevents them from doing do.
            if (loopbackFlag == true)
            {
                fqEndStateName = "endState";

                _source.print(indent2);
                _source.print(fqEndStateName);
                _source.println(" = fsm.getState");
            }
            else
            {
                fqEndStateName = endStateName;
            }
        }

        // Decide if runtime loopback checking must be done.
        if (defaultFlag == true &&
            transType != Smc.TRANS_POP &&
            loopbackFlag == false)
        {
            _source.print(_indent);
            _source.print(
                "loopbackFlag = fsm.getState.getName == ");
            _source.print(fqEndStateName);
            _source.println(".getName");
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
                _source.println("unless loopbackFlag then");
            }

            _source.print(indent4);
            _source.println("fsm.getState.Exit(fsm)");

            if (transType == Smc.TRANS_SET &&
                defaultFlag == true)
            {
                _source.println(indent2 + "end");
            }
        }

        // Dump out this transition's actions.
        if (actions.size() == 0)
        {
            List entryActions = state.getEntryActions();
            List exitActions = state.getExitActions();

            if (condition.length() > 0)
            {
                _source.print(indent2);
                _source.println("# No actions.");
            }

            indent3 = indent2;
        }
        else
        {
            Iterator it;

            // Now that we are in the transition, clear the
            // current state.
            _source.print(indent2);
            _source.println("fsm.clearState");

            // v. 2.0.0: Place the actions inside a try/finally
            // block. This way the state will be set before an
            // exception leaves the transition method.
            // v. 2.2.0: Check if the user has turned off this
            // feature first.
            if (Smc.isNoCatch() == false)
            {
                _source.print(indent2);
                _source.println("begin");

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
                if (Smc.isDebug() == true)
                {
                    _source.print(indent2);
                    _source.println("rescue RuntimeError => e");
                    _source.print(indent2);
                    _source.println("    fsm.getDebugStream.write e");
                }

                _source.print(indent2);
                _source.println("ensure");
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
            _source.print("fsm.setState(");
            _source.print(fqEndStateName);
            _source.println(")");
        }
        else if (transType == Smc.TRANS_PUSH)
        {
            // Set the next state so this it can be pushed
            // onto the state stack. But only do so if a clear
            // state was done.
            if (loopbackFlag == false || actions.size() > 0)
            {
                _source.print(indent3);
                _source.print("fsm.setState(");
                _source.print(fqEndStateName);
                _source.println(")");
            }

            // Before doing the push, execute the end state's
            // entry actions (if any) if this is not a loopback.
            if (loopbackFlag == false)
            {
                if (defaultFlag == true)
                {
                    indent4 = indent3 + "    ";
                    _source.print(indent3);
                    _source.println("unless loopbackFlag then");
                }
                else
                {
                    indent4 = indent3;
                }

                _source.print(indent4);
                _source.println("fsm.getState.Entry(fsm)");

                if (defaultFlag == true)
                {
                    _source.print(indent3);
                    _source.println("end");
                }
            }

            _source.print(indent3);
            _source.print("fsm.pushState(");
            _source.print(pushStateName);
            _source.println(")");
        }
        else if (transType == Smc.TRANS_POP)
        {
            _source.print(indent3);
            _source.println("fsm.popState");
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

                _source.print(indent3);
                _source.println("unless loopbackFlag then");
            }

            _source.print(indent4);
            _source.println("fsm.getState.Entry(fsm)");

            if (transType == Smc.TRANS_SET &&
                defaultFlag == true)
            {
                _source.print(indent3);
                _source.println("end");
            }
        }

        // If there was a begin/rescue/ensure, then put the closing
        // end on the block.
        // v. 2.2.0: Check if the user has turned off this
        // feature first.
        if (actions.size() > 0 && Smc.isNoCatch() == false)
        {
            _source.print(indent2);
            _source.println("end");
        }

        // If there is a transition associated with the pop, then
        // issue that transition here.
        if (transType == Smc.TRANS_POP &&
            endStateName.equals(NIL_STATE) == false &&
            endStateName.length() > 0)
        {
            String popArgs = guard.getPopArgs();

            _source.print(indent2);
            _source.print("fsm.");
            _source.print(endStateName);

            // Output any and all pop arguments.
            if (popArgs.length() > 0)
            {
                _source.print("(");
                _source.print(popArgs);
                _source.println();
                _source.print(indent2);
                _source.print(")");
            }
            _source.println();
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
            _source.print("fsm.");
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

        _source.println(")");

        return;
    }

//---------------------------------------------------------------
// Member data
//
}

//
// CHANGE LOG
// $Log$
// Revision 1.1  2005/06/16 18:11:01  fperrad
// Added C, Perl & Ruby generators.
//
//