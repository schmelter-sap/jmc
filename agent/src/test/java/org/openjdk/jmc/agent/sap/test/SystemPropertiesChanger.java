package org.openjdk.jmc.agent.sap.test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

// You can run it via (if the cwd is the agent directory):
// java -javaagent:target/agent-1.0.1-SNAPSHOT.jar=traceSysPropsChange -cp target/test-classes org.openjdk.jmc.agent.sap.test.SystemPropertiesChanger
public class SystemPropertiesChanger extends TestBase {

	private static String TEST = "test";

	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			baseTest();
		} else if (TEST.equals(args[0])) {
			changeSystemProps();
		} else {
			throw new RuntimeException("Unknown test '" + args[0] + "'");
		}
	}

	public static void baseTest() throws IOException {
		JavaAgentRunner runner = new JavaAgentRunner(SystemPropertiesChanger.class,
				"traceSysPropsChange,logDest=stdout");
		runner.start(TEST);
		runner.waitForEnd();
		assertLinesContainsRegExp(runner.getStdoutLines(),
				"System property 'TEST_KEY' changed from 'null' to 'TEST_VAL'");
		assertLinesContainsRegExp(runner.getStdoutLines(),
				"System properties 'TEST_KEY' with value 'TEST_VAL' removed");
		assertLinesContainsRegExp(runner.getStdoutLines(), SystemPropertiesChanger.class.getName());
		assertLinesNotContainsRegExp(runner.getStdoutLines(), "TEST_KEY_NO_SYS");
	}

	public static void changeSystemProps() {
		new File("testfile");
		System.setProperty("TEST_KEY", "TEST_VAL");
		System.getProperties().remove("TEST_KEY");
		Properties props = new Properties();
		props.put("TEST_KEY_NO_SYS", "TEST_ADD_VALUE");
		props.setProperty("TEST_KEY_NO_SYS", "TEST_CHANGE_VALUE");
		props.remove("TEST_KEY_NO_SYS");
	}
}
