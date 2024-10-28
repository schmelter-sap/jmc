package org.openjdk.jmc.agent.sap.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class JavaAgentRunner {

	private final String classToRun;
	private final String options;
	private final String[] vmArgs;
	private final StringBuilder stdout;
	private final StringBuilder stderr;
	private String commandLine = "java";
	private Thread stdoutWorker;
	private Thread stderrWorker;
	private Process process;
	private static boolean dumpOnExit;
	private static int debugPort = -1;
	private static int MAX_WAIT_TIME = 60;

	private static final String AGENT_NAME = "agent-1.0.1-SNAPSHOT.jar";

	public JavaAgentRunner(Class<?> classToRun, String options, String ... vmArgs) {
		this.classToRun = classToRun.getName();
		this.options = options;
		this.vmArgs = vmArgs;
		this.stdout = new StringBuilder();
		this.stderr = new StringBuilder();
	}

	private ArrayList<String> getArgs(String[] javaArgs) {
		ArrayList<String> args = new ArrayList<>();
		args.add(getExe("java"));
		args.add("-javaagent:" + getAgent() + "=" + options);
		args.add("-cp");
		args.add(System.getProperty("java.class.path"));

		for (String vmArg : vmArgs) {
			args.add(vmArg);
		}

		if (debugPort >= 0) {
			args.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + debugPort);
			System.out.println("Waiting for debugger on port " + debugPort + ".");
		}

		args.add(classToRun);

		for (String javaArg : javaArgs) {
			args.add(javaArg);
		}

		commandLine = String.join(" ", args);

		return args;
	}

	public String getCommandLine() {
		return commandLine;
	}

	private static String getAgent() {
		File file = new File(AGENT_NAME);

		if (file.exists()) {
			return file.getAbsolutePath();
		}

		file = new File("target" + File.separator + AGENT_NAME);

		if (file.exists()) {
			return file.getAbsolutePath();
		}

		String javaHome = System.getProperty("java.home");
		file = new File(javaHome + File.separator + "lib" + File.separator + AGENT_NAME);

		if (file.exists()) {
			return file.getAbsolutePath();
		}

		throw new RuntimeException("Could not find agent " + AGENT_NAME + " (WD " + System.getProperty("user.dir"));
	}

	private static String getExe(String name) {
		String javaHome = System.getProperty("java.home");
		File java = new File(javaHome + File.separator + "bin" + File.separator + name);

		if (java.exists() && java.canExecute()) {
			return java.getAbsolutePath();
		}

		java = new File(java.getAbsolutePath() + ".exe");

		if (java.exists() && java.canExecute()) {
			return java.getAbsolutePath();
		}

		throw new RuntimeException("Could not locate '" + name + "'");
	}

	public void start(String ... javaArgs) throws IOException {
		stdout.setLength(0);
		stderr.setLength(0);

		ProcessBuilder pb = new ProcessBuilder(getArgs(javaArgs));
		commandLine = String.join(" ", pb.command());
		process = pb.start();
		stdoutWorker = new Thread(new OutputReader(process.getInputStream(), stdout));
		stdoutWorker.setDaemon(true);
		stdoutWorker.start();
		stderrWorker = new Thread(new OutputReader(process.getErrorStream(), stderr));
		stderrWorker.setDaemon(true);
		stderrWorker.start();
	}

	public void waitForStdout(String tag) {
		waitFor(stdout, tag);
	}

	public void waitForStderr(String tag) {
		waitFor(stderr, tag);
	}

	public void loadAgent(String options) throws IOException {
		if (process == null) {
			throw new IOException("No process");
		}

		if ((options.indexOf('=') >= 0) && System.getProperty("os.name").toLowerCase().startsWith("win")) {
			options = "'" + options + "'"; // Windows can remove everything after the equals.
		}

		long pid = process.pid();
		ArrayList<String> args = new ArrayList<>();
		args.add(getExe("jcmd"));
		args.add(Long.toString(pid));
		args.add("JVMTI.agent_load");
		args.add(getAgent());
		args.add(options);
		ProcessBuilder pb = new ProcessBuilder(args);
		pb.redirectError(Redirect.DISCARD);
		pb.redirectOutput(Redirect.DISCARD);
		Process p = pb.start();

		while (true) {
			try {
				int result = p.waitFor();

				if (result != 0) {
					kill();
					dumpOnExit();
					throw new RuntimeException(pb.command().toString() + " return with exit code " + result);
				}

				return;
			} catch (InterruptedException e) {
				// retry
			}
		}
	}

	public int waitForEnd() {
		long t1 = System.currentTimeMillis();

		while (true) {
			try {
				boolean exited = process.waitFor(5, TimeUnit.SECONDS);

				if (!exited && !checkWaitTimeout(t1)) {
					throw new RuntimeException("Waited over one minute for the process to end.");
				}

				if (!exited) {
					continue;
				}

				int result = process.exitValue();

				if (result != 0) {
					dumpOnExit();
				}

				return result;
			} catch (InterruptedException e) {
				// Retry
			}
		}
	}

	private void waitForOutput() {
		try {
			stdoutWorker.join();
			stderrWorker.join();
		} catch (InterruptedException e) {
			// Ignore
		}
	}

	public String[] getStdoutLines() {
		return getLines(stdout);
	}

	public String[] getStderrLines() {
		return getLines(stderr);
	}

	private String[] getLines(StringBuilder out) {
		String raw;

		synchronized (out) {
			raw = out.toString();
		}

		return raw.split("[\r\n]+");
	}

	private void dumpLines(StringBuilder sb) {
		String raw;

		synchronized (sb) {
			raw = sb.toString();
		}

		System.out.println(raw);
	}

	public static void setDumpOnExit(boolean dumpOnExit) {
		JavaAgentRunner.dumpOnExit = dumpOnExit;
	}

	public static void setDebugPort(int port) {
		debugPort = port;
	}

	private void dumpOnExit() {
		if (dumpOnExit) {
			waitForOutput();
			System.out.println("Command line: " + commandLine);
			System.out.println("Output on stdout:");
			dumpLines(stdout);
			System.out.println("Output on stderr:");
			dumpLines(stderr);
		}
	}

	public void kill() {
		int result = 0;

		try {
			if (process.waitFor(10, TimeUnit.MILLISECONDS)) {
				result = process.exitValue();
			} else {
				process.destroyForcibly();
				result = -1;
			}

		} catch (InterruptedException e) {
			// Ignore.
		}

		if (result != 0) {
			dumpOnExit();
		}
	}

	private boolean checkWaitTimeout(long t1) {
		long elapsed = (System.currentTimeMillis() - t1) / 1000;

		// We should never wait this long.
		if (elapsed > MAX_WAIT_TIME) {
			kill();

			return false;
		}

		return true;
	}

	private void waitFor(StringBuilder output, String tag) {
		long t1 = System.currentTimeMillis();

		while (true) {
			synchronized (output) {
				if (output.indexOf(tag) >= 0) {
					return;
				}

				if (!checkWaitTimeout(t1)) {
					throw new RuntimeException("Waited over one minute for '" + tag + "'");
				}

				try {
					output.wait(10000);
				} catch (InterruptedException e) {
					// Ignore.
				}
			}
		}
	}

	private static class OutputReader implements Runnable {
		private final StringBuilder out;
		private final InputStream is;

		public OutputReader(InputStream is, StringBuilder out) {
			this.is = is;
			this.out = out;
		}

		public void run() {
			try {
				byte[] buf = new byte[8192];
				int read;

				while ((read = is.read(buf)) > 0) {

					if (read > 0) {
						byte[] part = new byte[read];
						System.arraycopy(buf, 0, part, 0, read);
						String toAppend = new String(part, StandardCharsets.ISO_8859_1);

						synchronized (out) {
							out.append(toAppend);
							out.notifyAll();
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
