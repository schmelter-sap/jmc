package org.openjdk.jmc.agent.sap.test;

import java.util.regex.Pattern;

public class TestBase {

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
}
