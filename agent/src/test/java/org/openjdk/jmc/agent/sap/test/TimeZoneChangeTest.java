package org.openjdk.jmc.agent.sap.test;

import java.util.TimeZone;

public class TimeZoneChangeTest extends TestBase {

	public static void main(String[] args) {
		new TimeZoneChangeTest().dispatch(args);
	}

	@Override
	protected void runAllTests() throws Exception {
		JavaAgentRunner runner = getRunner("traceTimeZoneChange,logDest=stdout");
		runner.start("changeTimeZones");
		runner.waitForEnd();
		assertLinesContains(runner.getStdoutLines(), "Changed default time zone to Central European Time (CET)");
		assertLinesContains(runner.getStdoutLines(), "Changed default time zone to Greenwich Mean Time (Etc/GMT+0)");
		assertLinesContains(runner.getStdoutLines(),
				"Changed default time zone to Central European Standard Time (Europe/Berlin).");
	}

	public void changeTimeZones() {
		TimeZone.setDefault(TimeZone.getDefault());

		for (String id : TimeZone.getAvailableIDs()) {
			TimeZone.setDefault(TimeZone.getTimeZone(id));
		}
	}
}
