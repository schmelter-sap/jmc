package org.openjdk.jmc.agent.converters.sap;

public class SystemPropChangeLoggerKeyConverter {
	public static boolean convert(Object key) {
		if (key instanceof String) {
			SystemPropChangeLogger.logKey((String) key);

			return true;
		}

		return false;
	}
}
