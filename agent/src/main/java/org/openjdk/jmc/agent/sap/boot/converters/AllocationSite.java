package org.openjdk.jmc.agent.sap.boot.converters;

import java.io.PrintStream;
import java.util.Date;

public class AllocationSite {
	public final Exception stack;
	public final long timestamp;
	public final long size;

	public AllocationSite(long size) {
		this.stack = new Exception();
		this.timestamp = System.currentTimeMillis();
		this.size = size;
	}

	public boolean printOn(long address, PrintStream ps, AllocationStatisticDumpFilter filter) {
		if (size < filter.minStackSize) {
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
