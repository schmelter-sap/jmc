package org.openjdk.jmc.agent.util.sap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class JdkLogging {

	private static final String JDK_LOG_DEST = "com.sap.jdk.jdkLoggingDest";
	private static final PrintStream logStream;

	static {
		String logDest = AccessController.doPrivileged(new PrivilegedAction<String>() {
			public String run() {
				return System.getProperty(JDK_LOG_DEST);
			}
		});

		PrintStream tmpStream;

		if (logDest == null || "stdout".equals(logDest)) {
			tmpStream = System.out;
		} else if ("stderr".equals(logDest)) {
			tmpStream = System.err;
		} else {
			try {
				tmpStream = new PrintStream(new File(logDest));
			} catch (FileNotFoundException e) {
				System.err.println("Could not create file '" + logDest + "' for logging, using stderr instead");
				tmpStream = System.err;
			}
		}

		logStream = tmpStream;
	}

	public static void log(String msg) {
		logStream.println(msg);

		StackTraceElement[] frames = new Exception().fillInStackTrace().getStackTrace();

		for (int i = 3; i < frames.length; ++i) {
			logStream.println("\t" + frames[i]);
		}
	}
}
