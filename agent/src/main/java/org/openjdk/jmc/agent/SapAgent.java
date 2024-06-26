package org.openjdk.jmc.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.openjdk.jmc.agent.impl.DefaultTransformRegistry;
import org.openjdk.jmc.agent.jmx.AgentManagementFactory;
import org.openjdk.jmc.agent.util.ModuleUtils;

public class SapAgent {

	private static Logger logger = Logger.getLogger(SapAgent.class.getName());

	public static void premain(String agentArguments, Instrumentation instrumentation) throws Exception {
		System.out.println("Called Agent with " + agentArguments);
		agentmain(agentArguments, instrumentation);
	}

	public static void agentmain(String agentArguments, Instrumentation instrumentation) throws Exception {
		ModuleUtils.openUnsafePackage(instrumentation);

		if (agentArguments == null || agentArguments.trim().length() == 0) {
			initializeAgent(null, instrumentation);
		} else {
			File file = new File(agentArguments);

			try (InputStream stream = new FileInputStream(file)) {
				initializeAgent(stream, instrumentation);
			} catch (XMLStreamException | IOException | XMLValidationException e) {
				logger.log(Level.SEVERE, "Failed to read jfr probe definitions from " + file.getPath(), e); //$NON-NLS-1$
			}
		}
	}

	public static void initializeAgent(InputStream configuration, Instrumentation instrumentation)
			throws XMLStreamException, XMLValidationException {
		TransformRegistry registry = configuration != null ? DefaultTransformRegistry.from(configuration)
				: DefaultTransformRegistry.empty();
		instrumentation.addTransformer(new SapTransformer(registry), true);
		AgentManagementFactory.createAndRegisterAgentControllerMBean(instrumentation, registry);

		List<Class<?>> classesToRetransform = new ArrayList<>();
		Set<String> clazzes = registry.getClassNames().stream().map((name) -> name.replace('/', '.'))
				.collect(Collectors.toSet());

		for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
			if (clazzes.contains(clazz.getName())) {
				classesToRetransform.add(clazz);
				System.out.println("Class to retransform: " + clazz);
			}
		}

		try {
			instrumentation.retransformClasses(classesToRetransform.toArray(new Class<?>[0]));
		} catch (UnmodifiableClassException e) {
			logger.log(Level.SEVERE, "Unable to retransform classes", e);
		}
	}
}
