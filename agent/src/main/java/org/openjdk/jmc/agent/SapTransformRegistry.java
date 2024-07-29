package org.openjdk.jmc.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.openjdk.jmc.agent.jfr.JFRTransformDescriptor;

public class SapTransformRegistry implements TransformRegistry {

	private TransformRegistry registry;
	private final HashMap<String, List<TransformDescriptor>> modifiedTransforms = new HashMap<>();

	private void modifyTransformations() {
		modifiedTransforms.clear();

		for (String className : getClassNames()) {
			String simpleName = className.substring(className.lastIndexOf('/') + 1);
			List<TransformDescriptor> descs = getTransformData(className);
			List<TransformDescriptor> modifiedDescs = null;

			for (int i = 0; i < descs.size(); ++i) {
				JFRTransformDescriptor desc = (JFRTransformDescriptor) descs.get(i);
				JFRTransformDescriptor modified = null;

				if (simpleName.equals(desc.getMethod().getName())) {
					Method modifiedMethod = new Method("<init>", desc.getMethod().getSignature());
					modified = new JFRTransformDescriptor(desc.getId(), desc.getClassName(), modifiedMethod,
							desc.getTransformationAttributes(), desc.getParameters(), desc.getReturnValue(),
							desc.getFields());
				}

				if (modified != null) {
					if (modifiedDescs == null) {
						modifiedDescs = new ArrayList<TransformDescriptor>(descs);
					}

					modifiedDescs.set(i, modified);
				}
			}

			if (modifiedDescs != null) {
				modifiedTransforms.put(className, modifiedDescs);
			}
		}
	}

	public SapTransformRegistry(TransformRegistry registry) {
		this.registry = registry;
		modifyTransformations();
	}

	@Override
	public boolean hasPendingTransforms(String className) {
		return registry.hasPendingTransforms(className);
	}

	@Override
	public List<TransformDescriptor> getTransformData(String className) {
		List<TransformDescriptor> modified = modifiedTransforms.get(className);

		if (modified != null) {
			return modified;
		}

		return registry.getTransformData(className);
	}

	@Override
	public Set<String> getClassNames() {
		return registry.getClassNames();
	}

	@Override
	public String getCurrentConfiguration() {
		return registry.getCurrentConfiguration();
	}

	@Override
	public void setCurrentConfiguration(String xmlDescription) {
		registry.setCurrentConfiguration(xmlDescription);
		modifyTransformations();
	}

	@Override
	public Set<String> modify(String xmlDescription) throws XMLValidationException {
		Set<String> result = registry.modify(xmlDescription);
		modifyTransformations();

		return result;
	}

	@Override
	public Set<String> clearAllTransformData() {
		Set<String> result = registry.clearAllTransformData();
		modifyTransformations();

		return result;
	}

	@Override
	public void setRevertInstrumentation(boolean shouldRevert) {
		registry.setRevertInstrumentation(shouldRevert);
	}

	@Override
	public boolean isRevertIntrumentation() {
		return registry.isRevertIntrumentation();
	}
}
