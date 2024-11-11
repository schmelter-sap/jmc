package org.openjdk.jmc.agent.sap.boot.commands;

import org.openjdk.jmc.agent.sap.boot.util.Dumps;
import org.openjdk.jmc.agent.sap.boot.util.JdkLogging;

public class OpenFileStatisticCommand {
	public static final String MUST_CONTAIN = "mustContain";
	public static final String MUST_NOT_CONTAIN = "mustNotContain";
	public static final Command dumpCommand;
	public static final Command enableCommand;

	static {
		// spotless:off
		dumpCommand = new Command(
				"openFiles", "Dump the currently files opened by Java code.",
				MUST_CONTAIN, "A regexp which must the file name to be printed.",
				MUST_NOT_CONTAIN, "A regexp which must not match the file name to be printed.");
		enableCommand = new Command(dumpCommand,
				"traceOpenFiles", "Traces files opened by Java code.");
		// spotless:on

		JdkLogging.addOptions(enableCommand);
		Dumps.addOptions(enableCommand);
	}
}
