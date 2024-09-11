package org.openjdk.jmc.agent.converters.sap;

import java.io.PrintStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UnsafeMemoryAllocationConverter {

	private static final ThreadLocal<Long> sizeKey = new ThreadLocal<Long>();
	private static final ThreadLocal<Long> ptrKey = new ThreadLocal<Long>();
	private static long totalSize = 0;
	private static final HashMap<Long, AllocationSite> activeAllocations = new HashMap<>();

	static {
		new Thread(new Runnable() {
			public void run() {
				try {
					while (true) {
						Thread.sleep(10000);
						printActiveAllocations(System.out);
					}
				} catch (InterruptedException e) {
					// Ignore.
				}
			}
		}).start();
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
						totalSize += newSize - oldSite.getSize();

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
					totalSize -= site.getSize();
				}
			}
		}

		return ptr;
	}

	public static void printActiveAllocations(PrintStream ps) {
		synchronized (activeAllocations) {
			for (Map.Entry<Long, AllocationSite> entry : activeAllocations.entrySet()) {
				entry.getValue().printOn(entry.getKey(), 10, ps);
				totalSize += entry.getValue().getSize();
			}

			ps.println("Printed " + activeAllocations.size() + " allocation with " + totalSize
					+ " bytes allocated in total.");
		}
	}

	private static class AllocationSite {
		private final Exception stack;
		private final long timestamp;
		private final long size;

		public AllocationSite(long size) {
			this.stack = new Exception();
			this.timestamp = System.currentTimeMillis();
			this.size = size;
		}

		public void printOn(long address, int maxFrames, PrintStream ps) {
			ps.println("Allocated " + size + " bytes at 0x" + Long.toUnsignedString(address, 16));
			ps.println("Timestamp: " + new Date(timestamp).toString());
			ps.println("Allocated at:");

			StackTraceElement[] frames = stack.getStackTrace();

			for (int i = 3; i < Math.min(maxFrames + 3, frames.length); ++i) {
				ps.println("\t" + frames[i]);
			}
		}

		public long getSize() {
			return size;
		}
	}
}
