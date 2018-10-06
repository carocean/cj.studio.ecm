package cj.studio.ecm.bridge;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.adapter.IAdaptable;
import cj.studio.ecm.adapter.IPrototype;
import cj.ultimate.net.sf.cglib.proxy.Enhancer;
import cj.ultimate.net.sf.cglib.proxy.MethodProxy;
import cj.ultimate.org.objectweb.asm.Type;
import cj.ultimate.util.StringUtil;

public class Joinpoint implements IJoinpoint {
	private List<AspectWrapper> accepts;// 最终接受的方面，注意：不同的（服务属性引用者）会拥有不同的Joinpoint实例，其可接受的方面不同
	private List<String> rejects;// 表达式中计算会总共的拒绝方面
	private String express;// 最终拼和了私有方面的表达式
	private IServiceProvider provider;
	private Object service;// 原服务

	public Joinpoint() {

	}

	public Joinpoint(Object service) {
		this.service = service;
	}

	@Override
	public Object getService() {
		return service;
	}

	@Override
	public Object cut(Object bridge, Method method, MethodProxy proxy,
			Object[] args) {
		IAdaptable a = (IAdaptable) bridge;
		IPrototype pt = a.getAdapter(IPrototype.class);
		String id = pt.getServiceDefinitionId();
		ICutpoint cp = new Cutpoint(method, proxy,
				this.accepts/* 传入计算后的已接受方面，注意，不同服务引用属性到的可能不同 */, id,
				pt.unWrapper().getClass()/*method.getDeclaringClass()*/);
		return cp.cut(bridge, args);
	}

//	private Pattern p = Pattern.compile("(\\-|\\+){0,1}(\\w+)");
	Pattern p = Pattern.compile("(\\-|\\+){0,1}([\\$|\\.|\\w]+)");//带上了jss作为方面的能力：例：$.jss.mod.JssService
	/**
	 * init在服务被实例化时调用一次，将声明的方面注入作为将来的模板被属性桥接时使用
	 */
	@Override
	public void init(String aspects, IServiceProvider provider) {
		this.provider = provider;
		this.accepts = new ArrayList<AspectWrapper>();
		this.rejects = new ArrayList<String>();
		express = StringUtil.isEmpty(aspects) ? "" : aspects.trim();
		loadAspects(aspects, this.accepts, this.rejects);
	}

	private List<String> loadAspects(String aspects,
			List<AspectWrapper> accepts, List<String> rejects) {
		List<String> allows = new ArrayList<>();
		if (StringUtil.isEmpty(aspects)) {
			return allows;
		}
		if (!accepts.isEmpty()) {
			AspectWrapper[] arr = accepts.toArray(new AspectWrapper[0]);
			for (AspectWrapper w : arr) {
				if (rejects.contains(w.getName())) {
					accepts.remove(w);
					continue;
				}
				allows.add(w.getName());
			}
		}
		
		Matcher m = p.matcher(aspects);
		while (m.find()) {
			String symol = m.group(1);
			String name = m.group(2);
			if (null == symol || "+".equals(symol)) {
				if (!allows.contains(name) && !rejects.contains(name)) {
					allows.add(name);
				}
			}
			if ("-".equals(symol)) {
				if (!rejects.contains(name)) {
					rejects.add(name);
					if (allows.contains(name)) {
						allows.remove(name);
					}
				}
			}
		}
		for (String allow : allows) {
			// 在此避免已装载到模板上的方面再被加载一次，循环肯定比搜索快，而且模板方面固化在服务自身，如果再次搜索服务可能会得到不同的方面实例（如果方面被声明为多例），
			// 虽然模板的方面未变，但对于通过属性引用面对的方面来看，它可能会拥有不同方面实例，会与模板（在桥服务中声明的）方面概念冲突
			boolean isLoaded = false;
			for (AspectWrapper w : accepts) {
				if (allow.equals(w.getName())) {
					isLoaded = true;
				}
			}
			if (!isLoaded) {
				Object aspect = provider.getService(allow);
				if (aspect == null) {
					throw new EcmException(String.format("方面服务不存在：%s", allow));
				}
				accepts.add(new AspectWrapper(allow, (IAspect) aspect));
			}
		}
		return allows;
	}

	/**
	 * 每次在桥接即调用getGridge时根据对方桥服务的方面集合（模板）新建一个连接点，并拼合传入的私有连接点，私有连接点仅应用在属性引用机制中。
	 */
	@Override
	public IJoinpoint builtNewJoinpoint(
			String aspects/* 私有方面，如果以^开头，则私有方面放到模板方面之前 */) {
		if (StringUtil.isEmpty(aspects))// 没有私有方面则返回模板连接点，否则新建连接点对当前调用者
			return this;
		aspects = aspects.trim();
		boolean isPrior = aspects.startsWith("^");
		List<AspectWrapper> newaspectList = new ArrayList<AspectWrapper>();
		List<String> newrejects = new ArrayList<String>();
		String exp = "";
		if (isPrior) {
			aspects = aspects.substring(1, aspects.length());
			exp = String.format("%s%s", aspects,
					this.express.startsWith("+") || this.express.startsWith("-")
							? this.express : String.format("+%s", express));
		} else {// 如果不是优先私有点，则在此先将模板方面置入
			exp = String.format("%s%s", express,
					aspects.startsWith("+") || aspects.startsWith("-") ? aspects
							: String.format("+%s", aspects));
		}
		loadAspects(exp, newaspectList, newrejects);

		Joinpoint jp = new Joinpoint();
		jp.accepts = newaspectList;
		jp.rejects = newrejects;
		jp.express = exp;
		jp.service = service;
		jp.provider = provider;
		return jp;
	}

	@Override
	public Class<?>[] getCutInterfaces() {
		List<Class<?>> list = new ArrayList<Class<?>>();
		for (AspectWrapper aspect : this.accepts) {
			Class<?>[] arr = aspect.getAspect().getCutInterfaces();
			if (arr != null) {
				for (Class<?> c : arr) {
					if (!list.contains(c)) {
						list.add(c);
					}
				}
			}
		}
		return list.toArray(new Class<?>[0]);
	}

	private class Cutpoint implements ICutpoint {
		private Method method;
		private AspectWrapper[] accepts;
		private String serviceDefId;
		private Class<?> type;
		private int index;

		public Cutpoint(Method method, MethodProxy proxy,
				List<AspectWrapper> accepts, String serviceDefId,
				Class<?> type) {
			this.method = method;
			this.serviceDefId = serviceDefId;
			this.type = type;
			this.accepts = new AspectWrapper[accepts.size() + 1];
			for (int i = 0; i < accepts.size(); i++) {
				AspectWrapper a = accepts.get(i);
				this.accepts[i] = a;
			}
			this.accepts[accepts.size()] = new AspectWrapper("invokeAspect",
					new InvokeAspect(method, proxy));// 最后是一个方法执行器，一个切入点链是有次序的，且只有在最后一个执行方面才有真实的执行权，这主要是为了模拟一个操作方法的执行序
		}

		@Override
		public <T extends Annotation> T getMethodAnnotation(Class<T> clazz) {
			return method.getAnnotation(clazz);
		}

		// @Override
		// public Object cut(Object bridge, Object[] args) {
		// if (index < accepts.size()) {
		// IAspect current = accepts.get(index);
		// Class<?>[] arr = current.getCutInterfaces();
		// // 都为空视为拦截所有方法
		// if (arr == null || arr.length == 0) {
		// current = accepts.get(index);
		// index++;
		// return current.cut(bridge, args, this);
		// }
		// for (Class<?> c : arr) {
		// if (c.isAssignableFrom(type)) {
		// current = accepts.get(index);
		// index++;
		// return current.cut(bridge, args, this);
		// }
		// }
		// current = accepts.get(index);
		// index++;
		// return null;
		// }
		//
		// return null;
		// }
		@Override
		public Object cut(Object bridge, Object[] args) {
			if (index < accepts.length) {
				IAspect current = accepts[index].getAspect();
				index++;
				return current.cut(bridge, args, this);
			}
			throw new EcmException(String.format("切入点已执行完毕，此时溢出。执行在：%s.%s",
					type.getName(), method.getName()));
		}

		@Override
		public String getServiceDefId() {
			// TODO Auto-generated method stub
			return this.serviceDefId;
		}

		@Override
		public String getMethodName() {
			// TODO Auto-generated method stub
			return method.getName();
		}

		@Override
		public Class<?> getType() {
			// TODO Auto-generated method stub
			return this.type;
		}

		@Override
		public String getMethodDesc() {
			// TODO Auto-generated method stub
			return Type.getMethodDescriptor(method);
		}

	}

	// 此为方法执行器，在切入线的最末端。用于真正的执行方法
	private class InvokeAspect implements IAspect {
		private Method method;
		private MethodProxy proxy;

		public InvokeAspect(Method method, MethodProxy proxy) {
			this.method = method;
			this.proxy = proxy;
		}

		@Override
		public Object cut(Object bridge, Object[] args, ICutpoint point) {

			try {
				// 注释掉原因：尽量使用java返射执行，因为在cglib下容易导致适配器的unWrapper方法得不到原始对象,cglib的method是原始被代理方法可用
				// if ((getService() == null)
				// || getService().getClass().isAssignableFrom(
				// bridge.getClass()))
				if (getService() == null
						&& Enhancer.isEnhanced(bridge.getClass()))
					return proxy.invokeSuper(bridge, args);
				else {
					boolean isInstance=method.getDeclaringClass().isAssignableFrom(getService().getClass());
//					System.out.println("************"+isInstance);
					if(isInstance||Modifier.isStatic(method.getModifiers()) ){
						return method.invoke(getService(), args);
					}else{
						Method m = findMethod(method.getName(),
								method.getParameterTypes(),
								getService().getClass());
						if (m == null){
							throw new EcmException(String.format(
									"no such method in %s.%s",
									method.getDeclaringClass(), method.getName()));
						}
						return m.invoke(getService(), args);
					}
//					Method m = findMethod(method.getName(),
//							method.getParameterTypes(),
//							getService().getClass());
//					if (m == null){
//						throw new EcmException(String.format(
//								"no such method in dest object",
//								method.getDeclaringClass(), method.getName()));
//					}
//					return m.invoke(getService(), args);
//					return method.invoke(getService(), args);
				}
			} catch (Throwable e) {
				// CJSystem.current().environment().logging().error(getClass(),
				// e);
				if (e instanceof InvocationTargetException) {
					e = ((InvocationTargetException) e).getTargetException();
				}
				throw new EcmException(e);
			}

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
		@Override
		public Class<?>[] getCutInterfaces() {
			// TODO Auto-generated method stub
			return null;
		}

	}
}
