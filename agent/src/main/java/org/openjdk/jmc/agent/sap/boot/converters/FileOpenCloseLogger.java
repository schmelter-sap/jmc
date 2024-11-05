package org.openjdk.jmc.agent.sap.boot.converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.IdentityHashMap;

import org.openjdk.jmc.agent.sap.boot.commands.Command;
import org.openjdk.jmc.agent.sap.boot.commands.CommandArguments;
import org.openjdk.jmc.agent.sap.boot.util.Dumps;
import org.openjdk.jmc.agent.sap.boot.util.JdkLogging;

public class FileOpenCloseLogger {

	private static IdentityHashMap<Object, String> mapping = new IdentityHashMap<>();
	private static final ThreadLocal<String> pathKey = new ThreadLocal<String>();

	static {
		Dumps.registerDump(new Command("openFiles", "Dump the currently open files by Java."), null,
				(CommandArguments args) -> printOpenFiles(args));
	}

	public synchronized static boolean openFileInputStream(FileInputStream stream) {
		assert !mapping.containsKey(stream);
		assert pathKey.get() != null;
		mapping.put(stream, pathKey.get());
		pathKey.remove();

		return true;
	}

	public synchronized static String openFileInputStream(File file) {
		assert pathKey.get() == null;
		String result = file.getAbsolutePath();
		pathKey.set(result);

		return result;
	}

	public synchronized static String closeFileInputStream(FileInputStream stream) {
		assert pathKey.get() == null;
		assert mapping.containsKey(stream);
		String result = mapping.get(stream);
		mapping.remove(stream);

		return result;
	}

	public synchronized static boolean openFileOutputStream(FileOutputStream stream) {
		assert !mapping.containsKey(stream);
		assert pathKey.get() != null;
		mapping.put(stream, pathKey.get());
		pathKey.remove();

		return true;
	}

	public synchronized static boolean openFileOutputStream(boolean append) {
		assert pathKey.get() != null;

		return append;
	}

	public synchronized static String openFileOutputStream(File file) {
		assert pathKey.get() == null;
		String result = file.getAbsolutePath();
		pathKey.set(result);

		return result;
	}

	public synchronized static String closeFileOutputStream(FileOutputStream stream) {
		assert pathKey.get() == null;
		assert mapping.containsKey(stream);
		String result = mapping.get(stream);
		mapping.remove(stream);

		return result;
	}

	public static boolean printOpenFiles(CommandArguments args) {
		IdentityHashMap<Object, String> copy;

		// Make a copy first, since logging might open new files.
		synchronized (FileOpenCloseLogger.class) {
			copy = new IdentityHashMap<Object, String>(mapping);
		}

		JdkLogging.log(args, copy.size() + " file(s) currently opened.");

		return false;
	}
}
