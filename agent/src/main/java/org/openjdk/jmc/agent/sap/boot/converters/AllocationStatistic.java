package org.openjdk.jmc.agent.sap.boot.converters;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.openjdk.jmc.agent.sap.boot.commands.CommandArguments;
import org.openjdk.jmc.agent.sap.boot.util.JdkLogging;

public class AllocationStatistic {
	private HashMap<Long, AllocationSite> activeAllocations = new HashMap<>();
	private long totalSize = 0;
	private static long lastDumpSize = 0;

	public AllocationStatistic copy() {
		AllocationStatistic result = new AllocationStatistic();

		synchronized (activeAllocations) {
			result.activeAllocations = new HashMap<>(activeAllocations);
			result.totalSize = totalSize;
		}

		return result;
	}

	public void addAllocastion(long addr, long size) {
		AllocationSite site = new AllocationSite(size);

		synchronized (activeAllocations) {
			assert !activeAllocations.containsKey(addr);
			totalSize += size;
			activeAllocations.put(addr, site);
		}
	}

	public void removeAllocation(long addr) {
		synchronized (activeAllocations) {
			assert activeAllocations.containsKey(addr);
			AllocationSite site = activeAllocations.remove(addr);

			if (site != null) {
				assert totalSize > site.size;
				totalSize -= site.size;
			}
		}
	}

	public boolean printActiveAllocations(CommandArguments args) {
		PrintStream ps = JdkLogging.getStream(args);
		AllocationStatisticDumpFilter filter = new AllocationStatisticDumpFilter(args);

		synchronized (AllocationStatistic.class) {
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
				ps.println("Printed " + printedCount + " of " + activeAllocations.size() + " allocations with "
						+ printedSize + " bytes (of " + totalSize + " bytes allocated in total).");
				lastDumpSize = totalSize;
				dumped = true;
			}

			return dumped;
		}
	}
}
