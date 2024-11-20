package org.openjdk.jmc.agent.sap.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class OpenFileStatisticTest extends TestBase {

	public static void main(String[] args) {
		new OpenFileStatisticTest().dispatch(args);
	}

	@Override
	protected void runAllTests() throws Exception {
		JavaAgentRunner runner = getRunner("traceOpenFiles,logDest=stdout");
		runner.start("test");
		runner.waitForDone(1);
		runner.loadAgent("dump=openFiles,logDest=stderr");

		if (!smokeTestsOnly()) {
			runner.waitForDone();
			runner.loadAgent("dump=openFiles,logDest=stdout");
		}

		runner.kill();

		String[] stderr = runner.getStderrLines();
		assertLinesContains(stderr, getFileName(1) + "', mode 'w'");
		assertLinesContains(stderr, getFileName(2) + "', mode 'wa'");
		assertLinesContains(stderr, getFileName(1) + "', mode 'r'");
		assertLinesContains(stderr, getFileName(3) + "', mode 'w'");
		assertLinesContains(stderr, getFileName(4) + "', mode 'wa'");
		assertLinesContains(stderr, getFileName(2) + "', mode 'r'");
		assertLinesContains(stderr, getFileName(5) + "', mode 'rw'");
		assertLinesContains(stderr, getFileName(5) + "', mode 'r'");
		assertLinesContains(stderr, getFileName(6) + "', mode 'rw'");
		assertLinesContains(stderr, getFileName(6) + "', mode 'r'");
		assertLinesContainsRegExp(stderr, "Printed [0-9]+ of [0-9][0-9]+ file.* currently opened");

		if (!smokeTestsOnly()) {
			assertLinesNotContains(runner.getStdoutLines(), getFileName(1));
		}
	}

	public static String getFileName(int index) {
		return "testopen" + index + ".txt";
	}

	public static File getFile(int index) {
		return new File(getFileName(index));
	}

	@SuppressWarnings("resource")
	public void test() throws IOException {
		FileOutputStream fos1 = new FileOutputStream(getFileName(1));
		FileOutputStream fos2 = new FileOutputStream(getFileName(2), true);
		FileInputStream fis1 = new FileInputStream(getFileName(1));
		FileOutputStream fos3 = new FileOutputStream(getFile(3));
		FileOutputStream fos4 = new FileOutputStream(getFile(4), true);
		FileInputStream fis2 = new FileInputStream(getFile(2));
		RandomAccessFile raf1 = new RandomAccessFile(getFileName(5), "rw");
		RandomAccessFile raf2 = new RandomAccessFile(getFileName(5), "r");
		RandomAccessFile raf3 = new RandomAccessFile(getFile(6), "rw");
		RandomAccessFile raf4 = new RandomAccessFile(getFile(6), "r");
		FileChannel fc = FileChannel.open(getFile(1).toPath());
		fc.read(ByteBuffer.allocate(10));

		done(1, 3000);

		fos1.close();
		fos2.close();
		fos3.close();
		fos4.close();
		fis1.close();
		fis2.close();
		raf1.close();
		raf2.close();
		raf3.close();
		raf4.close();
		fc.close();

		for (int i = 1; i <= 6; ++i) {
			File file = getFile(i);

			while (file.exists()) {
				file.delete();
			}
		}

		FileInputStream dummy = null;

		try {
			dummy = new FileInputStream(getFileName(1));
			throw new RuntimeException("Should not be able to open the file");
		} catch (FileNotFoundException e) {
			// This is what we expect.
		}

		done();
		assertNotNull(dummy);
	}
}
