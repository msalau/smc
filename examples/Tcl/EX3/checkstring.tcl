#!/bin/sh
# -*- tab-width: 4; -*-
# \
exec Tclsh80 "$0" "$@"

# 
# The contents of this file are subject to the Mozilla Public
# License Version 1.1 (the "License"); you may not use this file
# except in compliance with the License. You may obtain a copy of
# the License at http://www.mozilla.org/MPL/
# 
# Software distributed under the License is distributed on an "AS
# IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
# implied. See the License for the specific language governing
# rights and limitations under the License.
# 
# The Original Code is State Machine Compiler (SMC).
# 
# The Initial Developer of the Original Code is Charles W. Rapp.
# Portions created by Charles W. Rapp are
# Copyright (C) 2000 Charles W. Rapp.
# All Rights Reserved.
# 
# Contributor(s):
#
# checkstring --
#
#  This test program uses the state machine language to determine
#  is a string is of the form 0*1*.
#
# RCS ID
# $Id$
#
# CHANGE LOG
# $Log$
# Revision 1.4  2002/05/07 00:29:50  cwrapp
# Changes in release 1.3.2:
# Add the following feature:
# + 528321: Modified push transition syntax to be:
#
# 	  <transname> <state1>/push(<state2>)  {<actions>}
#
# 	  which means "transition to <state1> and then
# 	  immediately push to <state2>". The current
# 	  syntax:
#
# 	  <transname> push(<state2>)  {<actions>}
#
#           is still valid and <state1> is assumed to be "nil".
#
# No bug fixes.
#
# Revision 1.2  2001/10/12 14:28:04  cwrapp
# SMC v. 1.0.1
#
# Revision 1.1.1.1  2001/01/03 03:14:00  cwrapp
#
# ----------------------------------------------------------------------
# SMC - The State Map Compiler
# Version: 1.0, Beta 3
#
# SMC compiles state map descriptions into a target object oriented
# language. Currently supported languages are: C++, Java and [incr Tcl].
# SMC finite state machines have such features as:
# + Entry/Exit actions for states.
# + Transition guards
# + Transition arguments
# + Push and Pop transitions.
# + Default transitions. 
# ----------------------------------------------------------------------
#
# Revision 1.1.1.1  2000/08/02 12:51:05  charlesr
# Initial source import, SMC v. 1.0, Beta 1.
#

package require Itcl;
package require statemap;

namespace import ::itcl::*;
namespace import ::statemap::*;

source ./AppClass.tcl;

# Check if a string has been passed in.
if {[llength $argv] < 1} {
    puts stderr "No string to check.";
    set ErrorCode 1;
} else {
    AppClass mycontext;

    set ErrorCode 0;

    set InputString [lindex $argv 0];
    puts -nonewline stdout "The string \"$InputString\" is ";
    flush stdout;

    if {! [mycontext CheckString $InputString]} {
        puts -nonewline stdout "not ";
    }

    puts stdout "acceptable.";
}

exit $ErrorCode;
