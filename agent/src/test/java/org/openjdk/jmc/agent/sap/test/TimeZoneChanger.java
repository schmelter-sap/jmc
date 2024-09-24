package org.openjdk.jmc.agent.sap.test;

import java.util.TimeZone;

public class TimeZoneChanger {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getDefault());

		for (String id : TimeZone.getAvailableIDs()) {
			TimeZone.setDefault(TimeZone.getTimeZone(id));
		}
	}
}
