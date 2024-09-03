package org.openjdk.jmc.agent.converters.sap;

import java.util.Properties;

public class SystemPropChangeLoggerPropsConverter {
	public static boolean convert(Properties props) {
		return SystemPropChangeLogger.logProperties(props);
	}
}
