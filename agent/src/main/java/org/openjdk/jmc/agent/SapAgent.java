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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
import org.openjdk.jmc.agent.util.IOToolkit;
import org.openjdk.jmc.agent.util.ModuleUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SapAgent {

	private static Logger logger = Logger.getLogger(SapAgent.class.getName());

	public static void premain(String agentArguments, Instrumentation instrumentation) throws Exception {
		System.out.println("Called Agent with " + agentArguments); //$NON-NLS-1$
		agentmain(agentArguments, instrumentation);
	}

	public static void agentmain(String agentArguments, Instrumentation instrumentation) throws Exception {
		ModuleUtils.openUnsafePackage(instrumentation);

		if (agentArguments == null || agentArguments.trim().length() == 0) {
			initializeAgent(null, instrumentation);
		} else {
			try (InputStream stream = new ByteArrayInputStream(getXmlConfig(agentArguments))) {
				initializeAgent(stream, instrumentation);
			} catch (XMLStreamException | IOException | XMLValidationException e) {
				logger.log(Level.SEVERE, "Failed to read jfr probe definitions from " + agentArguments, e); //$NON-NLS-1$
			}
		}
	}

	private static File getFileForConfig(String config) throws Exception {
		File absFile = new File(config);

		if (absFile.exists()) {
			return absFile;
		}

		// TODO: Handle files supplied by SapMachine.
		throw new FileNotFoundException(config);
	}

	private static byte[] getXmlConfig(String agentArguments) throws Exception {
		if (agentArguments.indexOf(',') == -1) {
			try (InputStream is = new FileInputStream(getFileForConfig(agentArguments))) {
				return IOToolkit.readFully(is, -1, true);
			}
		}

		String[] confs = agentArguments.split(","); //$NON-NLS-1$
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

		for (int i = 0; i < confs.length; ++i) {
			Document doc = factory.newDocumentBuilder().parse(getFileForConfig(confs[i]));

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
