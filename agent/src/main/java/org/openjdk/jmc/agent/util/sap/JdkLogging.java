/*
 * Copyright (c) 2024 SAP SE. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

package org.openjdk.jmc.agent.util.sap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class JdkLogging {

	private static final String JDK_LOG_DEST = "com.sap.jdk.jdkLoggingDest";
	private static final PrintStream logStream;

	static {
		String logDest = AccessController.doPrivileged(new PrivilegedAction<String>() {
			public String run() {
				return System.getProperty(JDK_LOG_DEST);
			}
		});

		PrintStream tmpStream;

		if (logDest == null || "stdout".equals(logDest)) {
			tmpStream = System.out;
		} else if ("stderr".equals(logDest)) {
			tmpStream = System.err;
		} else {
			try {
				tmpStream = new PrintStream(new File(logDest));
			} catch (FileNotFoundException e) {
				System.err.println("Could not create file '" + logDest + "' for logging, using stderr instead");
				tmpStream = System.err;
			}
		}

		logStream = tmpStream;
	}

	public static void log(String msg) {
		logStream.println(msg);

		StackTraceElement[] frames = new Exception().getStackTrace();

		for (int i = 3; i < frames.length; ++i) {
			logStream.println("\t" + frames[i]);
		}
	}
}
