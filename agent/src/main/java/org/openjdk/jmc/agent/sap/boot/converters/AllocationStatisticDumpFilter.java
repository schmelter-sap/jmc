package org.openjdk.jmc.agent.sap.boot.converters;

import java.util.regex.Pattern;

import org.openjdk.jmc.agent.sap.boot.commands.CommandArguments;

public class AllocationStatisticDumpFilter {
	public final int maxFrames;
	public final long minSize;
	public final long minStackSize;
	public final long minIncrease;
	public final double minPercentageIncrease;
	public final long minAge;
	public final long maxAge;
	public final Pattern mustContain;
	public final Pattern mustNotContain;

	public AllocationStatisticDumpFilter(CommandArguments args) {
		this.maxFrames = args.getInt(UnsafeMemoryAllocationLogger.MAX_FRAMES, 16);
		this.minSize = args.getSize(UnsafeMemoryAllocationLogger.MIN_SIZE, 0);
		this.minStackSize = args.getSize(UnsafeMemoryAllocationLogger.MIN_STACK_SIZE, 0);
		this.minIncrease = args.getSize(UnsafeMemoryAllocationLogger.MIN_INCREASE, -1);
		this.minPercentageIncrease = 0.01 * args.getLong(UnsafeMemoryAllocationLogger.MIN_PERCENTAGE, 0);
		this.minAge = 1000 * args.getDurationInSeconds(UnsafeMemoryAllocationLogger.MIN_AGE, 0);
		this.maxAge = 1000 * args.getDurationInSeconds(UnsafeMemoryAllocationLogger.MAX_AGE, 365 * 24 * 3600);
		this.mustContain = args.getPattern(UnsafeMemoryAllocationLogger.MUST_CONTAIN, null);
		this.mustNotContain = args.getPattern(UnsafeMemoryAllocationLogger.MUST_NOT_CONTAIN, null);
	}
}
