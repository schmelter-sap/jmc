package org.openjdk.jmc.agent.sap.test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

public class TestRunner {
	private static Class<?>[] testClasses = new Class[] {UnsafeAllocationTest.class, SysPropsChangeTest.class,
			TimeZoneChangeTest.class, LocaleChangeTest.class};

	public static void main(String[] args) throws Exception {
		ArrayList<String> leftArgs = new ArrayList<>(Arrays.asList(args));

		while (leftArgs.size() > 0) {
			String first = leftArgs.get(0);

			if (first.equals("-dump")) {
				JavaAgentRunner.setDumpOnExit(true);
				leftArgs.remove(0);
			} else if (first.equals("-debug")) {
				if (leftArgs.size() < 2) {
					throw new Exception("Missing port to '-debug'");
				}

				JavaAgentRunner.setDebugPort(Integer.parseInt(leftArgs.get(1)));
				leftArgs.remove(0);
				leftArgs.remove(0);
			} else if (first.equals("-smoke")) {
				TestBase.setSmokeTestOnly();
				leftArgs.remove(0);
			} else {
				break;
			}
		}

		for (Class<?> testClass : testClasses) {
			boolean run = false;

			if (leftArgs.size() == 0) {
				run = true;
			} else {
				for (String arg : leftArgs) {
					if (testClass.getName().endsWith("." + arg)) {
						run = true;
					}
				}
			}

			if (run) {
				Method mainMethod = testClass.getDeclaredMethod("main", String[].class);
				mainMethod.invoke(null, new Object[] {new String[0]});
			}
		}
	}
}
