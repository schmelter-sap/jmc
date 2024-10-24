package org.openjdk.jmc.agent.sap.test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

// You can run it via (if the cwd is the agent directory):
// java -javaagent:target/agent-1.0.1-SNAPSHOT.jar=traceUnsafeAllocations -cp target/test-classes --add-opens java.base/jdk.internal.misc=ALL-UNNAMED org.openjdk.jmc.agent.sap.test.UnsafeAllocationTestRunner
public class UnsafeAllocationTest extends TestBase {

	private static Method allocateMemoryMethod;
	private static Method reallocateMemoryMethod;
	private static Method freeMemoryMethod;
	private static Object theUnsafe;
	private static String DO_ALLOCS = "runRandomAllocs";
	private static String DO_NATIVE_ALLOCS = "doNativeAllocs";
	private static String DONE = "DONE";

	private static void initUnsafe() {
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

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length == 0) {
			testNativeAllocs();
		} else if (DO_ALLOCS.equals(args[0])) {
			runRandomAllocs(args);
		} else if (DO_NATIVE_ALLOCS.equals(args[0])) {
			doNativeAllocs();
		} else {
			System.out.println("Unknown test '" + args[0] + "'");
		}
	}

	private static void done() {
		System.out.println(DONE);
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void testNativeAllocs() throws IOException {
		JavaAgentRunner runner = new JavaAgentRunner(UnsafeAllocationTest.class,
				"traceUnsafeAllocations,logDest=stdout", "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED");
		runner.start(DO_NATIVE_ALLOCS);
		runner.waitForStdout(DONE);
		runner.loadAgent("dump=unsafeAllocations,logDest=stderr,mustContain=doNativeAllocs");
		runner.kill();
		assertLinesContainsRegExp(runner.getStderrLines(), "Allocated 570 bytes at");
		assertLinesContainsRegExp(runner.getStderrLines(), "Allocated 750 bytes at");
		assertLinesContainsRegExp(runner.getStderrLines(), "Printed 2 of 2 allocations with 1320 bytes");
		runner.start(DO_NATIVE_ALLOCS);
		runner.waitForStdout(DONE);
		runner.loadAgent("dump=unsafeAllocations,logDest=stderr,mustNotContain=reallocateMemory");
		runner.kill();
		assertLinesNotContainsRegExp(runner.getStderrLines(), "Allocated 570 bytes at");
		assertLinesContainsRegExp(runner.getStderrLines(), "Allocated 750 bytes at");
		assertLinesContainsRegExp(runner.getStderrLines(), "Printed 1 of 2 allocations with 750 bytes");
		runner.start(DO_NATIVE_ALLOCS);
		runner.waitForStdout(DONE);
		runner.loadAgent("dump=unsafeAllocations,logDest=stderr,minStackSize=571");
		runner.kill();
		assertLinesNotContainsRegExp(runner.getStderrLines(), "Allocated 570 bytes at");
		assertLinesContainsRegExp(runner.getStderrLines(), "Allocated 750 bytes at");
		assertLinesContainsRegExp(runner.getStderrLines(), "Printed 1 of 2 allocations with 750 bytes");
		runner.start(DO_NATIVE_ALLOCS);
		runner.waitForStdout(DONE);
		runner.loadAgent("dump=unsafeAllocations,logDest=stderr,minAge=1");
		runner.kill();
		assertLinesNotContainsRegExp(runner.getStderrLines(), "Allocated 570 bytes at");
		assertLinesNotContainsRegExp(runner.getStderrLines(), "Allocated 750 bytes at");
		assertLinesNotContainsRegExp(runner.getStderrLines(), "Printed");
	}

	public static void testRandomAllocs() throws IOException {
		JavaAgentRunner runner = new JavaAgentRunner(UnsafeAllocationTest.class,
				"traceUnsafeAllocations,dumpCount=1,dumpInterval=3s,logDest=stdout", "--add-opens",
				"java.base/jdk.internal.misc=ALL-UNNAMED");
		runner.start(DO_ALLOCS);
		runner.waitForStdout(DONE);
		runner.loadAgent("dump=unsafeAllocations,logDest=stderr,maxFrames=8");
		runner.kill();
		assertLinesContainsRegExp(runner.getStdoutLines(), "^Printed [0-9]+ of [0-9] allocations with [0-9]+ bytes");
		assertLinesContainsRegExp(runner.getStderrLines(), "^Printed [0-9]+ of [0-9] allocations with [0-9]+ bytes");
	}

	public static void doNativeAllocs() {
		initUnsafe();
		long a1 = 0;

		try {
			a1 = allocateMemory(1L << 56); // Should fail.
		} catch (OutOfMemoryError e) {
			// Excpected.
		}

		try {
			a1 = reallocateMemory(a1, 1L << 56); // Should fail.
		} catch (OutOfMemoryError e) {
			// Excpected.
		}

		a1 = allocateMemory(4027);
		a1 = reallocateMemory(a1, 0); // Should act like a free
		a1 = reallocateMemory(0, 570); // Should act like a malloc.
		long a2 = allocateMemory(128);
		freeMemory(a2);
		a2 = allocateMemory(750);
		done();
	}

	public static void runRandomAllocs(String[] args) {
		initUnsafe();
		int increasePerAlloc = args.length > 1 ? Integer.parseInt(args[1]) : 256;
		int maxDepth = args.length > 2 ? Integer.parseInt(args[2]) : 10;
		long addr = 0;
		long allocSize = increasePerAlloc;

		while (true) {
			addr = doAlloc1(addr, allocSize, Math.max(2, (int) (Math.random() * maxDepth)),
					(long) (Math.random() * Integer.MAX_VALUE));
			allocSize += increasePerAlloc;
		}
	}

	public static long doAllocImpl(long addr, long allocSize) {
		long dummy = allocateMemory(1024);
		addr = reallocateMemory(addr, Math.max(0, allocSize - 1024));
		addr = reallocateMemory(addr, allocSize);
		freeMemory(dummy);

		return addr;
	}

	public static long doAlloc1(long addr, long allocSize, int depth, long seed) {
		if (depth == 0) {
			return doAllocImpl(addr, allocSize);
		} else if ((seed & 1) == 0) {
			return doAlloc1(addr, allocSize, depth - 1, seed / 2);
		} else {
			return doAlloc2(addr, allocSize, depth - 1, seed / 2);
		}
	}

	public static long doAlloc2(long addr, long allocSize, int depth, long seed) {
		if (depth == 0) {
			return doAllocImpl(addr, allocSize);
		} else if ((seed & 1) == 0) {
			return doAlloc1(addr, allocSize, depth - 1, seed / 2);
		} else {
			return doAlloc2(addr, allocSize, depth - 1, seed / 2);
		}
	}

	private static long allocateMemory(long size) {
		try {
			return (Long) allocateMemoryMethod.invoke(theUnsafe, Long.valueOf(size));
		} catch (Exception e) {
			if (e.getCause() instanceof OutOfMemoryError) {
				throw (OutOfMemoryError) e.getCause();
			}

			e.printStackTrace();
			return 0;
		}
	}

	private static long reallocateMemory(long addr, long size) {
		try {
			return (Long) reallocateMemoryMethod.invoke(theUnsafe, Long.valueOf(addr), Long.valueOf(size));
		} catch (Exception e) {
			if (e.getCause() instanceof OutOfMemoryError) {
				throw (OutOfMemoryError) e.getCause();
			}

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
