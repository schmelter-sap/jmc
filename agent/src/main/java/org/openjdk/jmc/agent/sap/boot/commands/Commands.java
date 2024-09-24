package org.openjdk.jmc.agent.sap.boot.commands;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;

import org.openjdk.jmc.agent.sap.boot.converters.LocaleChangeLogger;
import org.openjdk.jmc.agent.sap.boot.converters.SystemPropChangeLogger;
import org.openjdk.jmc.agent.sap.boot.converters.TimeZoneChangeLogger;
import org.openjdk.jmc.agent.sap.boot.converters.UnsafeMemoryAllocationLogger;

public class Commands {

	public static final Command[] commands = new Command[] {LocaleChangeLogger.command, SystemPropChangeLogger.command,
			TimeZoneChangeLogger.command, UnsafeMemoryAllocationLogger.command};

	public static void printAllCommands() {
		System.out.println("The following commands are suppored:");

		for (Command command : commands) {
			System.out.println(command.getName() + ": " + command.getDescription());
		}

		System.out.println("Use <command>,help to get further help for a specific command.");
	}

	public static Command getCommand(String name) {
		for (Command command : commands) {
			if (command.getName().equals(name)) {
				return command;
			}
		}

		return null;
	}

	public static boolean checkCommands() {
		for (Command command : commands) {
			String optionsLine = AccessController.doPrivileged(new PrivilegedAction<String>() {
				public String run() {
					return System.getProperty("com.sap.jvm.jmcagent.options." + command.getName(), null);
				}
			});

			if (optionsLine == null) {
				continue;
			}

			CommandArguments args = new CommandArguments(optionsLine, command);

			if (args.hasHelpOption()) {
				printHelp(command);

				return false;
			}

			String unknownArgument = args.getUnknownArgument();

			if (unknownArgument != null) {
				System.err
						.println("Unknown argument '" + unknownArgument + "' for command '" + command.getName() + "'.");
				printHelp(command);

				return false;
			}
		}

		return true;
	}

	private static void printHelp(Command command) {
		System.err.println("Help for command '" + command.getName() + "':");
		System.err.println("Description: " + command.getDescription());
		String[] options = command.getOptions();
		Arrays.sort(options, String.CASE_INSENSITIVE_ORDER);

		if (options.length > 0) {
			System.err.println("The following options are supported:");

			for (String option : options) {
				System.err.println(option + ": " + command.getOptionHJelp(option));
			}
		}
	}
}
