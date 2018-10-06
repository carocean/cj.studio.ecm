package cj.studio.ecm.weaving;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceDefinitionRegistry;
import cj.studio.ecm.IServiceTypeWeaver;
import cj.studio.ecm.bridge.IBridgeable;
import cj.studio.ecm.bridge.IJoinpoint;
import cj.studio.ecm.container.describer.BridgeDescriber;
import cj.studio.ecm.container.describer.TypeDescriber;
import cj.ultimate.org.objectweb.asm.ClassReader;
import cj.ultimate.org.objectweb.asm.ClassVisitor;
import cj.ultimate.org.objectweb.asm.ClassWriter;
import cj.ultimate.org.objectweb.asm.Opcodes;
import cj.ultimate.org.objectweb.asm.Type;
import cj.ultimate.org.objectweb.asm.tree.ClassNode;
import cj.ultimate.org.objectweb.asm.tree.MethodNode;

//桥织入器
public class BridgeWeaver implements IServiceTypeWeaver {

	private Map<String, BridgeDescriber> serviceBridge;

	public BridgeWeaver(IServiceDefinitionRegistry registry) {
		serviceBridge = new HashMap<String, BridgeDescriber>();
		for (String name : registry.enumServiceDefinitionNames()) {
			IServiceDefinition def = registry.getServiceDefinition(name);
			if ((def.getServiceDescribeForm() & IServiceDefinition.SERVICE_DESCRIBEFORM) == IServiceDefinition.SERVICE_DESCRIBEFORM) {
				if ((def.getServiceDescribeForm() & IServiceDefinition.BRIDGE_DESCRIBEFORM) == IServiceDefinition.BRIDGE_DESCRIBEFORM) {
					List<TypeDescriber> list = def.getExtraDescribers();
					for (TypeDescriber td : list) {
						if (td instanceof BridgeDescriber) {
							BridgeDescriber bd = (BridgeDescriber) td;
							if (bd.isValid())
								serviceBridge.put(def.getServiceDescriber()
										.getClassName(), (BridgeDescriber) td);
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
//		BridgeDescriber bd = this.serviceBridge.get(className);
		ClassVisitor ca = new BridgeClassAdapter(cw,className);
		ClassReader cr = new ClassReader(b);
		ClassNode cn = new ClassNode();
//		PrintWriter pw = new PrintWriter(System.out);
//		 ASMifier acv = new ASMifier();
//		 acv.print(pw);
//		cr.accept(acv, 0);
		cr.accept(cn, 0);
		cr.accept(ca, 0);
		String intenelName = className.replace(".", "/");
		String superName = cr.getSuperName();
		String[] inters = cr.getInterfaces();
		boolean hasBridge = false;
		for (String str : inters) {
			if (str.equals(Type.getInternalName(IBridgeable.class))) {
				hasBridge = true;
				break;
			}
		}
		if (!hasBridge) {
			String[] newInters = new String[inters.length + 1];
			System.arraycopy(inters, 0, newInters, 0, inters.length);
			newInters[inters.length] = Type.getInternalName(IBridgeable.class);
//			newInters[inters.length+1] = Type.getInternalName(IAdaptable.class);
			//如何动态的获取当前java或class的类版本号，现在1.6下如改为1.5在代码中如有if等语句时会报：数据超出边界错误
			ca.visit(cn.version, Opcodes.ACC_PUBLIC, intenelName, null,
					superName, newInters);
			boolean hasBridgeMethod = false;
			String getBridgeDesc="(Ljava/lang/String;)Ljava/lang/Object;";
			for (Object e : cn.methods) {
				MethodNode node = (MethodNode) e;
				if (node.name.equals("getBridge") && node.desc.equals(getBridgeDesc)) {
					hasBridgeMethod = true;
					break;
				}
			}
			if (!hasBridgeMethod)
				ca.visitMethod(Opcodes.ACC_PUBLIC, "getBridge",
						getBridgeDesc, null, null);
		} else {
			ca.visit(cn.version, Opcodes.ACC_PUBLIC, intenelName, null,
					superName, inters);
		}

		ca.visitField(Opcodes.ACC_PRIVATE, "cj$joinpoint",
				Type.getDescriptor(IJoinpoint.class), null, null);
		ca.visitEnd();
		if (chain.hasWeavingType(className))
			return chain.weave(className, cw.toByteArray());
		return cw.toByteArray();
	}

	@Override
	public boolean hasWeavingType(String sClassName) {
		return this.serviceBridge.containsKey(sClassName);
	}

}
