package cj.studio.ecm.weaving;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceDefinitionRegistry;
import cj.studio.ecm.IServiceTypeWeaver;
import cj.studio.ecm.adapter.IAdaptable;
import cj.studio.ecm.container.describer.BridgeDescriber;
import cj.studio.ecm.container.describer.TypeDescriber;
import cj.ultimate.collection.ICollection;
import cj.ultimate.org.objectweb.asm.ClassReader;
import cj.ultimate.org.objectweb.asm.ClassWriter;
import cj.ultimate.org.objectweb.asm.Opcodes;
import cj.ultimate.org.objectweb.asm.Type;
import cj.ultimate.org.objectweb.asm.tree.ClassNode;

//适配器织入器,每个服务都是适配器，故而返回true
public class AdapterWeaver implements IServiceTypeWeaver {
	private Map<String, String> serviceClass;
	private Map<String, BridgeDescriber> serviceBridge;

	public AdapterWeaver(IServiceDefinitionRegistry registry) {
		serviceClass = new HashMap<String, String>();
		serviceBridge = new HashMap<String, BridgeDescriber>();
		ICollection<String> col = registry.enumServiceDefinitionNames();
		for (String name : col) {
			IServiceDefinition def = registry.getServiceDefinition(name);
			String cn = def.getServiceDescriber().getClassName();
			if ((def.getServiceDescribeForm() & IServiceDefinition.SERVICE_DESCRIBEFORM) == IServiceDefinition.SERVICE_DESCRIBEFORM) {
				if (def.getServiceDescriber().isExoteric()) {// 声明为开放服务则被内核自动定义为适配器,因此在此设置。
					serviceClass.put(cn, def.getServiceDescriber()
							.getServiceId());
				}
				if ((def.getServiceDescribeForm() & IServiceDefinition.BRIDGE_DESCRIBEFORM) == IServiceDefinition.BRIDGE_DESCRIBEFORM) {
					List<TypeDescriber> list = def.getExtraDescribers();
					for (TypeDescriber td : list) {
						if (td instanceof BridgeDescriber) {
							serviceBridge.put(cn, (BridgeDescriber) td);
							if (!serviceClass.containsKey(cn)) {
								serviceClass.put(cn, def.getServiceDescriber()
										.getServiceId());// 声明为桥模式则一定是适配器，因此在此设置，桥中已声明了一个getAdapter空的方法，而在适配器织入器中实现了该方法
							}
							break;
						}
					}
				}
			}
		}
	}

	@Override
	public byte[] weave(String className, byte[] b, IWeaverChain chain) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		String defId = this.serviceClass.get(className);
		BridgeDescriber bd = this.serviceBridge.get(className);
		// TraceClassVisitor可查看和跟踪字节码
		// PrintWriter pw = new PrintWriter(System.out);
		// TraceClassVisitor tcv = new TraceClassVisitor(cw, pw);
		AdapterClassAdapter ca = new AdapterClassAdapter(cw, className, defId,
				bd);
		ClassReader cr = new ClassReader(b);
		ClassNode cn = new ClassNode();
		// 如何去写一个类
		// ASMifierClassVisitor acv = new ASMifierClassVisitor(pw);
		// cr.accept(acv, 0);
		cr.accept(cn, 0);
		cr.accept(ca, 0);
		String intenelName = className.replace(".", "/");
		String superName = cr.getSuperName();
		String[] inters = cr.getInterfaces();
		boolean hasAdaptable = false;
		for (String str : inters) {
			if (str.equals(Type.getInternalName(IAdaptable.class))) {
				hasAdaptable = true;
				break;
			}
		}
		// 问题：如果服务已定义了getAdapter方法，则会报重复定义方法错误，之后要修改
		// 比如使用analysis分析类得到该类的所有方法，只要存在getAdapter方法则不再为此生成
		ca.classversion = cn.version;// 由于1.6以上使用访问方法侦，为了生成兼容类，因此设此字段
		if (!hasAdaptable) {
			String[] newInters = new String[inters.length + 1];
			System.arraycopy(inters, 0, newInters, 0, inters.length);
			newInters[inters.length] = Type.getInternalName(IAdaptable.class);
			ca.visit(/* Opcodes.V1_6 */cn.version, Opcodes.ACC_PUBLIC+Opcodes.ACC_SUPER,
					intenelName, null, superName, newInters);
			ca.visitMethod(Opcodes.ACC_PUBLIC, "getAdapter",
					"(Ljava/lang/Class;)Ljava/lang/Object;", null, null);
		} else {
			ca.visit(/* Opcodes.V1_6 */cn.version, Opcodes.ACC_PUBLIC,
					intenelName, null, superName, inters);
		}
		ca.visitEnd();
		if (chain.hasWeavingType(className))
			return chain.weave(className, cw.toByteArray());
		return cw.toByteArray();
	}

	@Override
	public boolean hasWeavingType(String sClassName) {
		// 需排除接口
		return serviceClass.containsKey(sClassName);
	}

}
