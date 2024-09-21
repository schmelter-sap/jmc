package org.openjdk.jmc.agent.sap.boot.commands;

import org.openjdk.jmc.agent.sap.boot.util.JdkLogging;

public class SystemPropChangeCommand {
	public static final Command enableCommand = new Command("traceSysPropsChange",
			"Traces changes to the system properties.");

	static {
		JdkLogging.addOptions(enableCommand);
	}
}
