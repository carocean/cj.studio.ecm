package cj.studio.ecm.weaving;

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
 * 1.每调用一次桥方法，将导致被代理的当前类的空构造方法被调用一次，而且总是要求开发者保留一个空构造方法，这侵入了开发者的代码逻辑，因此不好。
 * 2.结构上，被代理的当前类由于是开发者编写的，里面会有许多属性，如果代理当前类，则代理上会有许多空的属性，非常不好看。
 * 
 * 以上从Object.class上生成代理则可避免，但是有所限制：
 * 1.在一个类c中的属性p通过桥接到服务b时，p不能声明为b的类型，p只能是b的某个接口或其某个方面中指定的某个接口
 * 2.当然，限制也是合理的，这可以引导开发者必须关注于b的某个方面(接口)而使用它，而不是引用b的类类型。
 */

public class BridgeClassAdapter extends ClassVisitor implements Opcodes {
	private String className;

	public BridgeClassAdapter(ClassVisitor cv, String className) {
		super(ASM5, cv);
		this.className = className;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		String getBridgeDesc = "(Ljava/lang/String;)Ljava/lang/Object;";
		if ("getBridge".equals(name) && (desc.equals(getBridgeDesc))) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			this.visitGetBridge(mv);
			return mv;
		}
		if ("getAdapter".equals(name) && desc.equals("(Ljava/lang/Class;)Ljava/lang/Object;")) {
			return super.visitMethod(access, name, desc, signature, exceptions);
		}
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		return mv;
	}
	/*
	 * 	private IJoinpoint cj$joinpoint;

	public <T> T getBridge(String aspects) {
		if (this.cj$joinpoint == null) {
			String msg = "连接点对象为空";
			CJSystem.current().environment().logging().error(msg);
			throw new EcmException(msg);
		}

		IJoinpoint bridgeJp = this.cj$joinpoint.builtNewJoinpoint(aspects);
		EnhanceInterceptor ei = new EnhanceInterceptor(bridgeJp);
		Class<?>[] cutInterfaces = bridgeJp.getCutInterfaces();
		List<Class<?>> allFaces = new ArrayList<>();
		ObjectHelper.fetchAllInterface(this.getClass(), allFaces);
		if (cutInterfaces != null) {
			for (Class<?> c : cutInterfaces) {
				if (allFaces.contains(c))
					continue;
				allFaces.add(c);
			}
		}
		allFaces.remove(IBridge.class);
		allFaces.remove(IAdaptable.class);
		Class<?>[] allfaceArr = allFaces.toArray(new Class<?>[0]);
		Object bridge = null;
		// 如果无特别指定方面，或者方面接口为空，则以当前类型为原型创建桥
		if (allfaceArr.length == 0) {
			bridge = Enhancer.create(Object.class, new Class<?>[] { IBridge.class, IAdaptable.class }, ei);
		} else {

			Class<?>[] newInters = new Class<?>[allfaceArr.length + 2];
			System.arraycopy(allfaceArr, 0, newInters, 0, allfaceArr.length);
			newInters[allfaceArr.length] = IBridge.class;
			newInters[allfaceArr.length + 1] = IAdaptable.class;
			// 过期：引导开发者关注于方面编程，引用的属性类型必须是桥的方面的可转换接口
			// 注释掉参数Object.class原因：不能让方面决定服务的本身类型，比如a服务引用了b服务，如果b服务声明的方面含有接口集合，则a不能直接引用b的原始服务类型了，那岂不是会让原程序报错，因此注释了
			// bridge = Enhancer.create(Object.class, newInters, ei);
			Enhancer e = new Enhancer();
			e.setSuperclass(Object.class);
			e.setInterfaces(newInters);
			e.setCallback(ei);
			e.setClassLoader(this.getClass().getClassLoader());
			bridge = e.create();
		}
		return (T) bridge;
	}
	 */
	//从object生成代理
	private void visitGetBridge(MethodVisitor mv) {
		String fullClassname = className.replace(".", "/");
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, fullClassname, "cj$joinpoint", "Lcj/studio/ecm/bridge/IJoinpoint;");
		Label l0 = new Label();
		mv.visitJumpInsn(IFNONNULL, l0);
		mv.visitLdcInsn("\u8fde\u63a5\u70b9\u5bf9\u8c61\u4e3a\u7a7a");
		mv.visitVarInsn(ASTORE, 2);
		mv.visitMethodInsn(INVOKESTATIC, "cj/studio/ecm/CJSystem", "current", "()Lcj/studio/ecm/CJSystem;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "cj/studio/ecm/CJSystem", "environment", "()Lcj/studio/ecm/Environment;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "cj/studio/ecm/Environment", "logging", "()Lcj/studio/ecm/logging/ILogging;", false);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKEINTERFACE, "cj/studio/ecm/logging/ILogging", "error", "(Ljava/lang/Object;)V", true);
		mv.visitTypeInsn(NEW, "cj/studio/ecm/EcmException");
		mv.visitInsn(DUP);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKESPECIAL, "cj/studio/ecm/EcmException", "<init>", "(Ljava/lang/String;)V", false);
		mv.visitInsn(ATHROW);
		mv.visitLabel(l0);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, fullClassname, "cj$joinpoint", "Lcj/studio/ecm/bridge/IJoinpoint;");
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKEINTERFACE, "cj/studio/ecm/bridge/IJoinpoint", "builtNewJoinpoint", "(Ljava/lang/String;)Lcj/studio/ecm/bridge/IJoinpoint;", true);
		mv.visitVarInsn(ASTORE, 2);
		mv.visitTypeInsn(NEW, "cj/studio/ecm/bridge/EnhanceInterceptor");
		mv.visitInsn(DUP);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKESPECIAL, "cj/studio/ecm/bridge/EnhanceInterceptor", "<init>", "(Lcj/studio/ecm/bridge/IJoinpoint;)V", false);
		mv.visitVarInsn(ASTORE, 3);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKEINTERFACE, "cj/studio/ecm/bridge/IJoinpoint", "getCutInterfaces", "()[Ljava/lang/Class;", true);
		mv.visitVarInsn(ASTORE, 4);
		mv.visitTypeInsn(NEW, "java/util/ArrayList");
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
		mv.visitVarInsn(ASTORE, 5);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
		mv.visitVarInsn(ALOAD, 5);
		mv.visitMethodInsn(INVOKESTATIC, "cj/studio/ecm/util/ObjectHelper", "fetchAllInterface", "(Ljava/lang/Class;Ljava/util/List;)V", false);
		mv.visitVarInsn(ALOAD, 4);
		Label l1 = new Label();
		mv.visitJumpInsn(IFNULL, l1);
		mv.visitVarInsn(ALOAD, 4);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ASTORE, 9);
		mv.visitInsn(ARRAYLENGTH);
		mv.visitVarInsn(ISTORE, 8);
		mv.visitInsn(ICONST_0);
		mv.visitVarInsn(ISTORE, 7);
		Label l2 = new Label();
		mv.visitJumpInsn(GOTO, l2);
		Label l3 = new Label();
		mv.visitLabel(l3);
		mv.visitFrame(Opcodes.F_FULL, 10, new Object[] {fullClassname, "java/lang/String", "cj/studio/ecm/bridge/IJoinpoint", "cj/studio/ecm/bridge/EnhanceInterceptor", "[Ljava/lang/Class;", "java/util/List", Opcodes.TOP, Opcodes.INTEGER, Opcodes.INTEGER, "[Ljava/lang/Class;"}, 0, new Object[] {});
		mv.visitVarInsn(ALOAD, 9);
		mv.visitVarInsn(ILOAD, 7);
		mv.visitInsn(AALOAD);
		mv.visitVarInsn(ASTORE, 6);
		mv.visitVarInsn(ALOAD, 5);
		mv.visitVarInsn(ALOAD, 6);
		mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "contains", "(Ljava/lang/Object;)Z", true);
		Label l4 = new Label();
		mv.visitJumpInsn(IFEQ, l4);
		Label l5 = new Label();
		mv.visitJumpInsn(GOTO, l5);
		mv.visitLabel(l4);
		mv.visitFrame(Opcodes.F_FULL, 10, new Object[] {fullClassname, "java/lang/String", "cj/studio/ecm/bridge/IJoinpoint", "cj/studio/ecm/bridge/EnhanceInterceptor", "[Ljava/lang/Class;", "java/util/List", "java/lang/Class", Opcodes.INTEGER, Opcodes.INTEGER, "[Ljava/lang/Class;"}, 0, new Object[] {});
		mv.visitVarInsn(ALOAD, 5);
		mv.visitVarInsn(ALOAD, 6);
		mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
		mv.visitInsn(POP);
		mv.visitLabel(l5);
		mv.visitFrame(Opcodes.F_FULL, 10, new Object[] {fullClassname, "java/lang/String", "cj/studio/ecm/bridge/IJoinpoint", "cj/studio/ecm/bridge/EnhanceInterceptor", "[Ljava/lang/Class;", "java/util/List", Opcodes.TOP, Opcodes.INTEGER, Opcodes.INTEGER, "[Ljava/lang/Class;"}, 0, new Object[] {});
		mv.visitIincInsn(7, 1);
		mv.visitLabel(l2);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		mv.visitVarInsn(ILOAD, 7);
		mv.visitVarInsn(ILOAD, 8);
		mv.visitJumpInsn(IF_ICMPLT, l3);
		mv.visitLabel(l1);
		mv.visitFrame(Opcodes.F_FULL, 6, new Object[] {fullClassname, "java/lang/String", "cj/studio/ecm/bridge/IJoinpoint", "cj/studio/ecm/bridge/EnhanceInterceptor", "[Ljava/lang/Class;", "java/util/List"}, 0, new Object[] {});
		mv.visitVarInsn(ALOAD, 5);
		mv.visitLdcInsn(Type.getType("Lcj/studio/ecm/bridge/IBridge;"));
		mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "remove", "(Ljava/lang/Object;)Z", true);
		mv.visitInsn(POP);
		mv.visitVarInsn(ALOAD, 5);
		mv.visitLdcInsn(Type.getType("Lcj/studio/ecm/adapter/IAdaptable;"));
		mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "remove", "(Ljava/lang/Object;)Z", true);
		mv.visitInsn(POP);
		mv.visitVarInsn(ALOAD, 5);
		mv.visitInsn(ICONST_0);
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
		mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;", true);
		mv.visitTypeInsn(CHECKCAST, "[Ljava/lang/Class;");
		mv.visitVarInsn(ASTORE, 6);
		mv.visitInsn(ACONST_NULL);
		mv.visitVarInsn(ASTORE, 7);
		mv.visitVarInsn(ALOAD, 6);
		mv.visitInsn(ARRAYLENGTH);
		Label l6 = new Label();
		mv.visitJumpInsn(IFNE, l6);
		mv.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
		mv.visitInsn(ICONST_2);
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_0);
		mv.visitLdcInsn(Type.getType("Lcj/studio/ecm/bridge/IBridge;"));
		mv.visitInsn(AASTORE);
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_1);
		mv.visitLdcInsn(Type.getType("Lcj/studio/ecm/adapter/IAdaptable;"));
		mv.visitInsn(AASTORE);
		mv.visitVarInsn(ALOAD, 3);
		mv.visitMethodInsn(INVOKESTATIC, "cj/ultimate/net/sf/cglib/proxy/Enhancer", "create", "(Ljava/lang/Class;[Ljava/lang/Class;Lcj/ultimate/net/sf/cglib/proxy/Callback;)Ljava/lang/Object;", false);
		mv.visitVarInsn(ASTORE, 7);
		Label l7 = new Label();
		mv.visitJumpInsn(GOTO, l7);
		mv.visitLabel(l6);
		mv.visitFrame(Opcodes.F_APPEND,2, new Object[] {"[Ljava/lang/Class;", "java/lang/Object"}, 0, null);
		mv.visitVarInsn(ALOAD, 6);
		mv.visitInsn(ARRAYLENGTH);
		mv.visitInsn(ICONST_2);
		mv.visitInsn(IADD);
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
		mv.visitVarInsn(ASTORE, 8);
		mv.visitVarInsn(ALOAD, 6);
		mv.visitInsn(ICONST_0);
		mv.visitVarInsn(ALOAD, 8);
		mv.visitInsn(ICONST_0);
		mv.visitVarInsn(ALOAD, 6);
		mv.visitInsn(ARRAYLENGTH);
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", false);
		mv.visitVarInsn(ALOAD, 8);
		mv.visitVarInsn(ALOAD, 6);
		mv.visitInsn(ARRAYLENGTH);
		mv.visitLdcInsn(Type.getType("Lcj/studio/ecm/bridge/IBridge;"));
		mv.visitInsn(AASTORE);
		mv.visitVarInsn(ALOAD, 8);
		mv.visitVarInsn(ALOAD, 6);
		mv.visitInsn(ARRAYLENGTH);
		mv.visitInsn(ICONST_1);
		mv.visitInsn(IADD);
		mv.visitLdcInsn(Type.getType("Lcj/studio/ecm/adapter/IAdaptable;"));
		mv.visitInsn(AASTORE);
		mv.visitTypeInsn(NEW, "cj/ultimate/net/sf/cglib/proxy/Enhancer");
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, "cj/ultimate/net/sf/cglib/proxy/Enhancer", "<init>", "()V", false);
		mv.visitVarInsn(ASTORE, 9);
		mv.visitVarInsn(ALOAD, 9);
		mv.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
		mv.visitMethodInsn(INVOKEVIRTUAL, "cj/ultimate/net/sf/cglib/proxy/Enhancer", "setSuperclass", "(Ljava/lang/Class;)V", false);
		mv.visitVarInsn(ALOAD, 9);
		mv.visitVarInsn(ALOAD, 8);
		mv.visitMethodInsn(INVOKEVIRTUAL, "cj/ultimate/net/sf/cglib/proxy/Enhancer", "setInterfaces", "([Ljava/lang/Class;)V", false);
		mv.visitVarInsn(ALOAD, 9);
		mv.visitVarInsn(ALOAD, 3);
		mv.visitMethodInsn(INVOKEVIRTUAL, "cj/ultimate/net/sf/cglib/proxy/Enhancer", "setCallback", "(Lcj/ultimate/net/sf/cglib/proxy/Callback;)V", false);
		mv.visitVarInsn(ALOAD, 9);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "cj/ultimate/net/sf/cglib/proxy/Enhancer", "setClassLoader", "(Ljava/lang/ClassLoader;)V", false);
		mv.visitVarInsn(ALOAD, 9);
		mv.visitMethodInsn(INVOKEVIRTUAL, "cj/ultimate/net/sf/cglib/proxy/Enhancer", "create", "()Ljava/lang/Object;", false);
		mv.visitVarInsn(ASTORE, 7);
		mv.visitLabel(l7);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		mv.visitVarInsn(ALOAD, 7);
		mv.visitInsn(ARETURN);
		mv.visitMaxs(5, 10);
		mv.visitEnd();
	}
	/*
	 * 代码生成：
	 * java -cp .:asm-all-5.0.4.jar org.objectweb.asm.util.ASMifier ../studio/x230/ecm/cj.studio.ecm.test.chip2/bin/cj/studio/ecm/test/chip2/MyXmlService.class 
	* 用法说明：asm-all是asm的包所在路径，如果在终端目录下可省去目录路径，最后是类文件所在路径
	* 指令代码段原型：

		private IJoinpoint cj$joinpoint;
		public <T> T getBridge(String aspects) {
			if (this.cj$joinpoint == null) {
				String msg = "连接点对象为空";
				CJSystem.current().environment().logging().error(msg);
				throw new RuntimeException(msg);
			}

			IJoinpoint bridgeJp = this.cj$joinpoint.builtNewJoinpoint(aspects);
			EnhanceInterceptor ei = new EnhanceInterceptor(bridgeJp);
			Class<?>[] cutInterfaces = bridgeJp.getCutInterfaces();
			Object bridge = null;
			// 如果无特别指定方面，或者方面接口为空，则以当前类型为原型创建桥
			if (cutInterfaces == null || cutInterfaces.length == 0) {
				bridge = Enhancer.create(this.getClass(), new Class<?>[] { IBridge.class, IAdaptable.class }, ei);
			} else {
				
				Class<?>[] newInters = new Class<?>[cutInterfaces.length + 2];
				System.arraycopy(cutInterfaces, 0, newInters, 0, cutInterfaces.length);
				newInters[cutInterfaces.length] = IBridge.class;
				newInters[cutInterfaces.length + 1] = IAdaptable.class;
				// 过期：引导开发者关注于方面编程，引用的属性类型必须是桥的方面的可转换接口
				// 注释掉参数Object.class原因：不能让方面决定服务的本身类型，比如a服务引用了b服务，如果b服务声明的方面含有接口集合，则a不能直接引用b的原始服务类型了，那岂不是会让原程序报错，因此注释了
				bridge = Enhancer.create(this.getClass(), newInters, ei);
			}
			return (T) bridge;
		}
		*
		*
		*
		*
		**/
	//从当前类生成代理
//	private void visitGetBridge2(MethodVisitor mv) {
//		String fullClassname = className.replace(".", "/");
//		mv.visitCode();
//		mv.visitCode();
//		mv.visitVarInsn(ALOAD, 0);
//		mv.visitFieldInsn(GETFIELD, fullClassname, "cj$joinpoint", "Lcj/studio/ecm/bridge/IJoinpoint;");
//		Label l0 = new Label();
//		mv.visitJumpInsn(IFNONNULL, l0);
//		mv.visitLdcInsn("\u8fde\u63a5\u70b9\u5bf9\u8c61\u4e3a\u7a7a");
//		mv.visitVarInsn(ASTORE, 2);
//		mv.visitMethodInsn(INVOKESTATIC, "cj/studio/ecm/CJSystem", "current", "()Lcj/studio/ecm/CJSystem;", false);
//		mv.visitMethodInsn(INVOKEVIRTUAL, "cj/studio/ecm/CJSystem", "environment", "()Lcj/studio/ecm/Environment;", false);
//		mv.visitMethodInsn(INVOKEVIRTUAL, "cj/studio/ecm/Environment", "logging", "()Lcj/studio/ecm/logging/ILogging;", false);
//		mv.visitVarInsn(ALOAD, 2);
//		mv.visitMethodInsn(INVOKEINTERFACE, "cj/studio/ecm/logging/ILogging", "error", "(Ljava/lang/Object;)V", true);
//		mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
//		mv.visitInsn(DUP);
//		mv.visitVarInsn(ALOAD, 2);
//		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false);
//		mv.visitInsn(ATHROW);
//		mv.visitLabel(l0);
//		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
//		mv.visitVarInsn(ALOAD, 0);
//		mv.visitFieldInsn(GETFIELD, fullClassname, "cj$joinpoint", "Lcj/studio/ecm/bridge/IJoinpoint;");
//		mv.visitVarInsn(ALOAD, 1);
//		mv.visitMethodInsn(INVOKEINTERFACE, "cj/studio/ecm/bridge/IJoinpoint", "builtNewJoinpoint", "(Ljava/lang/String;)Lcj/studio/ecm/bridge/IJoinpoint;", true);
//		mv.visitVarInsn(ASTORE, 2);
//		mv.visitTypeInsn(NEW, "cj/studio/ecm/bridge/EnhanceInterceptor");
//		mv.visitInsn(DUP);
//		mv.visitVarInsn(ALOAD, 2);
//		mv.visitMethodInsn(INVOKESPECIAL, "cj/studio/ecm/bridge/EnhanceInterceptor", "<init>", "(Lcj/studio/ecm/bridge/IJoinpoint;)V", false);
//		mv.visitVarInsn(ASTORE, 3);
//		mv.visitVarInsn(ALOAD, 2);
//		mv.visitMethodInsn(INVOKEINTERFACE, "cj/studio/ecm/bridge/IJoinpoint", "getCutInterfaces", "()[Ljava/lang/Class;", true);
//		mv.visitVarInsn(ASTORE, 4);
//		mv.visitInsn(ACONST_NULL);
//		mv.visitVarInsn(ASTORE, 5);
//		mv.visitVarInsn(ALOAD, 4);
//		Label l1 = new Label();
//		mv.visitJumpInsn(IFNULL, l1);
//		mv.visitVarInsn(ALOAD, 4);
//		mv.visitInsn(ARRAYLENGTH);
//		Label l2 = new Label();
//		mv.visitJumpInsn(IFNE, l2);
//		mv.visitLabel(l1);
//		mv.visitFrame(Opcodes.F_FULL, 6, new Object[] {fullClassname, "java/lang/String", "cj/studio/ecm/bridge/IJoinpoint", "cj/studio/ecm/bridge/EnhanceInterceptor", "[Ljava/lang/Class;", "java/lang/Object"}, 0, new Object[] {});
//		mv.visitVarInsn(ALOAD, 0);
//		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
//		mv.visitInsn(ICONST_2);
//		mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
//		mv.visitInsn(DUP);
//		mv.visitInsn(ICONST_0);
//		mv.visitLdcInsn(Type.getType("Lcj/studio/ecm/bridge/IBridge;"));
//		mv.visitInsn(AASTORE);
//		mv.visitInsn(DUP);
//		mv.visitInsn(ICONST_1);
//		mv.visitLdcInsn(Type.getType("Lcj/studio/ecm/adapter/IAdaptable;"));
//		mv.visitInsn(AASTORE);
//		mv.visitVarInsn(ALOAD, 3);
//		mv.visitMethodInsn(INVOKESTATIC, "cj/ultimate/net/sf/cglib/proxy/Enhancer", "create", "(Ljava/lang/Class;[Ljava/lang/Class;Lcj/ultimate/net/sf/cglib/proxy/Callback;)Ljava/lang/Object;", false);
//		mv.visitVarInsn(ASTORE, 5);
//		Label l3 = new Label();
//		mv.visitJumpInsn(GOTO, l3);
//		mv.visitLabel(l2);
//		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
//		mv.visitVarInsn(ALOAD, 4);
//		mv.visitInsn(ARRAYLENGTH);
//		mv.visitInsn(ICONST_2);
//		mv.visitInsn(IADD);
//		mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
//		mv.visitVarInsn(ASTORE, 6);
//		mv.visitVarInsn(ALOAD, 4);
//		mv.visitInsn(ICONST_0);
//		mv.visitVarInsn(ALOAD, 6);
//		mv.visitInsn(ICONST_0);
//		mv.visitVarInsn(ALOAD, 4);
//		mv.visitInsn(ARRAYLENGTH);
//		mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", false);
//		mv.visitVarInsn(ALOAD, 6);
//		mv.visitVarInsn(ALOAD, 4);
//		mv.visitInsn(ARRAYLENGTH);
//		mv.visitLdcInsn(Type.getType("Lcj/studio/ecm/bridge/IBridge;"));
//		mv.visitInsn(AASTORE);
//		mv.visitVarInsn(ALOAD, 6);
//		mv.visitVarInsn(ALOAD, 4);
//		mv.visitInsn(ARRAYLENGTH);
//		mv.visitInsn(ICONST_1);
//		mv.visitInsn(IADD);
//		mv.visitLdcInsn(Type.getType("Lcj/studio/ecm/adapter/IAdaptable;"));
//		mv.visitInsn(AASTORE);
//		mv.visitVarInsn(ALOAD, 0);
//		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
//		mv.visitVarInsn(ALOAD, 6);
//		mv.visitVarInsn(ALOAD, 3);
//		mv.visitMethodInsn(INVOKESTATIC, "cj/ultimate/net/sf/cglib/proxy/Enhancer", "create", "(Ljava/lang/Class;[Ljava/lang/Class;Lcj/ultimate/net/sf/cglib/proxy/Callback;)Ljava/lang/Object;", false);
//		mv.visitVarInsn(ASTORE, 5);
//		mv.visitLabel(l3);
//		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
//		mv.visitVarInsn(ALOAD, 5);
//		mv.visitInsn(ARETURN);
//		mv.visitMaxs(5, 7);
//		mv.visitEnd();
//
//	}

}
