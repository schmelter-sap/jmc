package org.openjdk.jmc.agent.converters.sap;

import java.io.PrintStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.openjdk.jmc.agent.util.sap.AutomaticDumps;
import org.openjdk.jmc.agent.util.sap.Command;
import org.openjdk.jmc.agent.util.sap.CommandArguments;
import org.openjdk.jmc.agent.util.sap.JdkLogging;

public class UnsafeMemoryAllocationConverter {

	private static final ThreadLocal<Long> sizeKey = new ThreadLocal<Long>();
	private static final ThreadLocal<Long> ptrKey = new ThreadLocal<Long>();
	private static long totalSize = 0;
	private static final HashMap<Long, AllocationSite> activeAllocations = new HashMap<>();
	private static final String MAX_FRAMES = "maxFrames";
	private static final String MIN_SIZE = "minSize";
	private static final String MIN_INCREASE = "minIncrease";
	private static final String MIN_PERCENTAGE = "minPercentage";
	private static final String MIN_AGE = "minAge";
	private static final String MAX_AGE = "maxAge";
	private static final String MUST_CONTAIN = "mustContain";
	private static final String MUST_NOT_CONTAIN = "mustNotContain";
	private static final Command dumpCommand;
	private static final Command enableCommand;
	private static long lastDumpSize = 0;

	static {
		// spotless:off
		dumpCommand = new Command(
				"dumnpUnsafeAllocations", "Dump the currently active jdk.internal.misc.Unsafe allocatios.",
				MAX_FRAMES,	"The maximum number of frame to use for stack traces.",
				MIN_SIZE, "The minimum size of the live allocations to dump the result.",
				MIN_SIZE, "The increase in allocated size fopr a new dump to be printed.",
				MIN_PERCENTAGE, "The minimum percentage compared to the last dump to print a dump.",
				MIN_AGE, "The minimum age in minutes to include an allocation in the output.",
				MAX_AGE, "The maximum age in minutes to include an allocation in the output.",				
				MUST_CONTAIN, "A regexp which must match at least one frame to be printed.",
				MUST_NOT_CONTAIN, "A regexp which must not match any frame to be printed.");
		enableCommand = new Command(dumpCommand,
				"traceUnsafeAllocations", "Traces native memory allocation with jdk.internal.misc.Unsafe");
		// spotless:on

		AutomaticDumps.addOptions(enableCommand);
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
				AllocationSite newSite = new AllocationSite(newSize);

				synchronized (activeAllocations) {
					AllocationSite oldSite = activeAllocations.remove(oldPtr);

					// We don't want to fail if we haven't tracked the original allocation.
					if (oldSite != null) {
						totalSize += newSize - oldSite.size;

					} else {
						totalSize += newSize;
					}

					// Realloc with size 0 is a free.
					if (newSize > 0) {
						AllocationSite overwrittenSite = activeAllocations.put(toAdd, newSite);
						assert overwrittenSite != null;
					}
				}
			}
		} else {
			// This is malloc.
			Long size = sizeKey.get();

			if (size != null) {
				AllocationSite site = new AllocationSite(size);

				synchronized (activeAllocations) {
					AllocationSite oldSite = activeAllocations.put(toAdd, site);
					assert oldSite == null;
					totalSize += size;
				}
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
			synchronized (activeAllocations) {
				AllocationSite site = activeAllocations.remove(toRemove);
				assert site != null;

				if (site != null) {
					totalSize -= site.size;
				}
			}
		}

		return ptr;
	}

	public static boolean printActiveAllocations() {
		return printActiveAllocations(new CommandArguments(enableCommand));
	}

	public static boolean printActiveAllocations(CommandArguments args) {
		PrintStream ps = JdkLogging.getStream(args);
		DumpFilter filter = new DumpFilter(args);

		synchronized (activeAllocations) {
			if (totalSize < filter.minSize) {
				return false;
			}

			if (totalSize < lastDumpSize * filter.minPercentageIncrease) {
				return false;
			}

			if ((filter.minIncrease >= 0) && (totalSize < lastDumpSize + filter.minIncrease)) {
				return false;
			}

			long printedSize = 0;
			long printedCount = 0;
			boolean dumped = false;

			for (Map.Entry<Long, AllocationSite> entry : activeAllocations.entrySet()) {
				if (entry.getValue().printOn(entry.getKey(), ps, filter)) {
					printedSize += entry.getValue().size;
					printedCount += 1;
				}
			}

			if (printedCount > 0) {
				ps.println("Printed " + printedCount + " of " + activeAllocations.size() + " allocation with "
						+ printedSize + " bytes (of " + totalSize + " bytes allocated in total).");
				lastDumpSize = totalSize;
				dumped = true;
			}

			return dumped;
		}
	}

	private static class AllocationSite {
		public final Exception stack;
		public final long timestamp;
		public final long size;

		public AllocationSite(long size) {
			this.stack = new Exception();
			this.timestamp = System.currentTimeMillis();
			this.size = size;
		}

		public boolean printOn(long address, PrintStream ps, DumpFilter filter) {
			if (filter.minSize > size) {
				return false;
			}

			long age = System.currentTimeMillis() - timestamp;

			if (age > filter.maxAge) {
				return false;
			}

			if (age < filter.minAge) {
				return false;
			}

			StackTraceElement[] frames = stack.getStackTrace();
			int framesToSkip = 2;
			int maxFrames = Math.min(filter.maxFrames + framesToSkip, frames.length);

			if (filter.mustContain != null) {
				boolean matchFound = false;

				for (int i = framesToSkip; i < maxFrames; ++i) {
					if (filter.mustContain.matcher(frames[i].toString()).matches()) {
						matchFound = true;

						break;
					}
				}

				if (!matchFound) {
					return false;
				}
			}

			if (filter.mustNotContain != null) {
				for (int i = framesToSkip; i < maxFrames; ++i) {
					if (filter.mustNotContain.matcher(frames[i].toString()).matches()) {
						return false;
					}
				}
			}

			ps.println("Allocated " + size + " bytes at 0x" + Long.toUnsignedString(address, 16));
			ps.println("Timestamp: " + new Date(timestamp).toString());
			ps.println("Allocated at:");

			for (int i = framesToSkip; i < maxFrames; ++i) {
				ps.println("\t" + frames[i]);
			}

			return true;
		}
	}

	public static class DumpFilter {
		public final int maxFrames;
		public final long minSize;
		public final long minIncrease;
		public final double minPercentageIncrease;
		public final long minAge;
		public final long maxAge;
		public final Pattern mustContain;
		public final Pattern mustNotContain;

		public DumpFilter(CommandArguments args) {
			this.maxFrames = args.getInt(MAX_FRAMES, 16);
			this.minSize = args.getSize(MIN_SIZE, 0);
			this.minIncrease = args.getSize(MIN_INCREASE, -1);
			this.minPercentageIncrease = 0.01 * args.getLong(MIN_PERCENTAGE, 0);
			this.minAge = 1000 * args.getDurationInSeconds(MIN_AGE, 0);
			this.maxAge = 1000 * args.getDurationInSeconds(MAX_AGE, 365 * 24 * 3600);
			this.mustContain = args.getPattern(MUST_CONTAIN, null);
			this.mustNotContain = args.getPattern(MUST_NOT_CONTAIN, null);
		}
	}
}
