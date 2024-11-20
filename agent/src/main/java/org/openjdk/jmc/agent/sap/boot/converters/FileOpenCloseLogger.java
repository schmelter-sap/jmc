package org.openjdk.jmc.agent.sap.boot.converters;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.openjdk.jmc.agent.sap.boot.commands.Command;
import org.openjdk.jmc.agent.sap.boot.commands.CommandArguments;
import org.openjdk.jmc.agent.sap.boot.commands.OpenFileStatisticCommand;
import org.openjdk.jmc.agent.sap.boot.util.Dumps;
import org.openjdk.jmc.agent.sap.boot.util.JdkLogging;

public class FileOpenCloseLogger {

	private static HashMap<Key, Entry> mapping = new HashMap<>();
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
			mapping.put(new Key(stream), new Entry(pathKey.get(), "r", new Exception()));
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
		Key key = new Key(stream);
		Entry entry = mapping.get(key);

		if (entry != null) {
			mapping.remove(key);

			return entry.path;
		}

		return UNKNOWN_FILE;
	}

	public static synchronized boolean openFileOutputStream(FileOutputStream stream) {
		if ((pathKey.get() != null) && (modeKey.get() != null)) {
			mapping.put(new Key(stream), new Entry(pathKey.get(), modeKey.get(), new Exception()));
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
		Key key = new Key(stream);
		Entry entry = mapping.get(key);

		if (entry != null) {
			mapping.remove(key);

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
			mapping.put(new Key(file), new Entry(pathKey.get(), modeKey.get(), new Exception()));
		}

		return true;
	}

	public static synchronized String closeRandomAccessFile(RandomAccessFile file) {
		Key key = new Key(file);
		Entry entry = mapping.get(key);

		if (entry != null) {
			mapping.remove(key);

			return entry.path;
		}

		return UNKNOWN_FILE;
	}

	public static boolean printOpenFiles(CommandArguments args) {
		HashMap<Key, Entry> copy;

		// Make a copy first, since logging might open new files.
		synchronized (FileOpenCloseLogger.class) {
			copy = new HashMap<Key, Entry>(mapping);
		}

		for (Key key : new ArrayList<Key>(copy.keySet())) {
			Object obj = key.getObject();

			// If the ref is dead, remove it from the original map.
			if (obj == null) {
				synchronized (FileOpenCloseLogger.class) {
					mapping.remove(key);
				}
				// Fall through to remove from copy as well.
			}

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
				copy.remove(key);
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

	private static class Key {
		private final WeakReference<Object> ref;
		private final int hashCode;

		public Key(Object obj) {
			ref = new WeakReference<Object>(obj);
			hashCode = System.identityHashCode(obj);
		}

		public Object getObject() {
			return ref.get();
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof Key) {
				if (this == other) {
					// Needed so we can remove a dead key from the hash map.
					return true;
				}

				Object thisKey = ref.get();
				Object otherKey = ((Key) other).ref.get();

				if ((thisKey != null) && (otherKey != null)) {
					return thisKey == otherKey;
				}
			}

			return false;
		}
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
