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

package org.openjdk.jmc.agent.sap.boot.converters;

import org.openjdk.jmc.agent.sap.boot.commands.Command;
import org.openjdk.jmc.agent.sap.boot.commands.CommandArguments;
import org.openjdk.jmc.agent.sap.boot.commands.UnsafeMemoryAllocationCommand;
import org.openjdk.jmc.agent.sap.boot.util.Dumps;
import org.openjdk.jmc.agent.sap.boot.util.JdkLogging;

public class UnsafeMemoryAllocationLogger {

	private static final ThreadLocal<Long> sizeKey = new ThreadLocal<Long>();
	private static final ThreadLocal<Long> ptrKey = new ThreadLocal<Long>();
	private static final AllocationStatistic allocations = new AllocationStatistic();

	public static final Command command = UnsafeMemoryAllocationCommand.enableCommand;

	static {
		Dumps.registerDump(UnsafeMemoryAllocationCommand.dumpCommand, null,
				(CommandArguments args) -> printActiveAllocations(args));
		Dumps.registerDump(command, "Unsafe native memory allocation",
				(CommandArguments args) -> printActiveAllocations(args));
	}

	public static long logSize(long size) {
		assert sizeKey.get() == null;
		sizeKey.set(size);

		return size;
	}

	public static long logPtr(long ptr) {
		assert ptrKey.get() == null;
		assert sizeKey.get() != null;
		ptrKey.set(ptr);

		return ptr;
	}

	public static long logResult(long result) {
		Long toAdd = Long.valueOf(result);
		Long oldPtr = ptrKey.get();

		if ((oldPtr != null) && (oldPtr.longValue() != 0)) {
			// This is realloc.
			Long newSize = sizeKey.get();

			if (newSize != null) {
				allocations.removeAllocation(oldPtr);

				// Realloc with size 0 is a free.
				if (newSize > 0) {
					allocations.addAllocastion(toAdd, newSize);
				}
			}
		} else {
			// This is malloc.
			Long size = sizeKey.get();

			if (size != null) {
				allocations.addAllocastion(toAdd, size);
			}
		}

		sizeKey.remove();
		ptrKey.remove();

		return result;
	}

	public static long logFree(long ptr) {
		assert sizeKey.get() == null;
		assert ptrKey.get() == null;

		Long toRemove = Long.valueOf(ptr);

		if (toRemove != null) {
			allocations.removeAllocation(toRemove);
		}

		return ptr;
	}

	public static boolean printActiveAllocations() {
		return printActiveAllocations(new CommandArguments(command));
	}

	public static boolean printActiveAllocations(CommandArguments args) {
		if (JdkLogging.doesOutput(args)) {
			return allocations.copy().printActiveAllocations(args);
		}

		return false;
	}
}
