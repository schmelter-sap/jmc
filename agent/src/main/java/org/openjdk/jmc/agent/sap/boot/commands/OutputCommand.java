package org.openjdk.jmc.agent.sap.boot.commands;

import org.openjdk.jmc.agent.sap.boot.util.JdkLogging;

public class OutputCommand extends Command {

	public OutputCommand(String name, String description) {
		super(name, description);
		JdkLogging.addOptions(this);
	}
}
