package org.openjdk.jmc.agent.converters.sap;

public class UnsafeAllocateMemorySizeConverter {
	public static long convert(long size) {
		System.out.println("Size: " + size);
		return size;
	}
}
