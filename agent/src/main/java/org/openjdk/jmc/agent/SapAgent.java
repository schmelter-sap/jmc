/*
 * Copyright (c) 2024 SAP SE. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

package org.openjdk.jmc.agent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openjdk.jmc.agent.impl.DefaultTransformRegistry;
import org.openjdk.jmc.agent.jmx.AgentManagementFactory;
import org.openjdk.jmc.agent.sap.boot.commands.Command;
import org.openjdk.jmc.agent.sap.boot.commands.Commands;
import org.openjdk.jmc.agent.sap.boot.converters.GenericLogger;
import org.openjdk.jmc.agent.sap.boot.util.Dumps;
import org.openjdk.jmc.agent.util.ModuleUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SapAgent {

	private static final String CONFIGS_PATH = "org/openjdk/jmc/agent/sap/";
	private static Logger logger = Logger.getLogger(SapAgent.class.getName());
	private static Instrumentation instr;
	private static boolean addedBootJar = false;

	public static void premain(String agentArguments, Instrumentation instrumentation) throws Exception {
		System.out.println("Called Agent with " + agentArguments); //$NON-NLS-1$
		agentmain(agentArguments, instrumentation);
	}

	public static void agentmain(String agentArguments, Instrumentation instrumentation) throws Exception {
		ModuleUtils.openUnsafePackage(instrumentation);
		instr = instrumentation;

		if (agentArguments == null || agentArguments.trim().length() == 0) {
			initializeAgent(null, instrumentation);
		} else {
			if (agentArguments.equals("help")) {
				ensureBootJarAdded();
				Commands.printAllCommands();
				System.exit(0);
			}

			if (agentArguments.startsWith("dump=")) {
				ensureBootJarAdded();
				Dumps.performDump(agentArguments.substring(5));
				return;
			}

			try (InputStream stream = new ByteArrayInputStream(getXmlConfig(agentArguments))) {
				initializeAgent(stream, instrumentation);
			} catch (XMLStreamException | IOException | XMLValidationException e) {
				logger.log(Level.SEVERE, "Failed to read jfr probe definitions from " + agentArguments, e); //$NON-NLS-1$
			}
		}
	}

	private static void ensureBootJarAdded() throws IOException {
		if (addedBootJar) {
			return;
		}

		ClassLoader cl = SapAgent.class.getClassLoader();
		addedBootJar = true;

		// Find out where the agent jar is, since the boot jar should live
		// there too.
		URL url = cl.getResource(SapAgent.class.getName().replace('.', '/') + ".class");
		String file = url.getFile();

		if (!file.startsWith("file:/")) {
			throw new RuntimeException("Could not determine agent jar from " + file);
		}

		String jar = file.substring(6, file.indexOf(".jar!")) + "-boot.jar";

		if (!new File(jar).canRead()) {
			throw new RuntimeException("Could not find boot jar at " + jar);
		}

		instr.appendToBootstrapClassLoaderSearch(new JarFile(jar));
	}

	private static InputStream getStreamForConfig(String config) throws Exception {
		try {
			return new FileInputStream(config);
		} catch (FileNotFoundException e) {
			ensureBootJarAdded();

			ClassLoader cl = SapAgent.class.getClassLoader();
			InputStream is = cl.getResourceAsStream(CONFIGS_PATH + config + ".xml");

			if (is != null) {
				return is;
			}

			throw e;
		}
	}

	private static StringBuilder addCommandOptions(String commandName, StringBuilder options) {
		if (addedBootJar) {
			// If the boot jar was not yet added, we cannot have encountered a supported command.
			Command command = Commands.getCommand(commandName);

			if (command != null) {
				command.preTraceInit();
				System.setProperty("com.sap.jvm.jmcagent.options." + commandName, options.toString());
			}
		}

		return new StringBuilder();
	}

	private static byte[] getXmlConfig(String agentArguments) throws Exception {
		String[] parts = agentArguments.split("(?<!\\\\),");
		StringBuilder configProp = new StringBuilder();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();

		// We merge into a dummy which is guaranteed to contain all the stuff we expect.
		// Thanks to spotless for forcing this to look like shit.
		String DUMMY = "<jfragent>\n" + "    <config>\n" + "        <classprefix>__JFREvent</classprefix>\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ "        <allowtostring>false</allowtostring>\n" + "        <allowconverter>false</allowconverter>\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "    </config>\n" + "    <events>\n" + "    </events>\n" + "</jfragent>\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		Document base = factory.newDocumentBuilder()
				.parse(new ByteArrayInputStream(DUMMY.getBytes(StandardCharsets.UTF_8)));
		Node events = base.getElementsByTagName("events").item(0); //$NON-NLS-1$
		String requestedPrefix = null;
		String configName = null;

		for (String part : parts) {
			if (part.equals("help")) {
				configProp.append(part).append(',');
			} else if (part.startsWith(GenericLogger.GENERIC_COMMAND_PREFIX)) {
				// Do nothing, just pick up the options and make sure the converter is accessible.
				configProp = addCommandOptions(configName, configProp);
				ensureBootJarAdded();
			} else if (part.indexOf('=') > 0) {
				configProp.append(part).append(',');
			} else {
				Document doc = factory.newDocumentBuilder().parse(getStreamForConfig(part));
				configProp = addCommandOptions(configName, configProp);
				configName = part;

				if (getBool(doc, "allowtostring", false)) { //$NON-NLS-1$
					setText(base, "allowtostring", "true"); //$NON-NLS-1$ //$NON-NLS-2$
				}

				if (getBool(doc, "allowconverter", false)) { //$NON-NLS-1$
					setText(base, "allowconverter", "true"); //$NON-NLS-1$ //$NON-NLS-2$
				}

				String prefix = getString(doc, "classprefix"); //$NON-NLS-1$

				if (requestedPrefix == null) {
					requestedPrefix = prefix;
					setText(base, "classprefix", prefix); //$NON-NLS-1$
				} else if ((prefix != null) && !requestedPrefix.equals(prefix)) {
					System.out.println("Conflicting class prefixes " + prefix + " vs. " + requestedPrefix); //$NON-NLS-1$ //$NON-NLS-2$
				}

				NodeList list = doc.getElementsByTagName("event"); //$NON-NLS-1$

				for (int j = 0; j < list.getLength(); ++j) {
					Node toAdd = list.item(j).cloneNode(true);
					events.getOwnerDocument().adoptNode(toAdd);
					events.appendChild(toAdd);
				}
			}
		}

		addCommandOptions(configName, configProp);

		// If we added our boot jar, check the options now.
		if (addedBootJar) {
			Class<?> commands = Class.forName("org.openjdk.jmc.agent.sap.boot.commands.Commands", true, null);
			java.lang.reflect.Method checkOptions = commands.getDeclaredMethod("checkCommands");

			if (!(Boolean) checkOptions.invoke(null)) {
				System.exit(1);
			}
		}

		TransformerFactory tf = TransformerFactory.newInstance();
		javax.xml.transform.Transformer trans = tf.newTransformer();
		StringWriter sw = new StringWriter();
		trans.transform(new DOMSource(base), new StreamResult(sw));
		return sw.toString().getBytes();
	}

	private static void setText(Document doc, String tag, String text) {
		NodeList list = doc.getElementsByTagName(tag);

		if (list.getLength() == 1) {
			list.item(0).setTextContent(text);
		}
	}

	private static String getString(Document doc, String tag) {
		NodeList list = doc.getElementsByTagName(tag);

		if (list.getLength() != 1) {
			return null;
		}

		return list.item(0).getTextContent();
	}

	private static boolean getBool(Document doc, String tag, boolean fallback) {
		String text = getString(doc, tag);

		return text == null ? fallback : Boolean.parseBoolean(text);
	}

	public static void initializeAgent(InputStream configuration, Instrumentation instrumentation)
			throws XMLStreamException, XMLValidationException {
		TransformRegistry registry = configuration != null ? DefaultTransformRegistry.from(configuration)
				: DefaultTransformRegistry.empty();
		instrumentation.addTransformer(new SapTransformer(registry), true);
		AgentManagementFactory.createAndRegisterAgentControllerMBean(instrumentation,
				new SapTransformRegistry(registry));

		List<Class<?>> classesToRetransform = new ArrayList<>();
		Set<String> clazzes = registry.getClassNames().stream().map((name) -> name.replace('/', '.'))
				.collect(Collectors.toSet());

		for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
			if (clazzes.contains(clazz.getName())) {
				classesToRetransform.add(clazz);
				System.out.println("Class to retransform: " + clazz); //$NON-NLS-1$
			}
		}

		try {
			instrumentation.retransformClasses(classesToRetransform.toArray(new Class<?>[0]));
		} catch (UnmodifiableClassException e) {
			logger.log(Level.SEVERE, "Unable to retransform classes", e); //$NON-NLS-1$
		}
	}
}
