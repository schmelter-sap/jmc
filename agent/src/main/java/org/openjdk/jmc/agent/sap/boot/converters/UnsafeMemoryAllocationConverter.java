package org.openjdk.jmc.agent.sap.boot.converters;

import org.openjdk.jmc.agent.sap.boot.commands.CommandArguments;
import org.openjdk.jmc.agent.sap.boot.commands.UnsafeMemoryAllocationCommand;
import org.openjdk.jmc.agent.sap.boot.util.AutomaticDumps;
import org.openjdk.jmc.agent.sap.boot.util.JdkLogging;

public class UnsafeMemoryAllocationConverter extends UnsafeMemoryAllocationCommand {

	private static final ThreadLocal<Long> sizeKey = new ThreadLocal<Long>();
	private static final ThreadLocal<Long> ptrKey = new ThreadLocal<Long>();
	private static final AllocationStatistic allocations = new AllocationStatistic();

	static {
		AutomaticDumps.registerDump(new CommandArguments(enableCommand), "Unsafe native memory allocation",
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
		return printActiveAllocations(new CommandArguments(enableCommand));
	}

	public static boolean printActiveAllocations(CommandArguments args) {
		if (JdkLogging.doesOutput(args)) {
			return allocations.copy().printActiveAllocations(args);
		}

		return false;
	}
}
