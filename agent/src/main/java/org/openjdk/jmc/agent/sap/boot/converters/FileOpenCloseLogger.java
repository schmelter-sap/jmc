package org.openjdk.jmc.agent.sap.boot.converters;

import java.io.File;
import java.io.FileInputStream;
import java.util.IdentityHashMap;

public class FileOpenCloseLogger {

	private static IdentityHashMap<Object, String> mapping = new IdentityHashMap<>();
	private static final ThreadLocal<String> pathKey = new ThreadLocal<String>();

	public synchronized static boolean openFileInputStream(FileInputStream stream) {
		boolean result = stream.getChannel().isOpen();

		if (result) {
			assert !mapping.containsKey(stream);
			assert pathKey.get() != null;
			mapping.put(stream, pathKey.get());
			pathKey.remove();
		} else {
			assert pathKey.get() == null;
		}

		return result;
	}

	public synchronized static String openFileInputStream(File file) {
		assert pathKey.get() == null;
		String result = file.getAbsolutePath();
		pathKey.set(result);

		return result;
	}

	public synchronized static String openFileInputStream(String file) {
		assert pathKey.get() == null;
		String result = new File(file).getAbsolutePath();
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
}
