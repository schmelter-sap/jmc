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

package org.openjdk.jmc.agent.sap.boot.commands;

import org.openjdk.jmc.agent.sap.boot.util.Dumps;
import org.openjdk.jmc.agent.sap.boot.util.JdkLogging;

public class UnsafeMemoryAllocationCommand {
	public static final String MAX_FRAMES = "maxFrames";
	public static final String MIN_STACK_SIZE = "minStackSize";
	public static final String MIN_SIZE = "minSize";
	public static final String MIN_INCREASE = "minIncrease";
	public static final String MIN_PERCENTAGE = "minPercentage";
	public static final String MIN_AGE = "minAge";
	public static final String MAX_AGE = "maxAge";
	public static final String MUST_CONTAIN = "mustContain";
	public static final String MUST_NOT_CONTAIN = "mustNotContain";
	public static final Command dumpCommand;
	public static final Command enableCommand;

	static {
		// spotless:off
		dumpCommand = new Command(
				"unsafeAllocations", "Dump the currently active jdk.internal.misc.Unsafe allocatios.",
				MAX_FRAMES,	"The maximum number of frame to use for stack traces.",
				MIN_SIZE, "The minimum size of the live allocations to dump the result.",
				MIN_STACK_SIZE, "The minimum size of a stack to be included in a dump.",
				MIN_PERCENTAGE, "The minimum percentage compared to the last dump to print a dump.",
				MIN_AGE, "The minimum age in minutes to include an allocation in the output.",
				MAX_AGE, "The maximum age in minutes to include an allocation in the output.",				
				MUST_CONTAIN, "A regexp which must match at least one frame to be printed.",
				MUST_NOT_CONTAIN, "A regexp which must not match any frame to be printed.");
		enableCommand = new Command(dumpCommand,
				"traceUnsafeAllocations", "Traces native memory allocation with jdk.internal.misc.Unsafe");
		// spotless:on

		JdkLogging.addOptions(enableCommand);
		Dumps.addOptions(enableCommand);
	}
}
