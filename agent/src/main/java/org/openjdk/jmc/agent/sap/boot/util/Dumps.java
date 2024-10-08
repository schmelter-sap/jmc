package org.openjdk.jmc.agent.sap.boot.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.function.Predicate;

import org.openjdk.jmc.agent.sap.boot.commands.Command;
import org.openjdk.jmc.agent.sap.boot.commands.CommandArguments;

public class Dumps {
	public static final String DUMP_COUNT = "dumpCount";
	public static final String DUMP_INTERVAL = "dumpInterval";
	public static final String DUMP_DELAY = "dumpDelay";
	public static final String EXIT_AFTER_LAST_DUMP = "exitAfterLastDump";

	private static final HashMap<Command, Predicate<CommandArguments>> registeredDumps = new HashMap<>();

	public static void performDump(String arguments) throws IOException {
		if (arguments.equals("help")) {
			System.out.println("The following dumps are supported:");

			for (Command cmd : registeredDumps.keySet()) {
				System.out.println(cmd.getName() + ": " + cmd.getDescription());
			}

			System.out.println("Use dump=help:<dump> for all options for a specific dump.");
			return;
		} else if (arguments.startsWith("help:")) {
			String name = arguments.substring(5);

			for (Command cmd : registeredDumps.keySet()) {
				if (cmd.getName().equals(name)) {
					cmd.printHelp(System.out);
					return;
				}
			}

			System.err.println("Could not find dump type '" + name + "'. Use dump=help for a list of supported types.");
			return;
		}

		String[] parts = arguments.split(",");
		String type = parts[0];

		if (parts.length > 1) {
			arguments = arguments.substring(type.length() + 1);
		} else {
			arguments = "";
		}

		CommandArguments args = new CommandArguments(arguments);
		Predicate<CommandArguments> callback = null;

		synchronized (Dumps.class) {
			callback = registeredDumps.get(new Command(type, ""));
		}

		if (callback == null) {
			System.err.println("No dump registered for dumType='" + type + "'");
			return;
		}

		callback.test(args);
	}

	public static void registerDump(Command command, String name, Predicate<CommandArguments> callback) {
		synchronized (Dumps.class) {
			registeredDumps.put(command, callback);
		}

		CommandArguments args = new CommandArguments(command);
		long dumpCount = args.getLong(DUMP_COUNT, 0);

		if (dumpCount != 0) {
			long interval = args.getDurationInSeconds(DUMP_INTERVAL, 3600);
			long delay = args.getDurationInSeconds(DUMP_DELAY, interval);
			boolean exitAfterLastDump = args.getBoolean(EXIT_AFTER_LAST_DUMP, false);

			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						Thread.sleep(delay * 1000);

						long dumpsLeft = dumpCount;

						while (dumpsLeft > 0) {
							if (callback.test(args)) {
								dumpsLeft -= 1;
								JdkLogging.log(args,
										name + " dump " + (dumpCount - dumpsLeft) + " of " + dumpCount + ".");
							}

							if (dumpsLeft > 0) {
								Thread.sleep(interval * 1000);
							}
						}

						if (exitAfterLastDump) {
							JdkLogging.log(args, name + " dumps finished. Exiting VM.");
							System.exit(0);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});

			t.setDaemon(true);
			t.start();
		}
	}

	public static void addOptions(Command command) {
		command.addOption(DUMP_COUNT, "The maximum number of dumps to perform.");
		command.addOption(DUMP_INTERVAL, "The interval between successive dumnps.");
		command.addOption(DUMP_DELAY, "The delay until the first dump is triggered.");
		command.addOption(EXIT_AFTER_LAST_DUMP,
				"If set to 'true', the VM will be exited after the last dump is triggered.");
	}
}
