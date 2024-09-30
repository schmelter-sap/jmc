package org.openjdk.jmc.agent.sap.test;

import java.lang.reflect.Method;

public class DuplicateTracedClasses {

	public static void main(String[] args) throws Exception {
		CloneClassLoader cl1 = new CloneClassLoader(DuplicateTracedClasses.class.getClassLoader());
		CloneClassLoader cl2 = new CloneClassLoader(DuplicateTracedClasses.class.getClassLoader());
		Class<?> clz1 = cl1.loadClass(DuplicateTracedClasses.class.getName());
		Class<?> clz2 = cl2.loadClass(DuplicateTracedClasses.class.getName());
		Method m1 = clz1.getDeclaredMethod("test", String.class);
		Method m2 = clz2.getDeclaredMethod("test", String.class);
		m1.invoke(null, "Loader 1");
		m2.invoke(null, "Loader 2");
		test("Default loader");
	}

	public static void test(String msg) {
		// Nothing to do.
	}
}
