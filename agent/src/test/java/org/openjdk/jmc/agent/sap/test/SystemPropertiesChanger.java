package org.openjdk.jmc.agent.sap.test;

import java.io.File;
import java.util.Properties;

// You can run it via (if the cwd is the agent directory):
// java -javaagent:target/agent-1.0.1-SNAPSHOT.jar=traceSysPropsChange -cp target/test-classes org.openjdk.jmc.agent.sap.test.SystemPropertiesChanger
public class SystemPropertiesChanger {

	public static void main(String[] args) {
		new File("testfile");
		System.setProperty("TEST_KEY", "TEST_VAL");
		System.getProperties().remove("TEST_KEY");
		Properties props = new Properties();
		props.put("TEST_KEY", "TEST_ADD_VALUE");
		props.setProperty("TEST_KEY", "TEST_CHNAGE_VALUE");
		props.remove("TEST_KEY");
	}
}
