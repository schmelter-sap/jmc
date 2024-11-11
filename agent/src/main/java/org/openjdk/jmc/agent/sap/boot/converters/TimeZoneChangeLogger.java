package org.openjdk.jmc.agent.sap.boot.converters;

import java.util.TimeZone;

import org.openjdk.jmc.agent.sap.boot.commands.Command;
import org.openjdk.jmc.agent.sap.boot.commands.CommandArguments;
import org.openjdk.jmc.agent.sap.boot.commands.OutputCommand;
import org.openjdk.jmc.agent.sap.boot.util.JdkLogging;

public class TimeZoneChangeLogger {

	public static Command command = new OutputCommand("traceTimeZoneChange",
			"Logs when the default time zone is changed.") {
		public void preTraceInit() {
			TimeZone.getDefault(); // Trigger loading before we trace to avoid circularity errors.
		}
	};

	public static String logDefaultTimeZoneChange(TimeZone newZone) {
		String result = newZone.getDisplayName();

		// If this same, don't log it. 
		if (changesDefaultTimeZone(newZone)) {
			JdkLogging.logWithStack(new CommandArguments(command),
					"Changed default time zone to " + result + " (" + newZone.toZoneId().toString() + ").", 3);
		}

		return result;
	}

	public static String logDefaultTimeZoneIdChange(TimeZone newZone) {
		return newZone.toZoneId().getId();
	}

	public static boolean changesDefaultTimeZone(TimeZone newZone) {
		return !newZone.toZoneId().equals(TimeZone.getDefault().toZoneId());
	}
}
