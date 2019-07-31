package cj.studio.ecm.container.factory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceMetaData;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.adapter.IAdaptable;
import cj.studio.ecm.adapter.IObjectSetter;
import cj.studio.ecm.adapter.IPrototype;
import cj.studio.ecm.annotation.MethodMode;
import cj.studio.ecm.bridge.IBridgeable;
import cj.studio.ecm.bridge.IJoinpoint;
import cj.studio.ecm.bridge.Joinpoint;
import cj.studio.ecm.container.describer.MethodDescriber;
import cj.studio.ecm.container.describer.MethodWrapper;
import cj.studio.ecm.container.describer.ParametersMehtodDescriber;
import cj.studio.ecm.container.describer.ReturnMehtodDescriber;
import cj.studio.ecm.container.describer.ServiceMethod;
import cj.ultimate.collection.ICollection;
import cj.ultimate.util.PrimitiveType;
import cj.ultimate.util.StringUtil;

public class ServiceMethodInstanceFactory extends ServiceInstanceFactory
		implements IServiceMethodInstanceFactory {
	// 含有方法定义的服务集合
	private Map<String, IServiceDefinition> serviceDefinitionMap;
	private Map<String, Map<ServiceMethod, IServiceMethodInitializer>> initializerMap;

	public ServiceMethodInstanceFactory() {
		this.serviceDefinitionMap = new HashMap<String, IServiceDefinition>();
		initializerMap = new HashMap<String, Map<ServiceMethod, IServiceMethodInitializer>>();
	}
	@Override
	public Iterator<String> iteratorServiceWithMethod() {
		return initializerMap.keySet().iterator();
	}
	@Override
	public Map<ServiceMethod, IServiceMethodInitializer> getServiceMethodInitializer(
			String service) {
		return initializerMap.get(service);
	}
	@Override
	public IServiceMethodInitializer getServiceMethodInitializer(
			IServiceDefinition def, ServiceMethod sm) {
		String sDefId = def.getServiceDescriber().getServiceId();
		Map<ServiceMethod, IServiceMethodInitializer> map = this.initializerMap
				.get(sDefId);
		if (map == null)
			return null;
		return map.get(sm);
	}

	@Override
	public Object getService(String serviceId) {
		if(IServiceMethodInstanceFactory.class.getName().equals(serviceId)){
			return this;
		}
		return getContainer().getService(serviceId);
	}

	@Override
	public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
		return getContainer().getServices(serviceClazz);
	}

	@Override
	public IServiceMethodInitializer getServiceMethodInitializer(
			String serviceDefId, String methodName, String desc) {
		Map<ServiceMethod, IServiceMethodInitializer> map = this.initializerMap
				.get(serviceDefId);
		if (map == null)
			return null;
		for (ServiceMethod sm : map.keySet()) {
			if (sm.matched(methodName, desc)) {
				return map.get(sm);
			}
		}
		throw new EcmException(String.format(
				"当前方法已定义，肯定能找到定义，但却未被找到，该方法定义可能是失败的，请检查或移除方法定义。%s.%s %s",
				serviceDefId, methodName, desc));
		// return null;
	}

	@Override
	public void refresh() {
		ICollection<String> names = getRegistry().enumServiceDefinitionNames();
		for (String name : names) {
			IServiceDefinition def = getRegistry().getServiceDefinition(name);
			if (def.getServiceDescriber() == null)
				continue;
			// 只编织服务且具有方法定义的服务
			if ((def.getServiceDescribeForm() & IServiceDefinition.SERVICE_DESCRIBEFORM) == IServiceDefinition.SERVICE_DESCRIBEFORM) {
				String className = def.getServiceDescriber().getClassName();
				if (!def.getMethods().isEmpty()) {
					Map<ServiceMethod, IServiceMethodInitializer> map = new HashMap<ServiceMethod, IServiceMethodInitializer>();
					for (ServiceMethod sm : def.getMethods()) {
						IServiceMethodInitializer smi = new ServiceMethodInitializer(
								def, sm);
						map.put(sm, smi);
						initializerMap.put(name, map);
						serviceDefinitionMap.put(className, def);
					}
				}
			}
		}

	}
	@Override
	protected void dispose(boolean disposing) {
		super.dispose(disposing);
		if(disposing) {
			serviceDefinitionMap.clear();
			this.initializerMap.clear();
		}
	}
	@Override
	protected Map<String, Object> createServiceInstanceMap() {
		return new WeakHashMap<String, Object>();
	}

	@Override
	public FactoryType getType() {
		// TODO Auto-generated method stub
		return FactoryType.method;
	}

	@Override
	public IServiceDefinition getServiceDefinition(String className) {
		// TODO Auto-generated method stub
		return this.serviceDefinitionMap.get(className);
	}

	@Override
	public boolean containsServiceDefinition(String sClassName) {
		// TODO Auto-generated method stub
		return this.serviceDefinitionMap.containsKey(sClassName);
	}

	private class ServiceMethodInitializer implements IServiceMethodInitializer {
		private IServiceDefinition def;
		private ServiceMethod sm;

		public ServiceMethodInitializer(IServiceDefinition def, ServiceMethod sm) {
			this.def = def;
			this.sm = sm;
		}

		private void initMethodService(Object service, IServiceDefinition sd,
				IServiceMetaData md) {
			try {
				String name = getNameGenerator().generateServiceName(sd,
						getRegistry());
				initialService(name, service, sd, md);
				onAfterServiceCreated(sd, md, name, service);
				getServiceInstances().put(name, service);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		}

		// 参数为要执行其方法的服务实例
		@Override
		public Object invoke(Object service, MethodMode callMode) {
			Object refservice = null;
			IServiceMetaData meta = getRegistry().getMetaData(def);
			if ((sm != null) && (def != null) && (meta != null)) {
				MethodWrapper mw = meta.getServiceMethodMeta(sm);
				for (MethodDescriber md : sm.getMethodDescribers()) {
					if (!(md instanceof ParametersMehtodDescriber)) {
						break;
					}
					ParametersMehtodDescriber pmd = (ParametersMehtodDescriber) md;
					Object[] args = null;
					if (pmd.isEmpty()) {
						int len = sm.getParameterTypeNames() == null ? 0 : sm
								.getParameterTypeNames().length;
						args = new Object[len];
					} else {
						if (pmd.getLength() != sm.getParameterTypeNames().length)
							throw new RuntimeException(
									String.format(
											"在方法%s.%s中定义的参数与实际参数数目不等，要么一个参数都不定义，要么定义全部",
											def.getServiceDescriber()
													.getServiceId(), sm
													.getBind()));
						String[] arr = sm.getParameterTypeNames();
						Class<?>[] newArr = new Class<?>[arr.length];
						for (int i = 0; i < arr.length; i++) {
							String p = arr[i];
							Class<?> cp;
							try {
								cp = PrimitiveType.convert(p,
										getContainer().getResource().getClassLoader());
							} catch (ClassNotFoundException e) {
								throw new EcmException(e);
							}
							newArr[i] = cp;
						}
						args = new Object[pmd.getLength()];
						for (int i = 0; i < pmd.getLength(); i++) {
							String argDesc = pmd.getArgDesc(i);
							String injectMode = pmd.getInjectMode(i);
							if ("value".equals(injectMode)) {
								Class<?> c=newArr[i];
								Object obj = PrimitiveType.convert(c,
										argDesc);
								args[i] = obj;
							} else if ("ref".equals(injectMode)) {
								args[i] = getService(argDesc);
							}
						}
					}
					// 以下是避免引用的死循环
					// 如果是其它服务引用时调用了这个invoke方法，则
					if (callMode == MethodMode.ref) {
						switch (sm.getCallMode()) {
						case both:
						case ref:
							refservice = mw.invoke(service, args);
							break;
						case self:
							break;
						}
					} else if (callMode == MethodMode.self) {// 如果是本服务初始化时调用的这个invoke方法
						// 如果出现死循环：resolveConstructors，当构造为methodMode=self时会陷入，因此构造已改为总是ref形式
						if ("<init>".equals(sm.getBind())) {// 虽然在注解解析器中已设，但在其它解析器中未设，因此此处再设一次,以免将来扩展解析器时忘了而陷死循环
							callMode = MethodMode.ref;
						}
						switch (sm.getCallMode()) {
						case both:
						case self:
							refservice = mw.invoke(service, args);
							break;
						case ref:
							break;
						}
					}

					// refservice = mw.invoke(service, args);
					// 如果是构造函数则需要初始化新生成的实例
					if ("<init>".equals(sm.getBind())
							|| StringUtil.isEmpty(sm.getBind())) {
						this.initReturnService(refservice);
					}
					break;

				}
			}
			return refservice;
		}

		@Override
		public void initReturnService(Object result) {
			if (sm == null || result == null)
				return;
			// 方法初始化连接点
			if (result instanceof IBridgeable) {
				IAdaptable a = (IAdaptable) result;
				IPrototype pt = a.getAdapter(IPrototype.class);
				if (pt.isBridge()) {
					IObjectSetter setter = a.getAdapter(null);
					IJoinpoint jp = null;
					jp = new Joinpoint(result);
					setter.set("cj$joinpoint", jp);
					jp.init(pt.getAspects(), getContainer());
				}
			}
			// IServiceMetaData md = getRegistry().getMetaData(def);
			// 使用当前定义
			// if (sm.isFactoryMethod()) {
			// if (!md.getServiceTypeMeta().isInstance(result))
			// throw new
			// RuntimeException(String.format("工厂方法%s必须返回当前服务类型%s",sm.getAlias(),md.getServiceTypeMeta()));
			// this.initMethodService(result, def, md);
			// }
			ReturnMehtodDescriber rmd = null;
			if ((sm.getMethodDescribeForm() & ServiceMethod.RETURN_METHOD_DESCRIBEFORM) == ServiceMethod.RETURN_METHOD_DESCRIBEFORM) {
				for (MethodDescriber m : sm.getMethodDescribers()) {
					if (m instanceof ReturnMehtodDescriber) {
						rmd = (ReturnMehtodDescriber) m;
						break;
					}
				}
			}
			String bind = StringUtil.isEmpty(sm.getBind()) ? "<init>" : sm
					.getBind();
			// 如果是构造函数则模拟一个返回，构造函数生成的新实例，类似于方法返回一个值，故而模拟返回描述
			if (rmd == null && "<init>".equals(bind)) {
				rmd = new ReturnMehtodDescriber();
				rmd.setByDefinitionId(def.getServiceDescriber().getServiceId());
			}
			if (rmd == null)
				return;
			String serviceId = rmd.getByDefinitionId();
			String type = rmd.getByDefinitionType();
			
			if(".".equals(serviceId)){//如果方法返回值对象被声明为自身类型
				serviceId=def.getServiceDescriber().getServiceId();
			}
			// 如果都为空则按返回类型走
			if (StringUtil.isEmpty(serviceId) && StringUtil.isEmpty(type)) {
				type = result.getClass().getName();
			}
			if (!StringUtil.isEmpty(serviceId)) {
				IServiceDefinition retDef = getRegistry().getServiceDefinition(
						serviceId);
				IServiceMetaData retmd = getRegistry().getMetaData(retDef);
				this.initMethodService(result, retDef, retmd);
				return;
			}
			if (!StringUtil.isEmpty(type)) {
				ICollection<String> names = getRegistry()
						.enumServiceDefinitionNames();
				for (String name : names) {
					IServiceDefinition d = getRegistry().getServiceDefinition(
							name);
					if ((d.getServiceDescribeForm() & IServiceDefinition.SERVICE_DESCRIBEFORM) != IServiceDefinition.SERVICE_DESCRIBEFORM)
						continue;
					IServiceMetaData m = getRegistry().getMetaData(d);
					if (m == null)
						continue;
					Class<?> clazz;
					try {
						clazz = getRegistry().getResource().loadClass(type);
						if (m.getServiceTypeMeta().isAssignableFrom(clazz)) {
							this.initMethodService(result, d, m);
							break;
						}
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				}
			}

		}

	}
}
