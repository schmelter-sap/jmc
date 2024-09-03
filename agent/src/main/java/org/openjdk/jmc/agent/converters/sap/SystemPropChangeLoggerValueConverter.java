package org.openjdk.jmc.agent.converters.sap;

public class SystemPropChangeLoggerValueConverter {
	public static boolean convert(Object value) {
		if (value instanceof String) {
			SystemPropChangeLogger.logValue((String) value);

			return true;
		}

		return false;
	}
}
