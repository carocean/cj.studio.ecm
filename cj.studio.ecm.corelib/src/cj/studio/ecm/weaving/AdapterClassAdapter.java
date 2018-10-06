package cj.studio.ecm.weaving;

import cj.studio.ecm.container.describer.BridgeDescriber;
import cj.ultimate.org.objectweb.asm.ClassVisitor;
import cj.ultimate.org.objectweb.asm.Label;
import cj.ultimate.org.objectweb.asm.MethodVisitor;
import cj.ultimate.org.objectweb.asm.Opcodes;
import cj.ultimate.org.objectweb.asm.Type;
/*
 * 说明：
 * 
 * 桥的两种实现方案：
 * 1.使用enhance.create(当前类)生成代理
 * 2.使用enhance.create(Object.class)生成代理
 * 
 * 这两种方式差异较大，而且各有限制，左思右想采用了第2种方案，原因如下：
 * 
 * 使用当前类生成代理的问题：
 * 1.每调用一次适配器方法，将导致被代理的当前类的空构造方法被调用一次，而且总是要求开发者保留一个空构造方法，这侵入了开发者的代码逻辑，因此不好。
 * 2.结构上，被代理的当前类由于是开发者编写的，里面会有许多属性，如果代理当前类，则代理上会有许多空的属性，非常不好看。
 * 
 * 以上从Object.class上生成代理则可避免，但是有所限制：
 * 1.在一个类c中的属性p通过适配到服务b时，p不能声明为b的类型，p只能是b的某个接口或任意接口(b得有方法，不然报找不到方法错误）
 * 2.当然，限制也是合理的，这可以引导开发者必须关注于b的某个接口而使用它，而不是引用b的类类型。
 */
public class AdapterClassAdapter extends ClassVisitor implements Opcodes {
	private String className;
	private String defId;
	private String aspects;
	private String isBridge;
	int classversion = 52;// 默认是jdk1.6

	public AdapterClassAdapter(ClassVisitor cv, String className, String defId,
			BridgeDescriber bd) {
		super(ASM5, cv);
		this.className = className;
		this.defId = defId;
		this.aspects = bd == null ? "" : bd.getAspects();
		this.isBridge = bd == null ? "false" : Boolean.toString(bd.isValid());
	}
	/*
	 *   public <T> T getAdapter(Class<T> clazz) {

		String defId = "aaaaaa";
		String aspects = "bbbbb";
		String isBridge = "ccccc";
		IAdapterInterrupter ai = new AdapterInterrupter(this, defId, aspects, isBridge);
		Class<?>[] interfaces = null;
		if (clazz == null) {
			interfaces = new Class<?>[] { IObjectSetter.class };
		} else {
			interfaces = new Class<?>[] { clazz };
		}
		return (T) Enhancer.create(Object.class, interfaces, ai);
	 */
	//从Object.class生成代理
	private MethodVisitor visitGetAdapter(MethodVisitor mv) {
		mv.visitCode();
		mv.visitLdcInsn(this.defId);
		mv.visitVarInsn(ASTORE, 2);
		mv.visitLdcInsn(this.aspects);
		mv.visitVarInsn(ASTORE, 3);
		mv.visitLdcInsn(isBridge);
		mv.visitVarInsn(ASTORE, 4);
		mv.visitTypeInsn(NEW, "cj/studio/ecm/adapter/AdapterInterrupter");
		mv.visitInsn(DUP);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitVarInsn(ALOAD, 3);
		mv.visitVarInsn(ALOAD, 4);
		mv.visitMethodInsn(INVOKESPECIAL, "cj/studio/ecm/adapter/AdapterInterrupter", "<init>", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
		mv.visitVarInsn(ASTORE, 5);
		mv.visitInsn(ACONST_NULL);
		mv.visitVarInsn(ASTORE, 6);
		mv.visitVarInsn(ALOAD, 1);
		Label l0 = new Label();
		mv.visitJumpInsn(IFNONNULL, l0);
		mv.visitInsn(ICONST_1);
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_0);
		mv.visitLdcInsn(Type.getType("Lcj/studio/ecm/adapter/IObjectSetter;"));
		mv.visitInsn(AASTORE);
		mv.visitVarInsn(ASTORE, 6);
		Label l1 = new Label();
		mv.visitJumpInsn(GOTO, l1);
		mv.visitLabel(l0);
		mv.visitFrame(Opcodes.F_FULL, 7, new Object[] {className.replace(".", "/"), "java/lang/Class", "java/lang/String", "java/lang/String", "java/lang/String", "cj/studio/ecm/adapter/IAdapterInterrupter", "[Ljava/lang/Class;"}, 0, new Object[] {});
		mv.visitInsn(ICONST_1);
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitInsn(AASTORE);
		mv.visitVarInsn(ASTORE, 6);
		mv.visitLabel(l1);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		mv.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
		mv.visitVarInsn(ALOAD, 6);
		mv.visitVarInsn(ALOAD, 5);
		mv.visitMethodInsn(INVOKESTATIC, "cj/ultimate/net/sf/cglib/proxy/Enhancer", "create", "(Ljava/lang/Class;[Ljava/lang/Class;Lcj/ultimate/net/sf/cglib/proxy/Callback;)Ljava/lang/Object;", false);
		mv.visitInsn(ARETURN);
		mv.visitMaxs(6, 7);
		mv.visitEnd();
		return mv;
	}
// 指令：
//	java -cp .:asm-all-5.0.4.jar org.objectweb.asm.util.ASMifier class
//   用法说明：asm-all是asm的包所在路径，如果在终端目录下可省去目录路径，最后是类文件所在路径
// 原型 getAdapter的生成如下：
//	  public <T> T getAdapter(Class<T> clazz) {
//
//		String defId = "a";
//		String aspects = "b";
//		String isBridge = "c";
//		IAdapterInterrupter ai = new AdapterInterrupter(this, defId, aspects, isBridge);
//		Class<?>[] interfaces = null;
//		if (clazz == null) {
//			interfaces = new Class<?>[] { IObjectSetter.class };
//		} else {
//			interfaces = new Class<?>[] { clazz };
//		}
//		Class<?> the = this.getClass();
//		/*
//		 * 为什么求派生类型？因为可能导致找不到类，原因如下：
//		 * －cglib Enhancer.create(the, interfaces, ai);方法使用被代理类所在的类加载器
//		 * －cglib采用派生的形式生成代理类
//		 * －如果当前类已是代理类，如果使用这个代理类来生成对象，则实际是使用了Enhancer类所在的类加载器，即cglib包所在的加载上下文，从而导致被代理类所在的加载器内找不到该类
//		 *   除非它们在同一上下文中。
//		 */
//		if (Enhancer.isEnhanced(the)) {
//			the = the.getSuperclass();
//		}
//		return (T) Enhancer.create(the, interfaces, ai);
//	}
	 /*
	  * Enhancer可以指定加载器，通过以下方法调用
	  * cglib加载器选中策略：先找自定义，为空则找被代理的类类型，为空则找被代理的第一个接口，为空则找当前线程，为空则找父
	  * Enhancer e=new Enhancer();
	  * e.setSuperclass(the);
		e.setInterfaces(interfaces);
		e.setCallback(ai);
		e.setClassLoader(classLoader);
		
	  */
	//从当前类生成代理
//	private MethodVisitor visitGetAdapter2(MethodVisitor mv) {
//		mv.visitCode();
//		mv.visitLdcInsn(this.defId);
//		mv.visitVarInsn(ASTORE, 2);
//		mv.visitLdcInsn(this.aspects);
//		mv.visitVarInsn(ASTORE, 3);
//		mv.visitLdcInsn(this.isBridge);
//		mv.visitVarInsn(ASTORE, 4);
//		mv.visitTypeInsn(NEW, "cj/studio/ecm/adapter/AdapterInterrupter");
//		mv.visitInsn(DUP);
//		mv.visitVarInsn(ALOAD, 0);
//		mv.visitVarInsn(ALOAD, 2);
//		mv.visitVarInsn(ALOAD, 3);
//		mv.visitVarInsn(ALOAD, 4);
//		mv.visitMethodInsn(INVOKESPECIAL, "cj/studio/ecm/adapter/AdapterInterrupter", "<init>", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
//		mv.visitVarInsn(ASTORE, 5);
//		mv.visitInsn(ACONST_NULL);
//		mv.visitVarInsn(ASTORE, 6);
//		mv.visitVarInsn(ALOAD, 1);
//		Label l0 = new Label();
//		mv.visitJumpInsn(IFNONNULL, l0);
//		mv.visitInsn(ICONST_1);
//		mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
//		mv.visitInsn(DUP);
//		mv.visitInsn(ICONST_0);
//		mv.visitLdcInsn(Type.getType("Lcj/studio/ecm/adapter/IObjectSetter;"));
//		mv.visitInsn(AASTORE);
//		mv.visitVarInsn(ASTORE, 6);
//		Label l1 = new Label();
//		mv.visitJumpInsn(GOTO, l1);
//		mv.visitLabel(l0);
//		mv.visitFrame(Opcodes.F_FULL, 7, new Object[] {className.replace(".", "/"), "java/lang/Class", "java/lang/String", "java/lang/String", "java/lang/String", "cj/studio/ecm/adapter/IAdapterInterrupter", "[Ljava/lang/Class;"}, 0, new Object[] {});
//		mv.visitInsn(ICONST_1);
//		mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
//		mv.visitInsn(DUP);
//		mv.visitInsn(ICONST_0);
//		mv.visitVarInsn(ALOAD, 1);
//		mv.visitInsn(AASTORE);
//		mv.visitVarInsn(ASTORE, 6);
//		mv.visitLabel(l1);
//		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
//		mv.visitVarInsn(ALOAD, 0);
//		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
//		mv.visitVarInsn(ASTORE, 7);
//		mv.visitVarInsn(ALOAD, 7);
//		mv.visitMethodInsn(INVOKESTATIC, "cj/ultimate/net/sf/cglib/proxy/Enhancer", "isEnhanced", "(Ljava/lang/Class;)Z", false);
//		Label l2 = new Label();
//		mv.visitJumpInsn(IFEQ, l2);
//		mv.visitVarInsn(ALOAD, 7);
//		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getSuperclass", "()Ljava/lang/Class;", false);
//		mv.visitVarInsn(ASTORE, 7);
//		mv.visitLabel(l2);
//		mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {"java/lang/Class"}, 0, null);
//		mv.visitVarInsn(ALOAD, 7);
//		mv.visitVarInsn(ALOAD, 6);
//		mv.visitVarInsn(ALOAD, 5);
//		mv.visitMethodInsn(INVOKESTATIC, "cj/ultimate/net/sf/cglib/proxy/Enhancer", "create", "(Ljava/lang/Class;[Ljava/lang/Class;Lcj/ultimate/net/sf/cglib/proxy/Callback;)Ljava/lang/Object;", false);
//		mv.visitInsn(ARETURN);
//		mv.visitMaxs(6, 8);
//		mv.visitEnd();
//		return mv;
//	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		if ("getAdapter".equals(name)
				&& (desc.equals("(Ljava/lang/Class;)Ljava/lang/Object;"))) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature,
					exceptions);
			this.visitGetAdapter(mv);
			return mv;
		}
		return super.visitMethod(access, name, desc, signature, exceptions);
	}

}
