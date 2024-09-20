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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;

public class JdkLogging {
	public static final String LOG_DEST = "logDest";
	private static HashMap<String, PrintStream> outputs = new HashMap<>();

	public static void addOptions(Command command) {
		command.addOption(LOG_DEST, "Specified where the output shows up. Can be 'stdout', 'stderr' or a file name. "
				+ "Prepend the filename with a '+' to append to the file intead of overwriting it.");
	}

	public static PrintStream getStream(CommandArguments args) {
		String dest = args.getString(LOG_DEST, "stderr");

		if ("stdout".equals(dest)) {
			return System.out;
		}

		if ("stderr".equals(dest)) {
			return System.err;
		}

		synchronized (outputs) {
			PrintStream result = outputs.get(dest);

			if (result != null) {
				return result;
			}

			try {
				if (dest.startsWith("+")) {
					// Append if the file name starts with a +.
					result = new PrintStream(new FileOutputStream(dest.substring(1), true));
				} else {
					result = new PrintStream(new FileOutputStream(dest, false));
				}
			} catch (FileNotFoundException e) {
				System.err.println("Could not open file '" + dest + "' for output. Using stderr instead.");
				// Don't try this again.
				result = System.err;
			}

			outputs.put(dest, result);
			return result;
		}
	}

	public static void logWithStack(CommandArguments args, String msg) {
		PrintStream stream = getStream(args);
		stream.println(msg);

		StackTraceElement[] frames = new Exception().getStackTrace();

		for (int i = 3; i < frames.length; ++i) {
			stream.println("\t" + frames[i]);
		}
	}
}
