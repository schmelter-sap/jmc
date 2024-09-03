package org.openjdk.jmc.agent.converters.sap;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

import org.openjdk.jmc.agent.util.sap.JdkLogging;

public class SystemPropChangeLogger {
	private static final Properties systemProps = AccessController.doPrivileged(new PrivilegedAction<Properties>() {
		public Properties run() {
			return System.getProperties();
		}
	});

	private static final ThreadLocal<String> usedKey = new ThreadLocal<String>();
	private static final ThreadLocal<String> usedValue = new ThreadLocal<String>();

	public static boolean logProperties(Properties props) {
		String key = usedKey.get();
		String val = usedValue.get();

		usedKey.remove();
		usedValue.remove();

		if (props == systemProps) {
			String oldVal = props.getProperty(key);

			if (val == null) {
				JdkLogging.log("System properties '" + key + "' with value '" + oldVal + "' removed");
			} else {
				JdkLogging.log("System property '" + key + "' changed from '" + oldVal + "' to '" + val + "'");
			}

			return true;
		}

		return false;
	}

	public static void logKey(String key) {
		usedKey.set(key);
	}

	public static void logValue(String value) {
		usedValue.set(value);
	}
}
