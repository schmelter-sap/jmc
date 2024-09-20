package org.openjdk.jmc.agent.sap.test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

// You can run it via (if the cwd is the agent directory):
// java -javaagent:target/agent-1.0.1-SNAPSHOT.jar=traceUnsafeAllocations -cp target/test-classes --add-opens java.base/jdk.internal.misc=ALL-UNNAMED org.openjdk.jmc.agent.sap.test.UnsafeAllocationTestRunner
public class UnsafeAllocationTestRunner {

	private static Method allocateMemoryMethod;
	private static Method reallocateMemoryMethod;
	private static Method freeMemoryMethod;
	private static Object theUnsafe;

	static {
		try {
			Class<?> unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
			Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
			theUnsafeField.setAccessible(true);
			theUnsafe = theUnsafeField.get(null);
			allocateMemoryMethod = unsafeClass.getDeclaredMethod("allocateMemory", long.class);
			reallocateMemoryMethod = unsafeClass.getDeclaredMethod("reallocateMemory", long.class, long.class);
			freeMemoryMethod = unsafeClass.getDeclaredMethod("freeMemory", long.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		while (true) {
			long addr1 = allocateMemory(1000);
			addr1 = reallocateMemory(addr1, 2000);
			freeMemory(addr1);
		}
	}

	private static long allocateMemory(long size) {
		try {
			return (Long) allocateMemoryMethod.invoke(theUnsafe, Long.valueOf(size));
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	private static long reallocateMemory(long addr, long size) {
		try {
			return (Long) reallocateMemoryMethod.invoke(theUnsafe, Long.valueOf(addr), Long.valueOf(size));
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	private static void freeMemory(long addr) {
		try {
			freeMemoryMethod.invoke(theUnsafe, Long.valueOf(addr));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
