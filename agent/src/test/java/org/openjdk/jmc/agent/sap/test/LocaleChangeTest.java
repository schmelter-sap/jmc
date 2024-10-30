package org.openjdk.jmc.agent.sap.test;

import java.util.Locale;
import java.util.Locale.Category;

public class LocaleChanger {

	public static void main(String[] args) {
		Locale.setDefault(Locale.CANADA);
		Locale.setDefault(Locale.TAIWAN);
		Locale.setDefault(Locale.TAIWAN);
		Locale.setDefault(Locale.CANADA);
		Locale.setDefault(Category.DISPLAY, Locale.CHINA);
		Locale.setDefault(Category.DISPLAY, Locale.CHINA);
		Locale.setDefault(Category.DISPLAY, Locale.FRANCE);
		Locale.setDefault(Category.FORMAT, Locale.ITALIAN);
		Locale.setDefault(Category.FORMAT, Locale.ITALIAN);
		Locale.setDefault(Category.FORMAT, Locale.CANADA_FRENCH);
	}
}
