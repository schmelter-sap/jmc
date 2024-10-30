package org.openjdk.jmc.agent.sap.test;

import java.io.File;
import java.util.Properties;

// You can run it via (if the cwd is the agent directory):
// java -javaagent:target/agent-1.0.1-SNAPSHOT.jar=traceSysPropsChange -cp target/test-classes org.openjdk.jmc.agent.sap.test.SystemPropertiesChanger
public class SysPropsChangeTest extends TestBase {

	public static void main(String[] args) {
		new SysPropsChangeTest().dispatch(args);
	}

	@Override
	protected void runAllTests() throws Exception {
		JavaAgentRunner runner = getRunner("traceSysPropsChange,logDest=stdout");
		runner.start("changeSystemProps");
		runner.waitForEnd();
		assertLinesContainsRegExp(runner.getStdoutLines(),
				"System property 'TEST_KEY' changed from 'null' to 'TEST_VAL'");
		assertLinesContainsRegExp(runner.getStdoutLines(),
				"System properties 'TEST_KEY' with value 'TEST_VAL' removed");
		assertLinesContainsRegExp(runner.getStdoutLines(), SysPropsChangeTest.class.getName());
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
