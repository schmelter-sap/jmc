package org.openjdk.jmc.agent.sap.boot.converters;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
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
	private static final String UNKNOWN_FILE = "<unknown file>";

	public static final Command command = OpenFileStatisticCommand.enableCommand;

	static {
		Dumps.registerDump(OpenFileStatisticCommand.dumpCommand, null, (CommandArguments args) -> printOpenFiles(args));
		Dumps.registerDump(command, "Open files", (CommandArguments args) -> printOpenFiles(args));
	}

	public static synchronized boolean openFileInputStream(FileInputStream stream) {
		if (pathKey.get() != null) {
			mapping.put(stream, new Entry(pathKey.get(), "r", new Exception()));
			pathKey.remove();
		}

		return true;
	}

	public static synchronized String openFileInputStream(File file) {
		String result = file.getAbsolutePath();
		pathKey.set(result);

		return result;
	}

	public static synchronized String closeFileInputStream(FileInputStream stream) {
		Entry entry = mapping.get(stream);

		if (entry != null) {
			mapping.remove(stream);

			return entry.path;
		}

		return UNKNOWN_FILE;
	}

	public static synchronized boolean openFileOutputStream(FileOutputStream stream) {
		if ((pathKey.get() != null) && (modeKey.get() != null)) {
			mapping.put(stream, new Entry(pathKey.get(), modeKey.get(), new Exception()));
		}

		pathKey.remove();
		modeKey.remove();

		return true;
	}

	public static synchronized boolean openFileOutputStream(boolean append) {
		modeKey.set(append ? "wa" : "w");

		return append;
	}

	public static synchronized String openFileOutputStream(File file) {
		String result = file.getAbsolutePath();
		pathKey.set(result);

		return result;
	}

	public static synchronized String closeFileOutputStream(FileOutputStream stream) {
		Entry entry = mapping.get(stream);

		if (entry != null) {
			mapping.remove(stream);

			return entry.path;
		}

		return UNKNOWN_FILE;
	}

	public static synchronized String openRandomAccessFile(File file) {
		String result = file.getAbsolutePath();
		pathKey.set(result);

		return result;
	}

	public static synchronized String openRandomAccessFile(String file) {
		return openRandomAccessFile(new File(file));
	}

	public static synchronized String openRandomAccessFileMode(String mode) {
		modeKey.set(mode);

		return mode;
	}

	public static synchronized boolean openRandomAccessFile(RandomAccessFile file) {
		if ((pathKey.get() != null) && (modeKey.get() != null)) {
			mapping.put(file, new Entry(pathKey.get(), modeKey.get(), new Exception()));
		}

		return true;
	}

	public static synchronized String closeRandomAccessFile(RandomAccessFile file) {
		Entry entry = mapping.get(file);

		if (entry != null) {
			mapping.remove(file);

			return entry.path;
		}

		return UNKNOWN_FILE;
	}

	public static boolean printOpenFiles(CommandArguments args) {
		IdentityHashMap<Object, Entry> copy;

		// Make a copy first, since logging might open new files.
		synchronized (FileOpenCloseLogger.class) {
			copy = new IdentityHashMap<Object, Entry>(mapping);
		}

		for (Object obj : new ArrayList<Object>(copy.keySet())) {
			boolean remove = true;

			try {
				FileDescriptor fd = null;

				if (obj instanceof FileInputStream) {
					fd = ((FileInputStream) obj).getFD();
				} else if (obj instanceof FileOutputStream) {
					fd = ((FileOutputStream) obj).getFD();
				} else if (obj instanceof RandomAccessFile) {
					fd = ((RandomAccessFile) obj).getFD();
				}

				remove = (fd == null) || !fd.valid();
			} catch (IOException e) {
				// Remove too.
			}

			if (remove) {
				copy.remove(obj);
			}
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
