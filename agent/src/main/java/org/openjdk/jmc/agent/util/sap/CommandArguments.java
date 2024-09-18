package org.openjdk.jmc.agent.util.sap;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.regex.Pattern;

public class CommandArguments {
	private static final String optionsLine = AccessController.doPrivileged(new PrivilegedAction<String>() {
		public String run() {
			return System.getProperty("com.sap.jvm.jmcagent.options", "");
		}
	});

	private final HashMap<String, String> args;
	private final Command command;

	public CommandArguments(Command command) {
		this.command = command;
		this.args = getOptions(optionsLine);
	}

	public CommandArguments(String line, Command command) {
		this.command = command;
		this.args = getOptions(line);
	}

	public String getString(String argumentName, String defaultResult) {
		if (args.containsKey(argumentName)) {
			return args.get(argumentName);
		}

		return defaultResult;
	}

	public Pattern getPattern(String argumentName, Pattern defaultResult) {
		if (args.containsKey(argumentName)) {
			return Pattern.compile(args.get(argumentName));
		}

		return defaultResult;
	}

	public int getInt(String argumentName, int defaultRersult) {
		if (args.containsKey(argumentName)) {
			try {
				return Integer.parseInt(args.get(argumentName));
			} catch (NumberFormatException e) {
				throw new RuntimeException(
						"Could not parse value for argument " + argumentName + " of command " + command.getName(),
						null);
			}
		}

		return defaultRersult;
	}

	public long getLong(String argumentName, long defaultRersult) {
		if (args.containsKey(argumentName)) {
			try {
				return Long.parseLong(args.get(argumentName));
			} catch (NumberFormatException e) {
				throw new RuntimeException(
						"Could not parse value for argument " + argumentName + " of command " + command.getName(),
						null);
			}
		}

		return defaultRersult;
	}

	private long parseUnits(String argumentName, long defaultRersult, char[] suffixes, long[] scale) {
		if (!args.containsKey(argumentName)) {
			return defaultRersult;
		}

		String rest = args.get(argumentName);
		long result = 0;

		while (!rest.isEmpty()) {
			long part = 0;

			while (!rest.isEmpty()) {
				int c = rest.charAt(0);

				if ((c < '0') || (c > '9')) {
					break;
				}

				part = part * 10 + (c - '0');
				rest = rest.substring(1);
			}

			if (rest.isEmpty()) {
				result += part;
				break;
			}

			boolean found = false;

			for (int i = 0; i < suffixes.length; ++i) {
				if (rest.charAt(0) == suffixes[i]) {
					result += part * scale[i];
					found = true;
					break;
				}
			}

			if (!found) {
				throw new RuntimeException("Unknown unit '" + rest.charAt(0) + "' in '" + args.get(argumentName) + "'");
			}

			rest = rest.substring(1);
		}

		return result;
	}

	public long getSize(String argumentName, long defaultRersult) {
		return parseUnits(argumentName, defaultRersult, new char[] {'k', 'M', 'G'},
				new long[] {1024, 1024 * 1024, 1024 * 1024 * 1024});
	}

	public long getDurationInSeconds(String argumentName, long defaultRersult) {
		return parseUnits(argumentName, defaultRersult, new char[] {'s', 'm', 'h', 'd'},
				new long[] {1, 60, 3600, 3600 * 24});
	}

	private HashMap<String, String> getOptions(String line) {
		HashMap<String, String> result = new HashMap<>();
		String[] commandList = line.split("(?<!\\\\):");

		for (int i = 0; i < commandList.length; ++i) {
			int endPos = commandList[i].indexOf('=');

			if (endPos == -1) {
				continue;
			}

			String curr = commandList[i].substring(0, endPos);

			if (!curr.equals(command.getName())) {
				continue;
			}

			String[] keysAndValues = commandList[i].substring(endPos + 1).split("(?<!\\\\),");

			for (String keyAndValue : keysAndValues) {
				String[] keyAndValues = keyAndValue.split("(?<!\\\\)=");

				if (keyAndValues[0].length() > 0) {
					result.put(keyAndValues[0], keyAndValues.length == 1 ? null : keyAndValues[1]);
				}
			}
		}

		return result;
	}
}
