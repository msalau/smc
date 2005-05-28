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
// Copyright (C) 2000 - 2004. Charles W. Rapp.
// All Rights Reserved.
// 
// Contributor(s): 
//
// Function
//	Main
//
// Description
//  This routine starts the finite state machine running.
//
// RCS ID
// $Id$
//
// CHANGE LOG
// $Log$
// Revision 1.4  2005/05/28 13:31:18  cwrapp
// Updated C++ examples.
//
// Revision 1.1  2004/09/06 15:29:51  charlesr
// Updated for SMC v. 3.1.0.
//
// Revision 1.0  2003/12/14 19:20:14  charlesr
// Initial revision
//

#if (__GNUC__ >= 3 && __GNUC_MINOR__ >= 1)
#include <iostream>
#include <cassert>
#include <cstdio>
#elif defined(WIN32)
#include <iostream>
#include <windows.h>
#else
#include <iostream.h>
#endif
#include "AppClass.h"

const static char _rcs_id[] = "$Id$";

using namespace std;

int main(int argc, char *argv[])
{
	AppClass thisContext;
    int retcode = 0;

	if (argc < 2)
	{
		cerr << "No string to check." << endl;
        retcode = 2;
	}
    else if (argc > 2)
    {
        cerr << "Only one argument is accepted." << endl;
        retcode = 3;
    }
	else
	{
		cout << "The string \"" << argv[1] << "\" is ";

		if (thisContext.CheckString(argv[1]) == false)
		{
			cout << "not acceptable." << endl;
            retcode = 1;
		}
		else
		{
			cout << "acceptable." << endl;
		}
	}

	return (retcode);
}
