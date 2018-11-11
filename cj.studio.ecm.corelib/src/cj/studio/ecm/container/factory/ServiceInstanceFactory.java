package cj.studio.ecm.container.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceContainer;
import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceDefinitionRegistry;
import cj.studio.ecm.IServiceInstanceFactory;
import cj.studio.ecm.IServiceMetaData;
import cj.studio.ecm.IServiceNameGenerator;
import cj.studio.ecm.IServiceSetter;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.Scope;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.adapter.IAdaptable;
import cj.studio.ecm.adapter.IObjectSetter;
import cj.studio.ecm.adapter.IPrototype;
import cj.studio.ecm.annotation.MethodMode;
import cj.studio.ecm.bridge.IBridgeable;
import cj.studio.ecm.bridge.IJoinpoint;
import cj.studio.ecm.bridge.Joinpoint;
import cj.studio.ecm.bridge.UseBridgeMode;
import cj.studio.ecm.container.describer.BridgeJoinpoint;
import cj.studio.ecm.container.describer.FieldWrapper;
import cj.studio.ecm.container.describer.MethodDescriber;
import cj.studio.ecm.container.describer.ParametersMehtodDescriber;
import cj.studio.ecm.container.describer.PropertyDescriber;
import cj.studio.ecm.container.describer.ReturnMehtodDescriber;
import cj.studio.ecm.container.describer.ServiceDescriber;
import cj.studio.ecm.container.describer.ServiceInvertInjectionDescriber;
import cj.studio.ecm.container.describer.ServiceMethod;
import cj.studio.ecm.container.describer.ServiceProperty;
import cj.studio.ecm.container.describer.ServicePropertyValueDescriber;
import cj.studio.ecm.container.describer.ServiceRefDescriber;
import cj.studio.ecm.container.describer.ServiceSiteDescriber;
import cj.studio.ecm.container.describer.UncertainObject;
import cj.studio.ecm.context.IModuleContext;
import cj.studio.ecm.parser.IValueParser;
import cj.studio.ecm.parser.IValueParserFactory;
import cj.studio.ecm.weaving.ICanWeavingMethod;
import cj.ultimate.collection.ICollection;
import cj.ultimate.util.PrimitiveType;
import cj.ultimate.util.StringUtil;

public abstract class ServiceInstanceFactory implements IServiceInstanceFactory {
	private IServiceDefinitionRegistry registry;
	private IServiceNameGenerator nameGenerator;
	// 或许是java弱引用WeakHashMap
	private Map<String, Object> services;
	// 创建服务实例所对应的服务定义模板,它与服务集合对应，用于追宿实例的定义性质
	// 实例也可以没有定义
	private Map<String, String> instIdAndDefIdMap;

	public ServiceInstanceFactory() {

	}

	@Override
	public void initialize(IModuleContext context, IServiceNameGenerator serviceNameGenerator) {
		if (getType() == null)
			throw new RuntimeException("实例工厂没有指定区间");
		this.registry = context.getRegistry();
		this.nameGenerator = serviceNameGenerator;
		this.services = this.createServiceInstanceMap();
		this.instIdAndDefIdMap = new HashMap<String, String>();

	}

	@Override
	public String getDefinitionNameOfInstance(String instanceName) {
		// TODO Auto-generated method stub
		return instIdAndDefIdMap.get(instanceName);
	}

	/**
	 * 创建服务实例所对应的服务定义模板,它与服务集合对应，用于追宿实例的定义性质
	 * 
	 * @return
	 */
	protected Map<String, String> getInstIdAndDefIdMap() {
		return instIdAndDefIdMap;
	}

	@Override
	public abstract void refresh();

	protected Map<String, Object> getServiceInstances() {
		return this.services;
	}

	protected IServiceDefinitionRegistry getRegistry() {
		return registry;
	}

	protected IServiceNameGenerator getNameGenerator() {
		return nameGenerator;
	}

	protected abstract Map<String, Object> createServiceInstanceMap();

	protected IServiceInstanceFactory findInstanceFactory(FactoryType type) {
		IServiceContainer container = registry.getContainer();
		return container.getServiceInstanceFactory(type);
	}

	/**
	 * 创建服务的后处理。用于对服务进行修饰性加工，如修改服务的属性、注入一些东东
	 * 
	 * @param def
	 * @param meta
	 * @param service
	 */
	protected void onAfterServiceCreated(IServiceDefinition def, IServiceMetaData meta, String serviceInstName,
			Object service) {
	}

	@Override
	public Object getService(String serviceId) {
		IServiceDefinition def = registry.getServiceDefinition(serviceId);
		if (def == null) {
			return null;
		}
		if (!def.getServiceDescriber().getScope().name().equals(this.getType().name())) {
			return null;
		}
		IServiceMetaData meta = registry.getMetaData(def);
		if ((def == null) || (meta == null)) {
			return null;
		}
		if (services.containsKey(serviceId)) {
			return services.get(serviceId);
		}
		Object service = this.createNewService(def, meta);
		Object[] arr = (Object[]) service;
		service = arr[0];
		boolean isServiceInited = (boolean) arr[1];
		// 因为在服务方法执行之后服务已被初始化了
		if (isServiceInited) {
			String name = nameGenerator.generateServiceName(def, registry);
			getServiceInstances().put(name, service);
		} else {
			if (service != null) {
				try {
					String name = nameGenerator.generateServiceName(def, registry);
					this.initialService(name, service, def, meta);
					this.onAfterServiceCreated(def, meta, name, service);
					services.put(name, service);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		return service;
	}

	protected void initialService(String serviceName, Object service, IServiceDefinition def, IServiceMetaData meta)
			throws IllegalArgumentException, IllegalAccessException, SecurityException, NoSuchMethodException,
			InvocationTargetException {
		IServiceSite container = this.registry.getOwner();
		List<ServiceProperty> list = def.getProperties();
		for (ServiceProperty p : list) {
			FieldWrapper fw = meta.getServicePropMeta(p);
			if (fw == null)
				throw new RuntimeException(
						String.format("缺少属性%s", meta.getServiceTypeMeta().getName() + "." + p.getPropName()));
			Field f = fw.getField();
			ServiceRefDescriber ref = null;
			ServiceInvertInjectionDescriber siid = null;
			ServiceSiteDescriber site = null;
			ServicePropertyValueDescriber value = null;
			Iterator<PropertyDescriber> it = p.getPropertyDescribers().iterator();
			while (it.hasNext()) {
				PropertyDescriber pd = it.next();
				if (pd instanceof ServiceRefDescriber) {
					ref = (ServiceRefDescriber) pd;
					if (StringUtil.isEmpty(ref.getRefByName()) && StringUtil.isEmpty(ref.getRefByType()))
						ref.setRefByType(fw.getField().getType().getName());
				} else if (pd instanceof ServiceInvertInjectionDescriber) {
					siid = (ServiceInvertInjectionDescriber) pd;
				} else if (pd instanceof ServiceSiteDescriber) {
					site = (ServiceSiteDescriber) pd;
				} else if (pd instanceof ServicePropertyValueDescriber) {
					value = (ServicePropertyValueDescriber) pd;
				}
			}
			if (ref != null && value != null)
				throw new RuntimeException(String.format("属性%s的value和ref不能同时使用", p.getPropName()));
			if (siid != null && ref == null)
				throw new RuntimeException(String.format("属性%s的返向注入必须与ref属性描述器一起使用", p.getPropName()));
			if (site != null && (ref != null || value != null || siid != null))
				throw new RuntimeException(String.format("属性%s的服务站点注入不能与其它属性描述器一起使用", p.getPropName()));
			Object refService = null;
			if (ref != null)
				refService = this.initialServiceRef(service, def, ref, fw, container);
			if ((siid != null) && (refService != null))
				this.initialServiceIID(service, serviceName, siid, f, refService, container);
			if (value != null)
				this.initializeServicePropValue(service, def, value, fw, container);
			if ((site != null)) {
				this.initialServiceSite(service, site, f, container);
			}
		}
		String constructor = def.getServiceDescriber().getConstructor();
		if (!StringUtil.isEmpty(constructor) && def.getServiceDescriber().getScope() == Scope.singleon) {
			List<ServiceMethod> methods = def.getMethods();
			for (ServiceMethod sm : methods) {
				if ("<init>".equals(sm.getBind()) && constructor.equals(sm.getBind())) {
					initalizeServiceMethod(service, def, sm);
				}
			}
		}
	}

	private void initalizeServiceMethod(Object service, IServiceDefinition def, ServiceMethod sm) {
		IServiceMethodInstanceFactory factory = (IServiceMethodInstanceFactory) getContainer()
				.getServiceInstanceFactory(FactoryType.method);
		if (factory == null)
			throw new RuntimeException("容器中缺少服务方法实例工厂");
		IServiceMethodInitializer smi = factory.getServiceMethodInitializer(def, sm);
		if (smi == null)
			throw new RuntimeException(
					String.format("方法%s.%s缺少服务初始化器", def.getServiceDescriber().getServiceId(), sm.getAlias()));
		smi.invoke(service, MethodMode.self);
	}

	private void initializeServicePropValue(Object service, IServiceDefinition def, ServicePropertyValueDescriber value,
			FieldWrapper fw, IServiceSite container) {
		IValueParserFactory factory = (IValueParserFactory) container
				.getService("$." + IValueParserFactory.class.getName());
		if (StringUtil.isEmpty(value.getParser()))
			value.setParser("cj.basic");
		IValueParser parser = factory.getValueParser(value.getParser());
		if (parser == null)
			throw new RuntimeException(String.format("属性%s没有指定的解析器", fw.getField().getName()));
		String propFullName = fw.getField().getDeclaringClass().getName() + "." + fw.getField().getName();
		Object realValue = parser.parse(propFullName, value.getValue(), fw.getField().getType(), container);
		Field f = fw.getField();
		f.setAccessible(true);
		try {
			if (service instanceof IAdaptable) {
				IAdaptable a = (IAdaptable) service;
				IObjectSetter os = a.getAdapter(IObjectSetter.class);
				os.set(f.getName(), realValue);
			} else {
				f.set(service, realValue);
			}
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private Object initialServiceSite(Object service, ServiceSiteDescriber siteDef, Field f, IServiceSite container)
			throws IllegalArgumentException, IllegalAccessException {
		// Object serviceSite = container.getService("serviceSite");
		Object serviceSite = container.getService(IServiceSite.class.getName());
		f.setAccessible(true);
		if (service instanceof IAdaptable) {
			IAdaptable a = (IAdaptable) service;
			IObjectSetter os = a.getAdapter(IObjectSetter.class);
			os.set(f.getName(), serviceSite);
		} else {
			f.set(service, serviceSite);
		}
		return serviceSite;
	}

	// 返回引用注解字段所引用的服务的实例
	private Object initialServiceRef(Object service, IServiceDefinition def, ServiceRefDescriber ref, FieldWrapper fw,
			IServiceSite container) throws IllegalArgumentException, IllegalAccessException {
		Field f = fw.getField();
		String serviceId = ref.getRefByName();// id为空则以类型查找
		Class<?> serviceClazz = fw.getRefType();
		String methodAlias = ref.getRefByMethod();
		Object refservice = null;

		// 如果是通过服务ID引用（方法名必为空，如果不为空表示通过方法引用;）
		if (!StringUtil.isEmpty(serviceId) && StringUtil.isEmpty(methodAlias)) {
			// 将服务自身注入到自己的属性中。
			if (def.getServiceDescriber().getServiceId().equals(serviceId))
				refservice = service;
			else
				// 将形成链式调用，直到容器将各工厂的服务搜索完毕
				refservice = container.getService(serviceId);
		} else {
			// 通过ID和方法引用优先于类型引用
			if ((refservice == null) && !StringUtil.isEmpty(methodAlias)) {
				// IServiceMetaData meta = registry.getMetaData(def);
				refservice = this.initRefServiceMethod(def, serviceId, service, methodAlias, container);
			} else if (serviceClazz != UncertainObject.class) {// 没有ID也没有方法引用，而使用refType
				// 如果指定了引用的服务类型，则在现有的实例工厂的集合中搜索,注意，按类型查找不会强制引用对象的初始化，只是在现有的服务中查找，因此在其它服务未加载完之前，可能导致符合类型查找不全。
				// 因此是个bugger，应在服务容器加载完毕，对服务进行合并操作时为它赋值
				ServiceCollection<?> col = container.getServices(serviceClazz);
				refservice = col;
				if (col.isEmpty())
					refservice = container.getService("$." + serviceClazz.getName());
			} else {// 即没ID也无指定方法和类型
				// 则按字段名作为要引用的服务名（服务名与字节名必须相同）
				// 按字段的当前类型来取
				String fName = f.getName();
				refservice = container.getService(fName);
			}
		}

		UseBridgeMode useBridge = ref.getUseBridge();
		switch (useBridge) {
		case normal:
			break;
		case forbidden:// 对方哪怕是桥也使用真实对象引用
			if (refservice instanceof IBridgeable) {
				IAdaptable a = (IAdaptable) refservice;
				IPrototype pt = a.getAdapter(IPrototype.class);
				refservice = pt.unWrapper();
			}
			break;
		case auto:// 如果引用的服务是桥则桥接
			BridgeJoinpoint bjp = ref.getBridgeJoinpoint();
			if (refservice instanceof IBridgeable) {// 如果引用服务已是桥了，而存在私有方面，则以原始对象再生成桥
				if (bjp == null || StringUtil.isEmpty(bjp.getAspects())) {// 没有私有方面
					break;
				}
				IAdaptable a = (IAdaptable) refservice;
				IPrototype pt = a.getAdapter(IPrototype.class);
				Object realService = pt.unWrapper();// 因为对方是桥，为了不代理多层，在真实对象上再生成桥，这样实际上引用的服务像是多实例的，但其真实代理（如果是单例模式）则是唯一的。
				IBridgeable b = null;
				if (realService instanceof IAdaptable) {
					b = ((IAdaptable) realService).getAdapter(IBridgeable.class);
				} else {
					b = a.getAdapter(IBridgeable.class);//
				}
				refservice = b.getBridge(bjp.getAspects());
				break;
			}
			if (!(refservice instanceof IAdaptable)) {
				break;
			}
			// 不是桥实例
			IAdaptable a = (IAdaptable) refservice;
			// 下面是通过原型获取桥的定义，也可通过ref变量引用信息，从注册表查到对方的服务定义，这样性能可能更好些
			// IServiceDefinition
			// refDef=registry.getServiceDefinition(ref.getRefByName());
			// if(refDef!=null&&refDef.get){
			//
			// }
			IPrototype pt = a.getAdapter(IPrototype.class);
			if (!pt.isBridge()) {
				break;
			}
			// 不是桥实例，但是有桥声明
			IBridgeable b = a.getAdapter(IBridgeable.class);// 生成桥实例
			if (bjp == null || StringUtil.isEmpty(bjp.getAspects())) {
				refservice = b.getBridge(pt.getAspects());
			} else {
				refservice = b.getBridge(bjp.getAspects());
			}
			break;

		}
		// if (refservice instanceof IBridgeable) {
		// UseBridgeMode isUseBridge = ref.getUseBridge();
		// BridgeJoinpoint bjp = ref.getBridgeJoinpoint();
		// IBridgeable b = (IBridgeable) refservice;
		// IAdaptable a = (IAdaptable) refservice;
		// IPrototype pt = a.getAdapter(IPrototype.class);
		// if (!Enhancer.isEnhanced(serviceClazz)) {
		// if (isUseBridge) {
		// if (pt.isBridge()) {
		// if (bjp == null)
		// refservice = b.getBridge(pt.getAspects());
		// else
		// refservice = b.getBridge(bjp.getAspects());
		// }
		// }
		// } else {
		// if (!isUseBridge) {
		// refservice = pt.unWrapper();
		// }
		// }
		// }
		// boolean isBridge = ref.isUseBridge();
		// if (isBridge && (refservice instanceof IBridgeable)
		// && !Enhancer.isEnhanced(serviceClazz)) {
		// // if (!(refservice instanceof IBridgeable))
		// // throw new
		// // RuntimeException(String.format("服务的属性%s欲通过桥引用服务%s，但该服务不支持桥",
		// // def.getServiceDescriber().getServiceId() + "." +
		// // fw.getField().getName(),
		// // refservice.getClass().getName()));
		// BridgeJoinpoint bjp = ref.getBridgeJoinpoint();
		// IBridgeable b = (IBridgeable) refservice;
		// IAdaptable a = (IAdaptable) refservice;
		// IPrototype pt = a.getAdapter(IPrototype.class);
		// if (pt.isBridge()) {
		// if (bjp == null)
		// refservice = b.getBridge(pt.getAspects());
		// else
		// refservice = b.getBridge(bjp.getAspects());
		// } else {
		// refservice = pt.unWrapper();
		// }
		// }
		f.setAccessible(true);
		if (refservice instanceof ServiceCollection<?>) {
			ServiceCollection<?> col = (ServiceCollection<?>) refservice;
			Class<?> colType = f.getType();
			if (!ICollection.class.isAssignableFrom(colType)) {
				if (col.size() > 1) {
					throw new EcmException("使用serviceRef的byType注解，所修饰的属性如果超过1个元素，则必须是ICollection或其派生类。" + serviceClazz
							+ "." + f.getName());
				} else {
					refservice = col.get(0);
				}
			}
		}
		// 服务可能使用了cglib代理，如果直接使用f.set(设置类属性，则不会被代理拦截，因此造成被代理对象上有值，而原始对象的该属性为null，因此需要使用适配器设置
		if (service instanceof IAdaptable) {
			IAdaptable a = (IAdaptable) service;
			IObjectSetter os = a.getAdapter(IObjectSetter.class);
			os.set(f.getName(), refservice);
		} else {
			f.set(service, refservice);
		}
		return refservice;
	}

	protected ServiceMethod getRefMethod(Class<?> propOnServiceType, IServiceDefinition methodOnSD,
			String methodAlias) {
		ServiceMethod sm = null;
		for (ServiceMethod s : methodOnSD.getMethods()) {
			if (methodAlias.equals(s.getAlias())) {
				sm = s;
				break;
			}
		}
		if (sm == null)
			throw new RuntimeException("没有查到引用的方法定义");
		if ((sm.getMethodDescribeForm()
				& ServiceMethod.RETURN_METHOD_DESCRIBEFORM) == ServiceMethod.RETURN_METHOD_DESCRIBEFORM) {
			for (MethodDescriber md : sm.getMethodDescribers()) {
				if (md instanceof ReturnMehtodDescriber) {
					ReturnMehtodDescriber rmd = (ReturnMehtodDescriber) md;
					String serviceId = rmd.getByDefinitionId();
					String type = rmd.getByDefinitionType();
					// 如果都为空则返回类型必须是当前服务类型,然而当前服务的属性通过当前服务的方法的返回值注入，其返回值类型不能为当前服务类型，否则陷入死循环
					if (StringUtil.isEmpty(serviceId) && StringUtil.isEmpty(type)) {
						serviceId = methodOnSD.getServiceDescriber().getServiceId();
					}
					// －－－－－－－开始注释掉returnType
					// Class<?> returnType = null;
					// if (!StringUtil.isEmpty(serviceId)) {
					// IServiceDefinition retDef = getRegistry()
					// .getServiceDefinition(serviceId);
					// IServiceMetaData smd = getRegistry()
					// .getMetaData(retDef);
					// returnType = smd.getServiceTypeMeta();
					// }
					// if (!StringUtil.isEmpty(type)) {
					// ICollection<String> names = getRegistry()
					// .enumServiceDefinitionNames();
					// for (String name : names) {
					// IServiceDefinition d = getRegistry()
					// .getServiceDefinition(name);
					// IServiceMetaData m = getRegistry().getMetaData(d);
					// Class<?> clazz;
					// try {
					// clazz = getRegistry().getResource()
					// .loadClass(type);
					// if (m.getServiceTypeMeta()
					// .isAssignableFrom(clazz)) {
					// returnType = clazz;
					// break;
					// }
					// } catch (ClassNotFoundException e) {
					// throw new RuntimeException(e);
					// }
					// }
					// }
					// 先暂时还没测到此冲突，因此注掉了（2016.0822.0124）,原先做的时候出现死循环可能是两个服务互为引用导致的。returnType相关的代码均注释掉了
					// if (returnType != null) {
					// if (returnType.equals(propOnServiceType)) {
					// ServiceMethod m = sm;
					// throw new RuntimeException(String.format(
					// "如果属性引用的方法的返回值的类型是属性所在类型自身，则陷入死循环。属性所在类型：%s该类型中某属性引用的方法是：%s",
					// returnType, methodOnSD.getServiceDescriber()
					// .getServiceId() + "." + m));
					// }
					// }
					// －－－－－－－开始注释掉returnType end
					break;
				}
			}
		}
		return sm;
	}

	protected IServiceContainer getContainer() {
		if (registry instanceof IServiceContainer)
			return (IServiceContainer) registry;
		return null;
	}

	private Object initRefServiceMethod(IServiceDefinition def, String serviceId, Object propOnService,
			String methodAlias, IServiceSite container) {
		IServiceMethodInstanceFactory factory = (IServiceMethodInstanceFactory) getContainer()
				.getServiceInstanceFactory(FactoryType.method);
		if (factory == null)
			throw new RuntimeException("容器中缺少服务方法实例工厂");
		IServiceDefinition methodOnSD = null;
		Object service = null;
		ServiceMethod sm = null;
		Class<?> propOnServiceType = propOnService.getClass();
		// id为空或者引用的ID为服务自身表示引用当前服务内的方法
		if (StringUtil.isEmpty(serviceId) || (def.getServiceDescriber().getServiceId().equals(serviceId))) {
			methodOnSD = def;
			sm = getRefMethod(propOnServiceType, methodOnSD, methodAlias);
			serviceId = def.getServiceDescriber().getServiceId();
			service = propOnService;
		} else {
			methodOnSD = this.getRegistry().getServiceDefinition(serviceId);
			sm = getRefMethod(propOnServiceType, methodOnSD, methodAlias);
			if (!"<init>".equals(sm.getBind()) && !StringUtil.isEmpty(sm.getBind())) {
				service = container.getService(serviceId);
				if (service == null) {
					throw new EcmException(String.format("当前服务%s调用服务%s.%s方法失败。",
							def.getServiceDescriber().getServiceId(), serviceId, methodAlias));
				}
			}
		}

		if (methodOnSD == null || sm == null)
			return null;
		Object returnService = null;
		IServiceMethodInitializer smi = factory.getServiceMethodInitializer(methodOnSD, sm);
		if (smi == null)
			throw new EcmException(
					String.format("方法%s.%s缺少服务初始化器", methodOnSD.getServiceDescriber().getServiceId(), sm.getAlias()));
		try {
			returnService = smi.invoke(service, MethodMode.ref);
		} catch (Exception e) {
			CJSystem.current().environment().logging().error(getClass(), String.format("方法:%s.%s, 原因：%s",
					methodOnSD.getServiceDescriber().getServiceId(), sm.getAlias(), e));
			throw new EcmException(e);
		}
		return returnService;
	}

	// 初始化反向注入服务
	private void initialServiceIID(Object service, String serviceName, ServiceInvertInjectionDescriber siid, Field f,
			Object iidService, IServiceSite container) throws IllegalArgumentException, IllegalAccessException,
			SecurityException, NoSuchMethodException, InvocationTargetException {
		if (!(iidService instanceof IServiceSetter))
			throw new RuntimeException("反向注入需实现IServiceSetter接口:" + iidService);
		Method meth = iidService.getClass().getMethod("setService", String.class, Object.class);
		meth.invoke(iidService, serviceName, service);
	}

	private ServiceMethod findMethod(IServiceDefinition def, String alias) {
		ServiceMethod sm = null;
		List<ServiceMethod> list = def.getMethods();
		for (ServiceMethod m : list) {
			if (alias.equals(m.getAlias())) {
				sm = m;
				break;
			}
		}
		return sm;
	}

	private void setMethodFactory(Class<?> st)
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		IServiceContainer container = (IServiceContainer) registry;
		IServiceMethodInstanceFactory factory = (IServiceMethodInstanceFactory) container
				.getServiceInstanceFactory(FactoryType.method);
		Field f = st.getDeclaredField("cj$methodFactory");
		f.setAccessible(true);
		f.set(null, factory);
	}

	/**
	 * 返回一个数组，0为服务，1为是否已初始化，这是因为：当服务构造指向的是静态方法时会被自动初始化，因此为避免二次初始化只好返回数组
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param def
	 * @param meta
	 * @return
	 */
	protected Object createNewService(IServiceDefinition def, IServiceMetaData meta) {
		if (meta == null)
			return null;

		try {
			// 排除开放接口的定义
			if (meta.getServiceTypeMeta().isInterface())
				return null;
			Object service = null;
			Class<?> serviceType = meta.getServiceTypeMeta();
			boolean isWeaving = ICanWeavingMethod.class.isAssignableFrom(serviceType);
			boolean isServiceInited = false;
			if (isWeaving) {
				setMethodFactory(serviceType);
			}
			String constructorAlias = def.getServiceDescriber().getConstructor();
			if (StringUtil.isEmpty(constructorAlias)) {
				service = meta.getServiceTypeMeta().newInstance();
			} else {

				ServiceMethod sm = findMethod(def, constructorAlias);
				if (sm == null)
					throw new EcmException(String.format("在服务%s头中指定的构造函数别名为%s不存在，请检查它是否是一个构造函数的别名",
							def.getServiceDescriber().getServiceId(), def.getServiceDescriber().getConstructor()));
				if ("<init>".equals(sm.getBind())) {// 使用构造
					String[] arr = sm.getParameterTypeNames();
					Class<?>[] newArr = new Class<?>[arr.length];
					for (int i = 0; i < arr.length; i++) {
						String p = arr[i];
						Class<?> cp = PrimitiveType.convert(p, getContainer().getResource().getClassLoader());
						newArr[i] = cp;
					}
					Constructor<?> constructor = meta.getServiceTypeMeta().getConstructor(newArr);
					for (MethodDescriber md : sm.getMethodDescribers()) {
						if (md instanceof ParametersMehtodDescriber) {
							ParametersMehtodDescriber pmd = (ParametersMehtodDescriber) md;
							if (pmd.getLength() != newArr.length)
								throw new RuntimeException(String.format("服务%s中定义的构造%s的参数数目和实际构造的参数数目不一致",
										def.getServiceDescriber().getServiceId(), sm.getAlias()));
							Object[] values = new Object[pmd.getLength()];
							for (int j = 0; j < pmd.getLength(); j++) {
								String mode = pmd.getInjectMode(j);
								if ("ref".equals(mode)) {
									String id = pmd.getArgDesc(j);
									values[j] = getService(id);
								} else if ("value".equals(mode)) {
									String value = pmd.getArgDesc(j);
									Class<?> c = newArr[j];
									Object obj = PrimitiveType.convert(c, value);
									values[j] = obj;
								}
							}
							try {
								service = constructor.newInstance(values);
							} catch (Exception e) {
								throw new EcmException(
										String.format("指定构造创建服务时出错，在：%s，原因：%s", constructor.toString(), e));
							}
							break;
						}
					}
				} else {
					String[] arr = sm.getParameterTypeNames();
					Class<?>[] newArr = new Class<?>[arr.length];
					for (int i = 0; i < arr.length; i++) {
						String p = arr[i];
						Class<?> cp = PrimitiveType.convert(p, getContainer().getResource().getClassLoader());
						newArr[i] = cp;
					}
					Method constructor = meta.getServiceTypeMeta().getMethod(constructorAlias, newArr);
					if (!Modifier.isStatic(constructor.getModifiers())) {
						throw new EcmException(String.format("方法：%s 必须声名为静态方法", constructor.toString()));
					}
					for (MethodDescriber md : sm.getMethodDescribers()) {
						if (md instanceof ParametersMehtodDescriber) {
							ParametersMehtodDescriber pmd = (ParametersMehtodDescriber) md;
							if (pmd.getLength() != newArr.length)
								throw new EcmException(String.format("服务%s中定义的静态方法:%s的参数数目和实际构造的参数数目不一致",
										def.getServiceDescriber().getServiceId(), sm.getAlias()));
							Object[] values = new Object[pmd.getLength()];
							for (int j = 0; j < pmd.getLength(); j++) {
								String mode = pmd.getInjectMode(j);
								if ("ref".equals(mode)) {
									String id = pmd.getArgDesc(j);
									values[j] = getService(id);
								} else if ("value".equals(mode)) {
									String value = pmd.getArgDesc(j);
									Class<?> c = newArr[j];
									Object obj = PrimitiveType.convert(c, value);
									values[j] = obj;
								}
							}
							service = constructor.invoke(null, values);
							isServiceInited = true;
							break;
						}
					}
				}

			}

			if (service instanceof IBridgeable) {
				IBridgeable b = (IBridgeable) service;
				IAdaptable a = (IAdaptable) service;
				IPrototype pt = a.getAdapter(IPrototype.class);
				if (pt.isBridge()) {
					IObjectSetter setter = a.getAdapter(null);
					IJoinpoint jp = null;
					jp = new Joinpoint(service);
					setter.set("cj$joinpoint", jp);
					jp.init(pt.getAspects(), getContainer());
					service = b.getBridge(null);// 去了pt.aspects
												// 由于已在joinpoint.init方法中传入了公共方面，因此此处传入null即可，带上虽然不出错，但表达式会累加，不好看
				}
			}
			return new Object[] { service, isServiceInited };
		} catch (Exception e) {
			throw new EcmException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
		List<T> list = new ArrayList<T>();
		// Collection<Object> set = this.services.values();
		// for (Object obj : set) {
		// if (serviceClazz.isAssignableFrom(obj.getClass())) {
		// list.add((T) obj);
		// }
		// }
		ICollection<String> col = registry.enumServiceDefinitionNames();
		for (String name : col) {
			IServiceDefinition def = registry.getServiceDefinition(name);
			if (def == null)
				continue;
			ServiceDescriber sd = def.getServiceDescriber();
			try {
				Class<?> sclass = Class.forName(sd.getClassName(), true, registry.getResource().getClassLoader());
				if (serviceClazz.isAssignableFrom(sclass)) {
					T obj = (T) getService(sd.getServiceId());
					if (obj == null)
						continue;
					list.add(obj);
				}
			} catch (ClassNotFoundException e) {
				throw new EcmException(e);
			}
		}
		return new ServiceCollection<T>(list);
	}

	@Override
	public abstract FactoryType getType();

	protected void dispose(boolean disposing) {
		if (disposing) {
			this.registry = null;
			if (services != null) {
				this.services.clear();
			}
		}
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		this.dispose(true);
	}

	@Override
	protected void finalize() throws Throwable {
		this.dispose(true);
		super.finalize();
	}

}
