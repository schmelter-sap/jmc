package org.openjdk.jmc.agent.sap.test;

import org.junit.Test;

public class SapIntegrationTest {

	@Test
	public void smokeTest() throws Exception {
		if (System.getProperty("fullTest", "false").equals("true")) {
			TestRunner.main(new String[0]);
		} else {
			TestRunner.main(new String[] {"-smoke"});
		}
	}
}
