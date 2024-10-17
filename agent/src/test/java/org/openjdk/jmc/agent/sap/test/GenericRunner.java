package org.openjdk.jmc.agent.sap.test;

import java.util.Date;

//You can run it via (if the cwd is the agent directory):
// java -javaagent:target/agent-1.0.1-SNAPSHOT.jar=target/test-classes/org/openjdk/jmc/agent/test/sap/generic.xml -cp target/test-classes org.openjdk.jmc.agent.sap.test.GenericRunner
public class GenericRunner {

	public static String s1 = "Test1";
	public static long l1 = Long.MAX_VALUE;

	public static void main(String[] args) {
		log1(false, (byte) 2, 17, 0.2f, "TestMe", new Date());
		log2((short) 7, 0.3, "Yeah", 17, 0.3f);
		log3('A', -1, 0.1, (short) -1, true);
		log4(false, (byte) 2, 17, -0.2f, "TestMe", new Date());
		log5((short) 7, 0.3, "Yeah", 17, -0.3f);
	}

	@SuppressWarnings("deprecation")
	public static String log1(boolean v1, byte v2, int v3, float v4, String v5, Date v6) {
		return v6.toGMTString();
	}

	public static int log2(short v1, double v2, String v3, Integer v4, float v5) {
		return (int) v2;
	}

	public static double log3(char v1, Integer v2, Double v3, Short v4, Boolean v5) {
		return v1 + v3;
	}

	@SuppressWarnings("deprecation")
	public static String log4(boolean v1, byte v2, int v3, float v4, String v5, Date v6) {
		return v6.toGMTString();
	}

	public static int log5(short v1, double v2, String v3, Integer v4, float v5) {
		return (int) v2;
	}
}
