package org.openjdk.jmc.agent.sap.test;

import java.util.Locale;
import java.util.Locale.Category;

public class LocaleChangeTest extends TestBase {

	public static void main(String[] args) {
		new LocaleChangeTest().dispatch(args);
	}

	@Override
	protected void runAllTests() throws Exception {
		JavaAgentRunner runner = getRunner("traceLocaleChange,logDest=stdout");
		runner.start("changeLocale");
		runner.waitForEnd();
		assertLinesContainsRegExp(runner.getStdoutLines(),
				"Changed default locale for category 'DISPLAY' from .+ to 'English [(]Canada[)]'");
		assertLinesContainsRegExp(runner.getStdoutLines(),
				"Changed default locale for category 'FORMAT' from .* to 'English [(]Canada[)]'");
		assertLinesContains(runner.getStdoutLines(),
				"Changed default locale for category 'DISPLAY' from 'English (Canada)' to 'Chinese (Taiwan)'.");
		assertLinesContains(runner.getStdoutLines(),
				"Changed default locale for category 'FORMAT' from 'English (Canada)' to 'Chinese (Taiwan)'.");
		assertLinesNotContains(runner.getStdoutLines(),
				"Changed default locale for category 'DISPLAY' from 'Chinese (Taiwan)' to 'Chinese (Taiwan)'.");
		assertLinesNotContains(runner.getStdoutLines(),
				"Changed default locale for category 'FORMAT' from 'Chinese (Taiwan)' to 'Chinese (Taiwan)'.");
		assertLinesContains(runner.getStdoutLines(),
				"Changed default locale for category 'DISPLAY' from 'Chinese (Taiwan)' to 'English (Canada)'");
		assertLinesContains(runner.getStdoutLines(),
				"Changed default locale for category 'FORMAT' from 'Chinese (Taiwan)' to 'English (Canada)'.");
		assertLinesContains(runner.getStdoutLines(),
				"Changed default locale for category 'DISPLAY' from 'English (Canada)' to 'Chinese (China)'.");
		assertLinesNotContains(runner.getStdoutLines(),
				"Changed default locale for category 'DISPLAY' from 'Chinese (China)' to 'Chinese (China)'.");
		assertLinesContains(runner.getStdoutLines(),
				"Changed default locale for category 'DISPLAY' from 'Chinese (China)' to 'French (France)'.");
		assertLinesContains(runner.getStdoutLines(),
				"Changed default locale for category 'FORMAT' from 'English (Canada)' to 'Italian'.");
		assertLinesNotContains(runner.getStdoutLines(),
				"Changed default locale for category 'FORMAT' from 'Italian' to 'Italian'.");
		assertLinesContains(runner.getStdoutLines(),
				"Changed default locale for category 'FORMAT' from 'Italian' to 'French (Canada)'.");
	}

	public static void changeLocale() {
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
