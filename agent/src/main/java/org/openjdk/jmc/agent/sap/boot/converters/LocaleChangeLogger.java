package org.openjdk.jmc.agent.sap.boot.converters;

import java.util.Locale;

import org.openjdk.jmc.agent.sap.boot.commands.Command;
import org.openjdk.jmc.agent.sap.boot.commands.CommandArguments;
import org.openjdk.jmc.agent.sap.boot.commands.OutputCommand;
import org.openjdk.jmc.agent.sap.boot.util.JdkLogging;

public class LocaleChangeLogger {

	private static final ThreadLocal<Locale.Category> categoryKey = new ThreadLocal<Locale.Category>();

	public static Command command = new OutputCommand("traceLocaleChange", "Logs when the default locale is changed.");

	public static String logDefaultLocaleCategoryChange(Locale.Category newCategory) {
		assert categoryKey.get() == null;
		categoryKey.set(newCategory);

		return newCategory.name();
	}

	public static String logDefaultLocale(Locale.Category newCategory) {
		assert categoryKey.get() != null;

		return Locale.getDefault(newCategory).getDisplayName(Locale.ENGLISH);
	}

	public static String logDefaultLocalChange(Locale newLocale) {
		assert categoryKey.get() != null;

		return newLocale.getDisplayName(Locale.ENGLISH);
	}

	public static boolean changesDefaultLocale(Locale newLocale) {
		assert categoryKey.get() != null;
		Locale oldLocale = Locale.getDefault(categoryKey.get());
		boolean result = !oldLocale.equals(newLocale);

		if (result) {
			JdkLogging.logWithStack(new CommandArguments(command),
					"Changed default locale for category '" + categoryKey.get().name() + "' from '"
							+ oldLocale.getDisplayName(Locale.ENGLISH) + "' to '"
							+ newLocale.getDisplayName(Locale.ENGLISH) + "'.",
					3);
		}

		categoryKey.remove();

		return result;
	}
}
