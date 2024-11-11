package org.openjdk.jmc.agent.sap.boot.converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.IdentityHashMap;
import java.util.regex.Pattern;

import org.openjdk.jmc.agent.sap.boot.commands.Command;
import org.openjdk.jmc.agent.sap.boot.commands.CommandArguments;
import org.openjdk.jmc.agent.sap.boot.commands.OpenFileStatisticCommand;
import org.openjdk.jmc.agent.sap.boot.util.Dumps;
import org.openjdk.jmc.agent.sap.boot.util.JdkLogging;

public class FileOpenCloseLogger {

	private static IdentityHashMap<Object, Entry> mapping = new IdentityHashMap<>();
	private static final ThreadLocal<String> pathKey = new ThreadLocal<String>();
	private static final ThreadLocal<String> modeKey = new ThreadLocal<String>();

	public static final Command command = OpenFileStatisticCommand.enableCommand;

	static {
		Dumps.registerDump(OpenFileStatisticCommand.dumpCommand, null, (CommandArguments args) -> printOpenFiles(args));
		Dumps.registerDump(command, "Open files", (CommandArguments args) -> printOpenFiles(args));
	}

	public static synchronized boolean openFileInputStream(FileInputStream stream) {
		assert !mapping.containsKey(stream);
		assert pathKey.get() != null;
		assert modeKey.get() == null;
		mapping.put(stream, new Entry(pathKey.get(), "r", new Exception()));
		pathKey.remove();

		return true;
	}

	public static synchronized String openFileInputStream(File file) {
		assert pathKey.get() == null;
		String result = file.getAbsolutePath();
		pathKey.set(result);

		return result;
	}

	public static synchronized String closeFileInputStream(FileInputStream stream) {
		assert pathKey.get() == null;
		assert mapping.containsKey(stream);
		String result = mapping.get(stream).path;
		mapping.remove(stream);

		return result;
	}

	public static synchronized boolean openFileOutputStream(FileOutputStream stream) {
		assert !mapping.containsKey(stream);
		assert pathKey.get() != null;
		assert modeKey.get() != null;
		mapping.put(stream, new Entry(pathKey.get(), modeKey.get(), new Exception()));
		pathKey.remove();
		modeKey.remove();

		return true;
	}

	public static synchronized boolean openFileOutputStream(boolean append) {
		assert pathKey.get() != null;
		assert modeKey.get() == null;
		modeKey.set(append ? "wa" : "w");

		return append;
	}

	public static synchronized String openFileOutputStream(File file) {
		assert pathKey.get() == null;
		String result = file.getAbsolutePath();
		pathKey.set(result);

		return result;
	}

	public static synchronized String closeFileOutputStream(FileOutputStream stream) {
		assert pathKey.get() == null;
		assert mapping.containsKey(stream);
		String result = mapping.get(stream).path;
		mapping.remove(stream);

		return result;
	}

	public static synchronized String openRandomAccessFile(File file) {
		assert pathKey.get() == null;
		String result = file.getAbsolutePath();
		pathKey.set(result);

		return result;
	}

	public static synchronized String openRandomAccessFile(String file) {
		return openRandomAccessFile(new File(file));
	}

	public static synchronized String openRandomAccessFileMode(String mode) {
		assert modeKey.get() == null;
		modeKey.set(mode);

		return mode;
	}

	public static synchronized boolean openRandomAccessFile(RandomAccessFile file) {
		assert pathKey.get() != null;
		assert modeKey.get() != null;
		assert !mapping.containsKey(file);
		mapping.put(file, new Entry(pathKey.get(), modeKey.get(), new Exception()));

		return true;
	}

	public static synchronized String closeRandomAccessFile(RandomAccessFile file) {
		assert mapping.containsKey(file);
		String path = mapping.get(file).path;
		mapping.remove(file);

		return path;
	}

	public static boolean printOpenFiles(CommandArguments args) {
		IdentityHashMap<Object, Entry> copy;

		// Make a copy first, since logging might open new files.
		synchronized (FileOpenCloseLogger.class) {
			copy = new IdentityHashMap<Object, Entry>(mapping);
		}

		Pattern mustContain = args.getPattern(OpenFileStatisticCommand.MUST_CONTAIN, null);
		Pattern mustNotContain = args.getPattern(OpenFileStatisticCommand.MUST_NOT_CONTAIN, null);
		int printed = 0;

		for (Entry entry : copy.values()) {
			if (mustContain != null) {
				if (!mustContain.matcher(entry.path).find()) {
					continue;
				}
			}

			if (mustNotContain != null) {
				if (mustNotContain.matcher(entry.path).find()) {
					continue;
				}
			}

			JdkLogging.log(args, "File '" + entry.path + "', mode '" + entry.mode + "'");
			JdkLogging.logStack(args, entry.stack, 1);
			printed += 1;
		}

		if (printed > 0) {
			JdkLogging.log(args, "Printed " + printed + " of " + copy.size() + " file(s) currently opened.");
		}

		return printed > 0;
	}

	private static class Entry {
		public final String path;
		public final String mode;
		public final Exception stack;

		public Entry(String path, String mode, Exception stack) {
			this.path = path;
			this.mode = mode;
			this.stack = stack;
		}
	}
}
