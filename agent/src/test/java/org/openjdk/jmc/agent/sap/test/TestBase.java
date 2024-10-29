package org.openjdk.jmc.agent.sap.test;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

public abstract class TestBase {

	private static boolean smokeTestsOnly;

	public void dispatch(String[] args) {
		try {
			if (args.length == 0) {
				runAllTests();
			} else {
				try {
					Method m = this.getClass().getDeclaredMethod(args[0]);
					m.invoke(this);
				} catch (NoSuchMethodException e) {
					throw new RuntimeException("Indefined test '" + args[0] + "'");
				}
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Test failed", e);
		}
	}

	protected abstract void runAllTests() throws Exception;

	private static void failLines(String[] lines, String msg) {
		failLines(lines, msg, -1);
	}

	private static void failLines(String[] lines, String msg, int markedLine) {
		System.err.println(msg + ":");
		System.err.println("---- START");

		for (int i = 0; i < lines.length; ++i) {
			if (i == markedLine) {
				System.err.println("=> " + lines[i]);
			} else {
				System.err.println("   " + lines[i]);
			}
		}

		System.err.println("---- END");
		throw new AssertionError(msg);
	}

	public static void assertLinesContainsRegExp(String[] lines, String regexp) {
		Pattern pattern = Pattern.compile(regexp);

		for (String line : lines) {
			if (pattern.matcher(line).find()) {
				return;
			}
		}

		failLines(lines, "Could not find regexp '" + regexp + "' in the lines");
	}

	public static void assertLinesNotContainsRegExp(String[] lines, String regexp) {
		Pattern pattern = Pattern.compile(regexp);

		for (int i = 0; i < lines.length; ++i) {
			if (pattern.matcher(lines[i]).find()) {
				failLines(lines, "Unexpectedly found regexp '" + regexp + "' in the lines", i);
				return;
			}
		}
	}

	public static void setSmokeTestOnly() {
		smokeTestsOnly = true;
	}

	public static boolean smokeTestsOnly() {
		return smokeTestsOnly;
	}

	public void assertRunnerFinished(JavaAgentRunner runner) {
		int result = runner.waitForEnd();

		if (result != 0) {
			throw new AssertionError("Exit code " + result + " for " + runner.getCommandLine());
		}
	}

	public static void sleep(long seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {

		}
	}
}
