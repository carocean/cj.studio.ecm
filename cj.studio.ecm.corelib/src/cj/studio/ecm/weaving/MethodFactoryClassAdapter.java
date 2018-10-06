package cj.studio.ecm.weaving;

import java.util.List;

import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.container.describer.ServiceMethod;
import cj.studio.ecm.container.factory.IServiceMethodInitializer;
import cj.studio.ecm.container.factory.IServiceMethodInstanceFactory;
import cj.ultimate.org.objectweb.asm.ClassVisitor;
import cj.ultimate.org.objectweb.asm.MethodVisitor;
import cj.ultimate.org.objectweb.asm.Opcodes;
import cj.ultimate.org.objectweb.asm.Type;
import cj.ultimate.org.objectweb.asm.commons.AdviceAdapter;

public class MethodFactoryClassAdapter extends ClassVisitor {
	private List<ServiceMethod> methodList;
	private String className;
	private String serviceDefId;

	public MethodFactoryClassAdapter(ClassVisitor cv, String className) {
		super(Opcodes.ASM5,cv);
		this.className = className;
	}

	public MethodFactoryClassAdapter(ClassVisitor cv, IServiceDefinition def) {
		this(cv, def.getServiceDescriber().getClassName());
		methodList = def.getMethods();
		this.serviceDefId = def.getServiceDescriber().getServiceId();
	}

	private ServiceMethod methodMatched(String name, String desc) {
		// 由于构造函数没有返回值，所以不参与编织
		if ("<init>".equals(name))
			return null;
		for (ServiceMethod sm : methodList) {
			if (!sm.matched(name, desc))
				continue;
			return sm;
		}
		return null;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature,
				exceptions);
		ServiceMethod sm=methodMatched(name, desc);
		if (sm!=null) {
			// 用于构造服务形式的返回值
			MethodVisitor ma = new WeavingMethod(sm,mv, Opcodes.ACC_PUBLIC, name,
					desc);
			return ma;
			// } else if ("<init>".equals(name)) {
			// mv.visitCode();
			// mv.visitVarInsn(Opcodes.ALOAD, 0);
			// mv.visitTypeInsn(Opcodes.NEW,
			// Type.getInternalName(CallbackEvent.class));
			// mv.visitInsn(Opcodes.DUP);
			// mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
			// Type.getInternalName(CallbackEvent.class), "<init>", "()V");
			// mv.visitFieldInsn(Opcodes.PUTFIELD,
			// this.className.replace(".", "/"), "callbackEvent",
			// Type.getDescriptor(ICallbackEvent.class));
			// mv.visitMaxs(0, 0);
			// mv.visitEnd();
			// return mv;
//		} else if (name.equals("setServiceMethodInstanceFactory")
//				&& ("(L"
//						+ Type.getInternalName(IServiceMethodInstanceFactory.class) + ";)V")
//						.equals(desc)) {
//			mv.visitCode();
//			mv.visitVarInsn(Opcodes.ALOAD, 0);
//			mv.visitVarInsn(Opcodes.ALOAD, 1);
//			mv.visitFieldInsn(Opcodes.PUTSTATIC, className.replace(".", "/"),
//					"cj$methodFactory",
//					Type.getDescriptor(IServiceMethodInstanceFactory.class));
//			mv.visitInsn(Opcodes.RETURN);
//			mv.visitMaxs(2, 2);
//			mv.visitEnd();
//			return mv;
		} else {
			mv.visitEnd();
			return mv;
		}
	}

	private class WeavingMethod extends AdviceAdapter {
		private String methodName;
		private ServiceMethod sm;
		public WeavingMethod(ServiceMethod sm,MethodVisitor mv, int access, String name,
				String desc) {
			super(Opcodes.ASM5,mv, access, name, desc);
			this.sm=sm;
			this.methodName = name;
		}

		// //方法的后代码织入，但方法还是没返回，而仍在方法体内
		@Override
		protected void onMethodExit(int opcode) {
			visitCode();
			// 只有放到最前面才能接收到返回值
			if (opcode == RETURN) {
				visitInsn(ACONST_NULL);
			} else if (opcode == ARETURN || opcode == ATHROW) {
				dup();
			} else {
				if (opcode == LRETURN || opcode == DRETURN) {
					dup2();
				} else {
					dup();
				}
				box(Type.getReturnType(this.methodDesc));
			}
			int pos=sm.getParameterTypeNames().length+1;
			int varRet=pos++;
			mv.visitVarInsn(Opcodes.ASTORE, varRet);
			mv.visitLdcInsn(serviceDefId);
			int varDefId=pos++;
			mv.visitVarInsn(Opcodes.ASTORE, varDefId);
			mv.visitLdcInsn(methodName);
			int varMethodName=pos++;
			mv.visitVarInsn(Opcodes.ASTORE, varMethodName);
			mv.visitLdcInsn(methodDesc);
			int varMethodDesc=pos++;
			mv.visitVarInsn(Opcodes.ASTORE, varMethodDesc);
			mv.visitFieldInsn(
					GETSTATIC,
					className.replace(".", "/"),
					"cj$methodFactory",
					"L"
							+ Type.getInternalName(IServiceMethodInstanceFactory.class)
							+ ";");
			mv.visitVarInsn(ALOAD, varDefId);
			mv.visitVarInsn(ALOAD, varMethodName);
			mv.visitVarInsn(ALOAD, varMethodDesc);
			mv.visitMethodInsn(
					INVOKEINTERFACE,
					Type.getInternalName(IServiceMethodInstanceFactory.class),
					"getServiceMethodInitializer",
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)L"
							+ Type.getInternalName(IServiceMethodInitializer.class)
							+ ";",true);
			int varSmi=pos++;
			mv.visitVarInsn(Opcodes.ASTORE, varSmi);
			mv.visitVarInsn(ALOAD, varSmi);
//			mv.visitInsn(IFNONNULL);
//			mv.visitInsn(ACONST_NULL);
//			mv.visitInsn(ARETURN);//上面是ARETURN一定后面加侦，虽然已使用了自动计算侦，但仍会出错，因此干脆不做非空判断
//			mv.visitFrame(Opcodes.F_APPEND, 1,
//					 new Object[] { "cj/test/website/TestMethod2" },
//					 0, null);
			mv.visitVarInsn(ALOAD, varSmi);
			mv.visitVarInsn(ALOAD, varRet);
			mv.visitMethodInsn(INVOKEINTERFACE,
					Type.getInternalName(IServiceMethodInitializer.class),
					"initReturnService", "(Ljava/lang/Object;)V",true);
//			mv.visitVarInsn(ALOAD, varRet);
//			mv.visitInsn(ARETURN);
			if (opcode == RETURN) {
				mv.visitInsn(ACONST_NULL);
				mv.visitInsn(RETURN);
			} else if (opcode == ARETURN) {
				mv.visitVarInsn(ALOAD, varRet);
				mv.visitInsn(ARETURN);
			} else if (opcode == IRETURN) {
				mv.visitVarInsn(ALOAD, varRet);
				mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
				mv.visitInsn(IRETURN);
			} else if (opcode == LRETURN) {
				mv.visitVarInsn(ALOAD, varRet);
				mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false);
				mv.visitInsn(LRETURN);
			} else if (opcode == FRETURN) {
				mv.visitVarInsn(ALOAD, varRet);
				mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
				mv.visitInsn(FRETURN);
			} else if (opcode == DRETURN) {
				mv.visitVarInsn(ALOAD, varRet);
				mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
				mv.visitInsn(DRETURN);
			}
			
//			mv.visitMaxs(pos, pos);//带上这两行后，他娘的，狗日的，只要服务方法中有ISNULL指令（即有if(xxx==null)语句方法结尾就出现两个visitEnd，因此注释掉它
//			mv.visitEnd();
		}
	}
}
