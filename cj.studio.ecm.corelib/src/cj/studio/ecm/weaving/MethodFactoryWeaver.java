package cj.studio.ecm.weaving;


import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceTypeWeaver;
import cj.studio.ecm.container.factory.IServiceMethodInstanceFactory;
import cj.ultimate.org.objectweb.asm.ClassReader;
import cj.ultimate.org.objectweb.asm.ClassVisitor;
import cj.ultimate.org.objectweb.asm.ClassWriter;
import cj.ultimate.org.objectweb.asm.Opcodes;
import cj.ultimate.org.objectweb.asm.Type;
import cj.ultimate.org.objectweb.asm.tree.ClassNode;

//类型编织器工厂，它组织和管理一组编织器链，如：工厂方法编织器、适配器编织器，服务代理编织器等
public class MethodFactoryWeaver implements IServiceTypeWeaver {
	private IServiceMethodInstanceFactory methodFactory;

	public MethodFactoryWeaver() {
	}

	public MethodFactoryWeaver(IServiceMethodInstanceFactory methodFactory) {
		this();
		this.methodFactory = methodFactory;
	}

	@Override
	public byte[] weave(String className, byte[] b, IWeaverChain chain) {

		IServiceDefinition def = this.methodFactory
				.getServiceDefinition(className);
		if (def == null)
			return null;
//		if ((def.getServiceDescribeForm() & IServiceDefinition.SERVICE_DESCRIBEFORM) != IServiceDefinition.SERVICE_DESCRIBEFORM) {
//			return null;
//		}
		ClassWriter cw = new ClassWriter(ClassWriter./*COMPUTE_FRAMES*/COMPUTE_FRAMES);
		// TraceClassVisitor可查看和跟踪字节码
//		 PrintWriter pw = new PrintWriter(System.out);
//		 TraceClassVisitor tcv = new TraceClassVisitor(cw,new ASMifier(), pw);
		ClassVisitor ca = new MethodFactoryClassAdapter(cw, def);
		ClassReader cr = new ClassReader(b);
		ClassNode cn = new ClassNode();
		// 如何去写一个类
		// ASMifierClassVisitor acv = new ASMifierClassVisitor(pw);
		// cr.accept(acv, 0);ß
		cr.accept(cn, ClassReader.EXPAND_FRAMES);
		cr.accept(ca, ClassReader.EXPAND_FRAMES);
		String intenelName = className.replace(".", "/");
		String superName = cr.getSuperName();
		String[] inters = cr.getInterfaces();
		String[] newInters = new String[inters.length + 1];
		System.arraycopy(inters, 0, newInters, 0, inters.length);
		newInters[inters.length] = Type
				.getInternalName(ICanWeavingMethod.class);
		ca.visit(cn.version, Opcodes.ACC_PUBLIC, intenelName, null,
				superName, newInters);
		// 在这里增加方法和字段，通过ServiceTypeAdapter拦截和编织
		ca.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC,
				"cj$methodFactory",
				Type.getDescriptor(IServiceMethodInstanceFactory.class), null,
				null);
//		ca.visitMethod(
//				Opcodes.ACC_PUBLIC,
//				"setServiceMethodInstanceFactory",
//				"(L"
//						+ Type.getInternalName(IServiceMethodInstanceFactory.class)
//						+ ";)V", null, null);
		ca.visitEnd();
		if (chain.hasWeavingType(className))
			return chain.weave(className, cw.toByteArray());
		return cw.toByteArray();
	}

	@Override
	public boolean hasWeavingType(String sClassName) {
		// TODO Auto-generated method stub
		return this.methodFactory.containsServiceDefinition(sClassName);
	}
}
