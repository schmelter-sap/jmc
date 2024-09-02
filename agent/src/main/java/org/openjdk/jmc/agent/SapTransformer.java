package org.openjdk.jmc.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openjdk.jmc.agent.util.TypeUtils;

public class SapTransformer implements ClassFileTransformer {

	private final Transformer impl;
	private final Module jfrModule;
	private TransformRegistry registry;

	public SapTransformer(TransformRegistry registry) {
		this.registry = registry;
		jfrModule = ModuleLayer.boot().findModule("jdk.jfr").get();
		impl = new Transformer(new SapTransformRegistry(registry));
	}

	private void grantJfrAccessToModule(
		Module module, ClassLoader loader, String className, ProtectionDomain protectionDomain)
			throws IllegalClassFormatException {
		// We need to access the jfr module.
		if (!module.canRead(jfrModule)) {
			// Create a class in the module which grants the access. 
			ClassWriter cw = new ClassWriter(0);
			String name = className.replace('/', '.') + "_$MakeJFRModuleReadable";
			cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, name.replace('.', '/'), null,
					"java/lang/Object", null);

			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getModule", "()Ljava/lang/Module;", false);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/ModuleLayer", "boot", "()Ljava/lang/ModuleLayer;",
					false);
			mv.visitLdcInsn("jdk.jfr");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/ModuleLayer", "findModule",
					"(Ljava/lang/String;)Ljava/util/Optional;", false);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "get", "()Ljava/lang/Object;", false);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Module", "addReads",
					"(Ljava/lang/Module;)Ljava/lang/Module;", false);
			mv.visitInsn(Opcodes.RETURN);
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLocalVariable("this", TypeUtils.parameterize(name.replace('.', '/')), null, l0, l1, 0);
			mv.visitMaxs(1, 1);
			mv.visitEnd();

			cw.visitEnd();
			byte[] bytes = cw.toByteArray();

			try {
				Class<?> cls = TypeUtils.defineClass(name, bytes, 0, bytes.length, loader, protectionDomain);
				// Running the constructor make runs the code grants jfr module access.
				cls.getDeclaredConstructor().newInstance();
			} catch (IllegalAccessException | InstantiationException e) {
				e.printStackTrace();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	@Override
	public byte[] transform(
		ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
		byte[] classfileBuffer) throws IllegalClassFormatException {
		if (registry.getTransformData(className).isEmpty()) {
			return null;
		}

		if (classBeingRedefined != null) {
			grantJfrAccessToModule(classBeingRedefined.getModule(), loader, className, protectionDomain);
		}

		System.out.println("Transform for " + className + (classBeingRedefined != null ? ".class" : ""));
		return impl.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
	}

	@Override
	public byte[] transform(
		Module module, ClassLoader loader, String className, Class<?> classBeingRedefined,
		ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		if (registry.getTransformData(className).isEmpty()) {
			return null;
		}

		grantJfrAccessToModule(module, loader, className, protectionDomain);

		System.out.println(
				"Transform for " + className + (classBeingRedefined != null ? ".class" : "") + " [" + module + "]");
		return impl.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
	}
}
