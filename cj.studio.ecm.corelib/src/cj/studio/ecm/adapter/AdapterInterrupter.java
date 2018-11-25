package cj.studio.ecm.adapter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.util.ObjectHelper;
import cj.ultimate.net.sf.cglib.proxy.Enhancer;
import cj.ultimate.net.sf.cglib.proxy.InvocationHandler;
import cj.ultimate.util.StringUtil;

//适配器实现
/**
 * 此为接口Iadaptable的拦截方法实现。
 * 
 * <pre>
 *
 * </pre>
 * 
 * @author carocean
 *
 */
public class AdapterInterrupter implements IAdapterInterrupter {
	private Object adaptee;
	// 当前服务实例的原型，即服务定义的ID
	private IPrototype prototype;
	private ProxySetter proxySetter;

	public AdapterInterrupter(Object adaptee, String defId, String aspects, String isBridge) {
		this.adaptee = adaptee;
		boolean is = StringUtil.isEmpty(isBridge) ? false : Boolean.valueOf(isBridge);
		this.prototype = new Prototype(defId, aspects, is);
		proxySetter = new ProxySetter();
	}
	public AdapterInterrupter(Object adaptee) {
		this.adaptee = adaptee;
		CjService cs=adaptee.getClass().getAnnotation(CjService.class);
		String name=cs==null?adaptee.getClass().getName():cs.name();
		this.prototype = new Prototype(name, "", false);
		proxySetter = new ProxySetter();
	}
	private Field findField(Class<?> clazz, String fieldName) {
		Field f = null;
		try {
			f = clazz.getDeclaredField(fieldName);
			return f;
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (NoSuchFieldException e) {
			if (!clazz.equals(Object.class)) {
				return findField(clazz.getSuperclass(), fieldName);
			}
		}
		return f;
	}

	@Override
	public Object invoke(Object adapter, Method method, Object[] args) throws Throwable {
		if (method.getName().equals("getAdapter")) {
			Object arg = args[0];
			if (arg instanceof IAdaptable)
				return adapter;
			Class<?> argClazz = (Class<?>) arg;
			Enhancer e=new Enhancer();
			e.setSuperclass(Object.class);
			e.setClassLoader(argClazz.getClassLoader());
			if(argClazz.isInterface()){
				e.setInterfaces(new Class<?>[] { argClazz, IAdaptable.class });
				e.setCallback(this);
				return e.create();
			}
			List<Class<?>> faces=new ArrayList<>();
			ObjectHelper.fetchAllInterface(argClazz, faces);
			if(!faces.contains(IAdaptable.class)){
				faces.add(IAdaptable.class);
			}
			e.setInterfaces(faces.toArray(new Class<?>[0]));
			e.setCallback(this);
			return e.create();
		}
		if (IProxySetter.class.isInstance(adapter)) {
			if ("proxy".equals(method.getName())) {
				proxySetter.proxy((InvocationHandler) args[0]);
				return null;
			}
			if (proxySetter.callback != null) {
				return proxySetter.callback.invoke(adaptee, method, args);
			}
			return method.invoke(adaptee, args);
		}
		if (IObjectSetter.class.isInstance(adapter)) {
			if ("set".equals(method.getName())) {
				String fName = (String) args[0];
				// Field f=adaptee.getClass().getDeclaredField(fName);
				Field f = findField(adaptee.getClass(), fName);
				if (f == null)
					throw new NoSuchFieldException("本类或基类中不存在属性：" + adaptee.getClass() + "   fieldName:" + fName);
				f.setAccessible(true);
				f.set(adaptee, args[1]);//如果要设的实例是代理桥而转换失败，很可能是因为使用了方面的对象没有通过接口引用。这是为了引导开发者多使用接口
				return null;
			}
			if ("get".equals(method.getName())) {
				String fName = (String) args[0];
				// Field f=adaptee.getClass().getDeclaredField(fName);
				Field f = findField(adaptee.getClass(), fName);
				if (f == null)
					throw new NoSuchFieldException("本类或基类中不存在属性：" + adaptee.getClass() + "   fieldName:" + fName);
				f.setAccessible(true);
				return f.get(adaptee);
			}
		}
		if (IActuator.class.isInstance(adapter)) {
			if (method.getName().equals("exeCommand")) {
				String commondAction = (String) args[0];
				Object[] commondArgs = (Object[]) args[1];
				Class<?>[] argTypes = new Class<?>[commondArgs.length];
				for (int i = 0; i < commondArgs.length; i++) {
					Object obj = commondArgs[i];
					argTypes[i] = obj.getClass();
				}
				if (adaptee instanceof ICommand) {
					ICommand inter = (ICommand) adapter;
					return inter.exeCommand(adapter, commondAction,argTypes, commondArgs);
				}
				Method foundMeth = findMethod(commondAction, argTypes, adaptee.getClass());
				if (foundMeth != null)
					return foundMeth.invoke(adaptee, commondArgs);
				else{
					throw new NoSuchMethodException(adaptee.getClass()+"." +commondAction);
				}
			}
			if (method.getName().equals("exactCommand")) {
				String commondAction = (String) args[0];
				Object[] commondArgs = (Object[]) args[2];
				Class<?>[] argTypes =(Class<?>[]) args[1];
				if (adaptee instanceof ICommand) {
					ICommand inter = (ICommand) adapter;
					return inter.exeCommand(adapter, commondAction, argTypes,commondArgs);
				}
				Method foundMeth = findMethod(commondAction, argTypes, adaptee.getClass());
				if (foundMeth != null)
					return foundMeth.invoke(adaptee, commondArgs);
				else{
					throw new NoSuchMethodException(adaptee.getClass()+"." +commondAction);
				}
			}
		}
		if (IPrototype.class.isInstance(adapter)) {
			// if ("getServiceDefinitionId".equals(method.getName())) {
			// return prototype.getServiceDefinitionId();
			// }
			// if ("getAspects".equals(method.getName())) {
			// return prototype.getServiceDefinitionId();
			// }
			// if ("isBridge".equals(method.getName())) {
			// return prototype.getServiceDefinitionId();
			// }
			Method foundMeth = findMethod(method.getName(), method.getParameterTypes(), prototype.getClass());
			if (foundMeth != null)
				return foundMeth.invoke(prototype, args);
		}
		Class<?> owner = method.getDeclaringClass();
		if (owner.isInstance(adaptee)) {
			method.setAccessible(true);
			return method.invoke(adaptee, args);
		}
		if(adaptee instanceof ICommand){
			return ((ICommand)adaptee).exeCommand(adapter, method.getName(),method.getParameterTypes(), args);
		}
		Method foundMeth = findMethod(method.getName(), method.getParameterTypes(), adaptee.getClass());
		if (foundMeth != null)
			return foundMeth.invoke(adaptee, args);
		else
			throw new NoSuchMethodException(method.getName());
	}
	private Method findMethod(String name, Class<?>[] argTypes, Class<?> clazz) {
		Method m = null;
		try {
			m = clazz.getDeclaredMethod(name, argTypes);
		} catch (NoSuchMethodException e) {
			if (!Object.class.equals(clazz)) {
				Class<?> superC = clazz.getSuperclass();
				m = findMethod(name, argTypes, superC);
			}
		}
		return m;
	}

	// private static Class<?>[] getInterfaces(Class<?> type) {
	// if (type.isInterface())
	// return new Class<?>[] { type };
	// Queue<Class<?>> queue = new LinkedList<Class<?>>();
	// queue.add(type);
	// List<Class<?>> list = new ArrayList<Class<?>>();
	// while (!queue.isEmpty()) {
	// Class<?> cur = queue.poll();
	// Class<?>[] arr = cur.getInterfaces();
	// list.addAll(Arrays.asList(arr));
	// Class<?> parent = cur.getSuperclass();
	// if (parent != Object.class) {
	// queue.add(parent);
	// }
	// }
	// return list.toArray(new Class<?>[0]);
	// }

	private class Prototype implements IPrototype {
		private String serviceDefinitionId;
		private String aspects;
		private boolean isBridge;

		public Prototype(String defId, String aspects, boolean isBridge) {
			this.serviceDefinitionId = defId;
			this.aspects = aspects;
			this.isBridge = isBridge;
		}

		public Object unWrapper() {
			return adaptee;
		}

		@Override
		public String getServiceDefinitionId() {
			// TODO Auto-generated method stub
			return this.serviceDefinitionId;
		}

		@Override
		public boolean isBridge() {
			// TODO Auto-generated method stub
			return isBridge;
		}

		@Override
		public String getAspects() {
			// TODO Auto-generated method stub
			return aspects;
		}
	}

	class ProxySetter implements IProxySetter {
		InvocationHandler callback;

		@Override
		public void proxy(InvocationHandler callback) {
			this.callback = callback;
		}
	}
}
