package org.openjdk.jmc.agent.sap.test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

public class TestRunner {
	private static TestConfig[] testConfigs = new TestConfig[] {new TestConfig(UnsafeAllocationTest.class)};

	public static void main(String[] args) throws Exception {
		ArrayList<String> leftArgs = new ArrayList<>(Arrays.asList(args));

		while (leftArgs.size() > 0) {
			String first = leftArgs.get(0);

			if (first.equals("-dump")) {
				JavaAgentRunner.setDumpOnExit(true);
				leftArgs.remove(0);
			} else if (first.equals("-debug")) {
				if (leftArgs.size() < 2) {
					System.err.println("Missing port to '-debug'");
					return;
				}

				JavaAgentRunner.setDebugPort(Integer.parseInt(leftArgs.get(1)));
				leftArgs.remove(0);
				leftArgs.remove(0);
			} else {
				break;
			}
		}

		for (TestConfig testConfig : testConfigs) {
			boolean run = false;

			if (leftArgs.size() == 0) {
				run = true;
			} else {
				for (String arg : leftArgs) {
					if (testConfig.testClass.getName().endsWith("." + arg)) {
						run = true;
					}
				}
			}

			if (run) {
				Method mainMethod = testConfig.testClass.getDeclaredMethod("main", String[].class);
				mainMethod.invoke(null, new Object[] {testConfig.args});
			}
		}
	}

	private static class TestConfig {
		public final Class<?> testClass;
		public final String[] args;

		public TestConfig(Class<?> testClass, String ... args) {
			this.testClass = testClass;
			this.args = args.clone();
		}
	}
}
