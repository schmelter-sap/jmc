package org.openjdk.jmc.agent.sap.test;

import org.junit.Test;

public class SapIntegrationTest {

	@Test
	public void smokeTest() throws Exception {
		TestRunner.main(new String[] {"-smoke"});
	}
}
