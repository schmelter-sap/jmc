package org.openjdk.jmc.agent.sap.test;

import java.lang.reflect.Method;

public class TestRunner {
	private static TestConfig[] testConfigs = new TestConfig[] {new TestConfig(UnsafeAllocationTestRunner.class)};

	public static void main(String[] args) throws Exception {
		for (TestConfig testConfig : testConfigs) {
			if ((args.length == 0) || testConfig.testClass.getName().endsWith("." + args[0])) {
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
