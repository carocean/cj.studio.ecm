package cj.studio.ecm.bridge;

import java.lang.reflect.Method;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.adapter.IAdaptable;
import cj.studio.ecm.adapter.IPrototype;
import cj.ultimate.net.sf.cglib.proxy.Enhancer;
import cj.ultimate.net.sf.cglib.proxy.MethodInterceptor;
import cj.ultimate.net.sf.cglib.proxy.MethodProxy;
import cj.ultimate.org.objectweb.asm.Type;

public class EnhanceInterceptor implements MethodInterceptor {
	private IJoinpoint joinpoint;
	static String getAdapterDesc = "(Ljava/lang/Class;)Ljava/lang/Object;";
	static String getBridgeDesc = "(Ljava/lang/String;)Ljava/lang/Object;";

	public EnhanceInterceptor(IJoinpoint jp) {
		this.joinpoint = jp;
	}

	@Override
	public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
		String desc = Type.getMethodDescriptor(method);
		// 系统方法不会拦截
		if ((method.getName().equals("getAdapter") && desc.equals(getAdapterDesc))
				|| ((method.getName().equals("getBridge")) && (desc.endsWith(getBridgeDesc)))) {
			// 注掉原因：由于在桥模式下，proxy.invokeSuper(obj,
			// args);执行的obj对象已是代理对象，导致adapter接口的unWrapper()获取不到被代理对象。因此不用cglib的invokeSuper方法，cglib的method是原始被代理方法可用
			// if
			// (joinpointIsNull||joinpoint.getService().getClass().isAssignableFrom(obj.getClass())){
			// return proxy.invokeSuper(obj, args);
			// }else {
			// Method m = this.findMethod(method.getName(),
			// method.getParameterTypes(), joinpoint.getService().getClass());
			// if (m == null)
			// throw new RuntimeException("no such method in dest object");
			return method.invoke(joinpoint.getService(), args);
			// }
		}
		boolean joinpointIsNull = (joinpoint == null) || (joinpoint.getService() == null);
		if (joinpointIsNull) {
			if (Enhancer.isEnhanced(obj.getClass())) {
				return proxy.invokeSuper(obj, args);
			} else {
				method.invoke(obj, args);
			}
		}
		if(method.getName().equals("cutMethod")){
			String mname=(String)args[0];
			IAdaptable a=(IAdaptable)obj;
			Class<?> c=a.getAdapter(IPrototype.class).unWrapper().getClass();
			Class<?>[] argTypes=(Class<?>[])args[1];
			Method m=findMethod(mname,argTypes,c);
			if(m==null){
				throw new EcmException(String.format("no such method %s", mname));
			}
			Object[] argsObjs=(Object[])args[2];
			return joinpoint.cut(obj, m, proxy, argsObjs);
		}
		Class<?> methodOnClass=method.getDeclaringClass();
		if(methodOnClass.isInterface()){//因为切面可能会用到查找方法的注解，而cj服务的规范是只在类注解，接口是干净的，因此获得原始类的方法而不用接口的方法
			IAdaptable a=(IAdaptable)obj;
			methodOnClass=a.getAdapter(IPrototype.class).unWrapper().getClass();
			Method m=findMethod(method.getName(),method.getParameterTypes(),methodOnClass);
			if(m==null){
				throw new EcmException(String.format("no such method %s", method));
			}
			return joinpoint.cut(obj, m, proxy, args);
		}
		return joinpoint.cut(obj, method, proxy, args);
	}

	private Method findMethod(String name, Class<?>[] argTypes,
			Class<?> clazz) {
		Method m = null;
		try {
			m = clazz.getDeclaredMethod(name, argTypes);
		} catch (NoSuchMethodException e) {
			if (!clazz.equals(Object.class)) {
				Class<?> superC = clazz.getSuperclass();
				m = findMethod(name, argTypes, superC);
			}
		}
		return m;
	}
}
