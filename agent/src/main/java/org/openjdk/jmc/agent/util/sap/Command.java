package org.openjdk.jmc.agent.util.sap;

import java.util.HashMap;

public class Command {
	private final String name;
	private final String description;
	private final HashMap<String, String> optionsWithHelp;

	public Command(String name, String description, String ... optionsWithHelp) {
		this.name = name;
		this.description = description;
		this.optionsWithHelp = new HashMap<>();

		for (int i = 0; i < optionsWithHelp.length; i += 2) {
			this.optionsWithHelp.put(optionsWithHelp[i], optionsWithHelp[i + 1]);
		}
	}

	public Command(Command parentCommand, String name, String description, String ... optionsWithHelp) {
		this.name = name;
		this.description = description;
		this.optionsWithHelp = new HashMap<>(parentCommand.optionsWithHelp);

		for (int i = 0; i < optionsWithHelp.length; i += 2) {
			this.optionsWithHelp.put(optionsWithHelp[i], optionsWithHelp[i + 1]);
		}
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String[] getOptions() {
		return optionsWithHelp.keySet().toArray(new String[optionsWithHelp.size()]);
	}

	public boolean hasOption(String name) {
		return optionsWithHelp.containsKey(name);
	}

	public String getOptionHJelp(String name) {
		return optionsWithHelp.get(name);
	}
}
