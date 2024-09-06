package org.openjdk.jmc.agent.converters.sap;

public class UnsafeAllocateMemoryResultConverter {
	public static long convert(long addr) {
		System.out.println("Result: 0x" + Long.toHexString(addr));

		return addr;
	}
}
