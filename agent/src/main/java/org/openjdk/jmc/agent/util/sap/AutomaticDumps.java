package org.openjdk.jmc.agent.util.sap;

import java.util.function.Predicate;

public class AutomaticDumps {
	public static final String DUMP_COUNT = "dumpCount";
	public static final String DUMP_INTERVAL = "dumpInterval";
	public static final String DUMP_DELAY = "dumpDelay";

	public static void registerDump(CommandArguments args, Predicate<CommandArguments> callback) {
		long dumpCount = args.getLong(DUMP_COUNT, 0);

		if (dumpCount != 0) {
			long interval = args.getDurationInSeconds(DUMP_INTERVAL, 3600);
			long delay = args.getDurationInSeconds(DUMP_DELAY, interval);

			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						Thread.sleep(delay * 1000);

						long dumpsLeft = dumpCount;

						while (dumpsLeft > 0) {
							if (callback.test(args)) {
								dumpsLeft -= 1;
							}

							if (dumpsLeft > 0) {
								Thread.sleep(interval * 1000);
							}
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
		command.addOption(DUMP_COUNT, "The maximum number of dumps to perform");
		command.addOption(DUMP_INTERVAL, "The interval between successive dumnps");
		command.addOption(DUMP_DELAY, "The delay until the first dump is triggered");
	}
}
