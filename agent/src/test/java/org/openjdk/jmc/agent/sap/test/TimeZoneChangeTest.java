package org.openjdk.jmc.agent.sap.test;

import java.util.TimeZone;
import java.util.regex.Pattern;

public class TimeZoneChangeTest extends TestBase {

	public static void main(String[] args) {
		new TimeZoneChangeTest().dispatch(args);
	}

	@Override
	protected void runAllTests() throws Exception {
		JavaAgentRunner runner = new JavaAgentRunner(TimeZoneChangeTest.class, "traceTimeZoneChange,logDest=stdout");
		runner.start("changeTimeZones");
		runner.waitForEnd();
		assertLinesContainsRegExp(runner.getStdoutLines(),
				Pattern.quote("Changed default time zone to Central European Time (CET)"));
		assertLinesContainsRegExp(runner.getStdoutLines(),
				Pattern.quote("Changed default time zone to Greenwich Mean Time (Etc/GMT+0)"));
		assertLinesContainsRegExp(runner.getStdoutLines(),
				Pattern.quote("Changed default time zone to Central European Standard Time (Europe/Berlin)."));
	}

	public void changeTimeZones() {
		TimeZone.setDefault(TimeZone.getDefault());

		for (String id : TimeZone.getAvailableIDs()) {
			TimeZone.setDefault(TimeZone.getTimeZone(id));
		}
	}
}
