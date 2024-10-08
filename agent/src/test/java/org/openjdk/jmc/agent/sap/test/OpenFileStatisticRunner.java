package org.openjdk.jmc.agent.sap.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class OpenFileStatisticRunner {

	public static void main(String[] args) throws IOException {
		String fileName = "testopen.txt";
		File file = new File(fileName);
		FileOutputStream fos = new FileOutputStream(fileName);
		fos.close();
		fos = new FileOutputStream(fileName, true);
		fos.close();
		FileInputStream fis = new FileInputStream(fileName);
		fis.close();
		fos = new FileOutputStream(file);
		fos.close();
		fos = new FileOutputStream(file, true);
		fos.close();
		fis = new FileInputStream(file);
		fis.close();
		FileChannel fc = FileChannel.open(file.toPath());
		fc.read(ByteBuffer.allocate(10));
		fc.close();
		file.delete();

		try (FileInputStream dummy = new FileInputStream(file)) {
			throw new RuntimeException("Should not be able to open the file");
		} catch (FileNotFoundException e) {
			// This is what we expect.
		}
	}
}
