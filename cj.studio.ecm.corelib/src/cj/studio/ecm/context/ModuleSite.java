package cj.studio.ecm.context;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cj.studio.ecm.ChipInfo;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.IAssemblyContext;
import cj.studio.ecm.IChip;
import cj.studio.ecm.IChipInfo;
import cj.studio.ecm.ICombinServiceDefinitionStrategy;
import cj.studio.ecm.IExotericalResourcePipeline;
import cj.studio.ecm.IServiceAfter;
import cj.studio.ecm.IServiceContainer;
import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceDefinitionRegistry;
import cj.studio.ecm.IServiceDefinitionResolver;
import cj.studio.ecm.IServiceDefinitionScanner;
import cj.studio.ecm.IServiceInstanceFactory;
import cj.studio.ecm.IServiceMetaDataCompiler;
import cj.studio.ecm.IServiceMetaDataResolver;
import cj.studio.ecm.IServiceNameGenerator;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.IServiceTypeWeaver;
import cj.studio.ecm.Scope;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.container.ServiceContainer;
import cj.studio.ecm.container.describer.ServiceDescriber;
import cj.studio.ecm.container.factory.IServiceMethodInstanceFactory;
import cj.studio.ecm.container.factory.MultitonServiceInstanceFactory;
import cj.studio.ecm.container.factory.RuntimeServiceInstanceFactory;
import cj.studio.ecm.container.factory.ServiceMethodInstanceFactory;
import cj.studio.ecm.container.factory.SingleonServiceInstanceFactory;
import cj.studio.ecm.container.resolver.AnnotationServiceDefinitionResolver;
import cj.studio.ecm.container.resolver.AnnotationServiceMetaDataResolver;
import cj.studio.ecm.container.resolver.JsonServiceDefinitionResolver;
import cj.studio.ecm.container.resolver.JsonServiceMetaDataResolver;
import cj.studio.ecm.container.resolver.XmlServiceDefinitionResolver;
import cj.studio.ecm.container.resolver.XmlServiceMetaDataResolver;
import cj.studio.ecm.container.scanner.CombinServiceDefinitionStrategy;
import cj.studio.ecm.container.scanner.ServiceDefinitionScanner;
import cj.studio.ecm.container.scanner.ServiceMetaDataCompiler;
import cj.studio.ecm.context.pipeline.ExotericalResourcePipeline;
import cj.studio.ecm.resource.IResource;
import cj.studio.ecm.script.IScriptContainer;
import cj.studio.ecm.script.JssServiceInstanceFactory;
import cj.studio.ecm.script.ScriptContainer;
import cj.studio.ecm.weaving.AdapterWeaver;
import cj.studio.ecm.weaving.BridgeWeaver;
import cj.studio.ecm.weaving.MethodFactoryWeaver;
import cj.studio.ecm.weaving.ServiceTypeWeaverChain;
import cj.ultimate.collection.ICollection;
import cj.ultimate.util.StringUtil;

public class ModuleSite implements IModuleContext, IServiceSite {
	private IServiceContainer container;
	private IChipInfo chipInfo;
	private IAssemblyContext context;
	private IExotericalResourcePipeline exotericalResourcePipeline;
	private IServiceProvider downServiceSite;
	private IServiceNameGenerator nameGenerator;
	private IScriptContainer scriptContainer;
	private IServiceSite delegateSite;// 开发给芯片内的开发者
	private IServiceSite coreSite;// 系统级服务容器
	private IServiceContainerMonitor serviceContainerMonitor;

	public ModuleSite(IAssemblyContext context) {
		init(context);
	}

	protected void init(IAssemblyContext context) {
		this.container = new ServiceContainer(this);
		this.scriptContainer = this.createScriptContainer(context.getResource().getClassLoader());
		this.context = context;
		// 一个模块站点关联一个资源引用的策略
		exotericalResourcePipeline = this.createExotericalResourcePipeline(context.getResource());
		downServiceSite = this.createDownriverServiceSite(this);
		coreSite = new CoreSite();
	}

	@Override
	public IServiceDefinitionRegistry getRegistry() {
		return container;
	}

	@Override
	public IServiceSite getCoreSite() {
		return coreSite;
	}

	@Override
	public IServiceSite getDelegateSite() {
		if (delegateSite == null) {
			this.delegateSite = new DelegateSite();
		}
		return delegateSite;
	}

	@Override
	public IServiceProvider getDownSite() {
		return downServiceSite;
	}

	// 给所有实例工厂调用
	@Override
	public Object getService(String serviceId) {
		Object service = coreSite.getService(serviceId);
		if (service != null) {
			return service;
		}

		return container.getService(serviceId);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
		ServiceCollection<T> col1 = coreSite.getServices(serviceClazz);

		if (IChipInfo.class.isAssignableFrom(serviceClazz)) {
			IChipInfo[] arr = new IChipInfo[] { chipInfo };
			return new ServiceCollection(arr);
		}
		ServiceCollection<T> col2 = container.getServices(serviceClazz);
		if (col1.isEmpty()) {
			return col2;
		}
		if (col2.isEmpty()) {
			return col1;
		}
		List<T> list = col2.asList();
		list.addAll(col1.asList());
		return new ServiceCollection<>(list);
	}

	@Override
	public void addService(Class<?> clazz, Object service) {
		container.addService(clazz, service);
	}

	@Override
	public void addService(String serviceName, Object service) {
		container.addService(serviceName, service);
	}

	@Override
	public void removeService(Class<?> clazz) {
		container.removeService(clazz);
	}

	@Override
	public void removeService(String serviceName) {
		container.removeService(serviceName);
	}

	@Override
	public String getProperty(String key) {
		return container.getProperty(key);
	}

	@Override
	public String[] enumProperty() {
		return container.enumProperty();
	}

	protected IScriptContainer createScriptContainer(ClassLoader cl) {
		IScriptContainer scriptContainer = new ScriptContainer();
		scriptContainer.classloader(cl);
		return scriptContainer;
	}

	public IScriptContainer getScriptContainer() {
		return scriptContainer;
	}

	@Override
	public final void parent(IServiceProvider parent) {
		container.parent(parent);
	}

	protected IServiceProvider createDownriverServiceSite(IServiceProvider down) {
		return new ExotericalServiceSite(down);
	}

	@Override
	public IAssemblyContext getAssemblyContext() {
		return context;
	}

	// 留给派生类实现其芯片类型
	protected IChipInfo createChipInfo() {
		return new GenericChipInfo(this);
	}

	protected IExotericalResourcePipeline createExotericalResourcePipeline(IResource resource) {
		return new ExotericalResourcePipeline(resource);
	}

	protected void monitorServiceContainerBefore() {
		String monitorClass = this.context.serviceContainerMonitor();
		if (StringUtil.isEmpty(monitorClass)) {
			return;
		}
		try {
			Class<?> clazz = Class.forName(monitorClass, true, container.getResource().getClassLoader());
			serviceContainerMonitor = (IServiceContainerMonitor) clazz.newInstance();
			serviceContainerMonitor.onBeforeRefresh(getDelegateSite());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			throw new EcmException(e);
		}
	}

	@Override
	public void refresh() {
		this.container.dispose();
		nameGenerator = new ServiceInstanceNameGenerator();
		IResource resource = context.getResource();
		IServiceDefinitionScanner scanner = new ServiceDefinitionScanner(this.container, resource);
		AnnotationServiceDefinitionResolver annotationDSresolve = new AnnotationServiceDefinitionResolver();
		IServiceDefinitionResolver jsonDSresolver = new JsonServiceDefinitionResolver();
		IServiceDefinitionResolver xmlDsResolver = new XmlServiceDefinitionResolver();
		IServiceMetaDataResolver AnntotationMResolver = new AnnotationServiceMetaDataResolver();
		IServiceMetaDataResolver jsonMResolver = new JsonServiceMetaDataResolver();
		IServiceMetaDataResolver xmlMResolver = new XmlServiceMetaDataResolver();
		scanner.addResolver(xmlDsResolver);
		scanner.addResolver(jsonDSresolver);
		scanner.addResolver(annotationDSresolve);
		IServiceNameGenerator nameGen = new ServiceDefinitionNameGenerator();
		scanner.setServiceNameGenerator(nameGen);
		ICombinServiceDefinitionStrategy combinServiceDefStrategy = new CombinServiceDefinitionStrategy();
		scanner.setCombinServiceDefinitionStrategy(combinServiceDefStrategy);
		IElement[] scans = this.context.getScans();
		Map<String, Boolean> exotericalMap = new HashMap<String, Boolean>();
		for (IElement e : scans) {
			String pack = ((IProperty) e.getNode("package")).getValue().getName();
			String extNames = ((IProperty) e.getNode("extName")).getValue().getName();
			String exoterical = ((IProperty) e.getNode("exoterical")).getValue().getName();
			if ("true".equals(exoterical)) {
				exotericalMap.put(pack, true);
			}
			String pattern = "";
			String[] extArr = extNames.split("\\|");
			if ((null == pack) || ("".equals(pack))) {
				pack = ".*";
			} else {
				pack = pack.replace(".", "/") + "/";
			}
			if ((null == pack) || ("".equals(pack))) {
				for (String ext : extArr) {
					pattern += "^(.*\\" + ext + ")|";
				}

			} else {
				for (String ext : extArr) {
					pattern += "^(" + pack + ".*\\" + ext + ")|";
				}
			}
			pattern = pattern.substring(0, pattern.length() - 1);
			scanner.scan(pattern);
		}
		Map<String, Boolean> map = scanner.getExotericalTypeNames();
		if (!exotericalMap.isEmpty()) {// 将程序集中开放类型设置
			map.putAll(exotericalMap);
		}
		// 设置开放类
		for (String etn : map.keySet()) {
			Boolean isPackage = map.get(etn);
			exotericalResourcePipeline.addExotericalTypeName(etn, isPackage);
		}
		// 释放到临时类加载器
		annotationDSresolve.dispose();
		IServiceMethodInstanceFactory methodFactory = new ServiceMethodInstanceFactory();
		methodFactory.initialize(this, this.nameGenerator);
		methodFactory.refresh();
		container.registerServiceInstanceFactory(methodFactory);
		IServiceMetaDataCompiler compiler = new ServiceMetaDataCompiler();
		compiler.addResolver(xmlMResolver);
		compiler.addResolver(jsonMResolver);
		compiler.addResolver(AnntotationMResolver);
		compiler.setResource(resource);
		compiler.setRegistry(this.container);
		List<IServiceTypeWeaver> weaverlist = new ArrayList<IServiceTypeWeaver>();
		IServiceTypeWeaver methodWeaver = new MethodFactoryWeaver(methodFactory);
		IServiceTypeWeaver adapterWeaver = new AdapterWeaver(this.container);
		IServiceTypeWeaver bridgeWeaver = new BridgeWeaver(this.container);
		weaverlist.add(adapterWeaver);
		weaverlist.add(methodWeaver);
		weaverlist.add(bridgeWeaver);
		ServiceTypeWeaverChain weaverChain = new ServiceTypeWeaverChain(weaverlist);
		compiler.setWeaverChain(weaverChain);
		compiler.compile();
		compiler.dispose();

		this.registerAndRefreshInstanceFactories();
	}

	protected void monitorServiceContainerAfter() {
		if (serviceContainerMonitor == null) {
			return;
		}
		serviceContainerMonitor.onAfterRefresh(getDelegateSite());
	}

	protected void registerAndRefreshInstanceFactories() {
		IServiceInstanceFactory runtime = new RuntimeServiceInstanceFactory();
		IServiceInstanceFactory singleon = new SingleonServiceInstanceFactory();
		IServiceInstanceFactory multiton = new MultitonServiceInstanceFactory();
		IServiceInstanceFactory jss = new JssServiceInstanceFactory();

		this.container.registerServiceInstanceFactory(singleon);
		this.container.registerServiceInstanceFactory(multiton);
		this.container.registerServiceInstanceFactory(runtime);
		this.container.registerServiceInstanceFactory(jss);

		singleon.initialize(this, nameGenerator);
		multiton.initialize(this, nameGenerator);
		runtime.initialize(this, nameGenerator);
		jss.initialize(this, nameGenerator);
		
		monitorServiceContainerBefore();
		
		singleon.refresh();
		multiton.refresh();
		runtime.refresh();
		jss.refresh();
		// 合并或组建组件
		try {
			this.combineParts();
		} catch (Exception e) {
			throw new EcmException(e);
		}
		
		monitorServiceContainerAfter();
	}

	/**
	 * 合并组件，诸如chip\output\input等
	 * 
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 */
	protected void combineParts() throws SecurityException, IllegalArgumentException, NoSuchFieldException,
			IllegalAccessException, ClassNotFoundException {

		// Field contextField = Pin.class.getDeclaredField("_site");
		// contextField.setAccessible(true);

		chipInfo = createChipInfo();
		// 开放给容器内部使用，这样在服务定义中可见
		this.container.addService(IChipInfo.class, chipInfo);
		ICollection<IServiceAfter> col = this.getServices(IServiceAfter.class);
		for (IServiceAfter a : col) {
			a.onAfter(getDelegateSite());
		}
	}

	protected void dispose(boolean isDisposing) {
		if (isDisposing) {
			this.container.dispose();
			// this.context = null;
			this.nameGenerator = null;
			this.exotericalResourcePipeline.dispose();
			// this.runtimeServiceInstanceFactory = null;
			this.chipInfo = null;
			this.delegateSite = null;
			this.scriptContainer = null;
			this.coreSite = null;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		this.dispose(true);
	}

	@Override
	public void dispose() {
		this.dispose(true);
	}

	private class ServiceDefinitionNameGenerator implements IServiceNameGenerator {

		@Override
		public String generateServiceName(IServiceDefinition definition, IServiceDefinitionRegistry registry) {
			ServiceDescriber sd = definition.getServiceDescriber();
			String id = sd.getServiceId();
			if ((null == id) || "".equals(id)) {
				// 如果没有ID则将类型名付给它
				id = "$." + sd.getClassName();
				sd.setServiceId(id);
			}
			if (registry.isServiceNameInUse(id)) {
				IServiceDefinition old = registry.getServiceDefinition(id);
				throw new EcmException(String.format("已存在服务ID:%s,冲突class:%s与class:%s", id, sd.getClassName(),
						old.getServiceDescriber().getClassName()));
			}
			return id;
		}

	}

	// 统一为一个服务容器中的服务实例命名，保证针对各实例工厂所有服务实例名都是唯一的
	private class ServiceInstanceNameGenerator implements IServiceNameGenerator {
		private Map<String, Integer> names;

		public ServiceInstanceNameGenerator() {
			this.names = new HashMap<String, Integer>();
		}

		@Override
		public String generateServiceName(IServiceDefinition definition, IServiceDefinitionRegistry registry) {
			// IServiceContainer container = registry.getOwner();

			String name = "";
			String defId = definition.getServiceDescriber().getServiceId();
			if (definition.getServiceDescriber().getScope() == Scope.singleon) {
				// 如果是单例保证全局唯一，
				if (!names.containsKey(defId))
					names.put(defId, 0);
				return defId;
			}
//			if (definition.getServiceDescriber().getScope() == Scope.runtime) {
//				if (names.containsKey(defId))
//					throw new RuntimeException("已存在名为" + defId + "的服务");
//			}
			if (names.containsKey(defId)) {
				int i = names.get(defId);
				i = i + 1;
				name = defId + "_" + (i);
				this.names.put(name, i);
			} else {
				name = defId;
				this.names.put(name, 0);
			}

			return name;
		}

	}

	private class GenericChipInfo extends ChipInfo implements IChipInfo {
		private String id;
		private String name;
		private String description;
		private String version;
		private Map<String, String> resource;
		private String icon;
		private String assemblyCompany;
		private String assemblyCopyright;
		private String assemblyProduct;
		private String assemblyDeveloperHome;

		public GenericChipInfo(IModuleContext ctx) {
			super(ctx);
		}

		@Override
		public String getIconFileName() {
			return icon;
		}

		@Override
		public String getCompany() {
			return assemblyCompany;
		}

		@Override
		public String getCopyright() {
			return assemblyCopyright;
		}

		@Override
		public String getDeveloperHome() {
			return assemblyDeveloperHome;
		}

		@Override
		public String getProduct() {
			return assemblyProduct;
		}

		@Override
		public InputStream getIconStream() {
			String url = "";
			if (CHIP_DEFAULT_ICON_KEY.equals(icon)) {// 找不到默认资源还得再调
				url = icon;
				InputStream in = container.getResource().getResourceAsStream(url);
				if (in == null) {
					URL z = container.getResource().getResource(url);
					if (z != null) {
						String f = z.getFile();
						if (!StringUtil.isEmpty(f)) {
							try {
								in = new FileInputStream(f);
							} catch (FileNotFoundException e) {
							}
						}
					}
				}
				return in;
			} else {
				url = String.format("cj/properties/%s", icon);
				InputStream in = container.getResource().getResourceAsStream(url);
				return in;
			}

		}

		@Override
		public String[] enumProperty() {
			return ModuleSite.this.context.enumProperty();
		}

		@Override
		public String getProperty(String name) {
			return ModuleSite.this.context.getProperty(name);
		}

		@Override
		protected void built(IModuleContext ctx) {
			IAssemblyContext c = ctx.getAssemblyContext();
			IElement root = c.getElement();
			INode infoNode = root.getNode("assemblyInfo");
			Element ct = (Element) infoNode;
			IProperty atProp = (IProperty) ct.getNode("assemblyTitle");
			name = atProp.getValue().getName();
			IProperty arProp = (IProperty) ct.getNode("assemblyResource");
			String res = arProp == null ? "" : arProp.getValue().getName();
			parseResource(res);
			IProperty gProp = (IProperty) ct.getNode("guid");
			id = gProp.getValue().getName();
			IProperty adProp = (IProperty) ct.getNode("assemblyDescription");
			this.description = adProp.getValue().getName();
			IProperty avProp = (IProperty) ct.getNode("assemblyVersion");
			version = avProp.getValue().getName();
			IProperty iconProp = (IProperty) ct.getNode("assemblyIcon");
			if (iconProp != null) {
				icon = iconProp.getValue().getName();
			}
			if (StringUtil.isEmpty(icon)) {
				icon = CHIP_DEFAULT_ICON_KEY;
			}
			IProperty p = (IProperty) ct.getNode("assemblyCompany");
			if (p != null) {
				this.assemblyCompany = p.getValue().getName();
			}
			p = (IProperty) ct.getNode("assemblyCopyright");
			if (p != null)
				this.assemblyCopyright = p.getValue().getName();
			p = (IProperty) ct.getNode("assemblyProduct");
			if (p != null)
				this.assemblyProduct = p.getValue().getName();
			p = (IProperty) ct.getNode("assemblyDeveloperHome");
			if (p != null)
				this.assemblyDeveloperHome = p.getValue().getName();
		}

		private void parseResource(String res) {
			resource = new HashMap<String, String>(4);
			if (StringUtil.isEmpty(res)) {
				resource.put("@", "");// @表示未按属性配置，用于存整个resource串
				return;
			}
			if (res.contains("=")) {// 表示按属性配置，则按属性解析
				String[] pairs = res.split(";");
				for (String pair : pairs) {
					if (StringUtil.isEmpty(pair))
						continue;
					if (!pair.contains("=")) {
						throw new EcmException(String.format(
								"程序集%s上下文中配置的resource欲配置成key=value对形式，而格式错误。正确格式为:key1=value1;key2=value2",
								this.getName()));
					}
					String[] arr = pair.split("=");
					if (StringUtil.isEmpty(arr[0])) {
						throw new EcmException(
								String.format("程序集%s上下文中配置的resource欲配置成key=value对形式,而key名为空", this.getName()));
					}
					if (arr[0].equals("@")) {
						throw new EcmException(
								String.format("程序集%s上下文中配置的resource欲配置成key=value对形式,而key名不能指定为@", this.getName()));
					}
					if (arr.length < 1)
						resource.put(arr[0], "");
					if (!arr[1].contains("$(")) {
						resource.put(arr[0], arr[1]);
					} else {
						Pattern p = Pattern.compile("\\$\\((\\w+)\\)");
						processResourcePropValue(p, arr[0], arr[1]);
					}
				}
			}
			resource.put("@", res);
		}

		private void processResourcePropValue(Pattern p, String key, String v) {
			String s = "";
			Matcher m = p.matcher(v);
			while (m.find()) {
				String ref = m.group(1);
				String mv = resource.get(ref);
				if (!StringUtil.isEmpty(mv) && mv.contains("$(")) {
					processResourcePropValue(p, ref, mv);
				}
				s = m.replaceFirst(mv);
				m = p.matcher(s);
			}
			resource.put(key, s);
		}

		@Override
		public String getResource() {
			return resource.get("@");
		}

		public String getResourceProp(String key) {
			return resource.get(key);
		}

		public String[] enumeResourceProp() {
			return resource.keySet().toArray(new String[0]);
		}

		public boolean isWebChip() {
			return resource.containsKey("site");
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getDescription() {
			return description;
		}

		@Override
		public String getVersion() {
			return version;
		}

	}

	private class MyChip implements IChip {
		@Override
		public IServiceSite site() {
			return getDelegateSite();
		}

		@Override
		public IChipInfo info() {
			return chipInfo;
		}

	}

	// 给开发者调用
	class DelegateSite implements IServiceSite {
		@Override
		public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
			return ModuleSite.this.getServices(serviceClazz);
		}

		@Override
		public Object getService(String serviceId) {
			return ModuleSite.this.getService(serviceId);
		}

		@Override
		public void addService(Class<?> clazz, Object service) {
			ModuleSite.this.addService(clazz, service);
		}

		@Override
		public void removeService(Class<?> clazz) {
			ModuleSite.this.removeService(clazz);
		}

		@Override
		public void addService(String serviceName, Object service) {
			ModuleSite.this.addService(serviceName, service);
		}

		@Override
		public void removeService(String serviceName) {
			ModuleSite.this.removeService(serviceName);
		}

		@Override
		public String getProperty(String key) {
			return ModuleSite.this.getProperty(key);
		}

		@Override
		public String[] enumProperty() {
			return ModuleSite.this.enumProperty();
		}

	}

	// 提供系统核心服务
	class CoreSite implements IServiceSite {
		Map<String, Object> map;

		public CoreSite() {
			map = new HashMap<>();
			registerCoreServices();
		}

		protected void registerCoreServices() {

		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
			List<T> list = new ArrayList<>();
			for (Object obj : map.values()) {
				if (serviceClazz.isAssignableFrom(obj.getClass())) {
					list.add((T) obj);
				}
			}
			return new ServiceCollection<T>(list);
		}

		@Override
		public Object getService(String serviceId) {
			Object service = map.get(serviceId);
			if (service != null) {
				return service;
			}
			if (IServiceSite.KEY_SERVICE_SITE.equals(serviceId)) {
				return getDelegateSite();
			}
			if (IScriptContainer.class.getName().equals(serviceId)) {
				return scriptContainer;
			}
			if (IChip.class.getName().equals(serviceId)) {
				if (chipInfo == null)
					chipInfo = createChipInfo();
				return new MyChip();
			}

			if (IResource.class.getName().equals(serviceId)) {
				return container.getResource();
			}
			return null;
		}

		@Override
		public void addService(Class<?> clazz, Object service) {
			map.put(clazz.getName(), service);
		}

		@Override
		public void removeService(Class<?> clazz) {
			// TODO Auto-generated method stub
			map.remove(clazz.getName());
		}

		@Override
		public void addService(String serviceName, Object service) {
			// TODO Auto-generated method stub
			map.put(serviceName, service);
		}

		@Override
		public void removeService(String serviceName) {
			// TODO Auto-generated method stub
			map.remove(serviceName);
		}

		@Override
		public String getProperty(String key) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String[] enumProperty() {
			// TODO Auto-generated method stub
			return null;
		}

	}
}
