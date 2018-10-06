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
import cj.studio.ecm.IDownriverPipeline;
import cj.studio.ecm.IExotericServiceFinder;
import cj.studio.ecm.IPipeline;
import cj.studio.ecm.IRuntimeServiceInstanceFactory;
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
import cj.studio.ecm.IUpriverPipeline;
import cj.studio.ecm.IValve;
import cj.studio.ecm.Scope;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.container.ServiceConainer;
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
import cj.studio.ecm.context.pipeline.ExotericServiceFinder;
import cj.studio.ecm.context.pipeline.ExotericServicePipeline;
import cj.studio.ecm.context.pipeline.ExotericalTypePipeline;
import cj.studio.ecm.context.pipeline.LocalExotericServiceProvider;
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

public class ModuleSite implements IServiceProvider, IModuleContext,
		IServiceSite {
	private IServiceContainer container;
	private IChipInfo chipInfo;
	private IAssemblyContext context;
	private IRuntimeServiceInstanceFactory runtimeServiceInstanceFactory;
	private IServiceSite serviceSite;
	private IUpriverPipeline upriverPipeline;
	private IDownriverPipeline downriverPipeline;
	private IServiceNameGenerator nameGenerator;
	private IServiceProvider parent;
	private IScriptContainer scriptContainer;

	public ModuleSite(IAssemblyContext context) {
		init(context);
	}

	public ModuleSite(IAssemblyContext context, IServiceSite parent) {
		init(context);
		this.parent = parent;
	}

	protected void init(IAssemblyContext context) {
		this.container = new ServiceConainer(context.getResource());
		this.scriptContainer = this.createScriptContainer(context.getResource()
				.getClassLoader());
		this.context = context;
		// 一个模块站点关联一个资源引用的策略
		upriverPipeline = this.createUpriverPipeline(context.getResource());
		downriverPipeline = this.createDownriverPipeline();
		// nameGenerator = new ServiceInstanceNameGenerator();
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
	public void parent(IServiceProvider parent) {
		this.parent = parent;
	}

	private IDownriverPipeline createDownriverPipeline() {
		LocalExotericServiceProvider lp = new LocalExotericServiceProvider(
				container);
		ExotericServiceFinder owner = new ExotericServiceFinder(lp);
		ExotericServicePipeline pipeline = new ExotericServicePipeline(owner);
		return pipeline;
	}

	@Override
	public IAssemblyContext getAssemblyContext() {
		// TODO Auto-generated method stub
		return context;
	}

	// 留给派生类实现其芯片类型
	protected IChipInfo createChipInfo() {
		return new GenericChipInfo(this);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
		if (IChipInfo.class.isAssignableFrom(serviceClazz)) {
			IChipInfo[] arr = new IChipInfo[] { chipInfo };
			return new ServiceCollection(arr);
		}
		if (IDownriverPipeline.class.isAssignableFrom(serviceClazz))
			return new ServiceCollection(
					new IDownriverPipeline[] { downriverPipeline });
		return this.container.getServices(serviceClazz);
	}

	@Override
	public String[] enumProperty() {
		return context.enumProperty();
	}

	@Override
	public String getProperty(String key) {
		return context.getProperty(key);
	}

	@Override
	public Object getService(String serviceId) {
		if (IScriptContainer.class.getName().equals(serviceId)) {
			return scriptContainer;
		}
		if (IChip.class.getName().equals(serviceId)
				|| String.format("$.%s", IChip.class.getName()).equals(
						serviceId)) {
			if (chipInfo == null)
				chipInfo = createChipInfo();
			return new MyChip();
		}
		
		if (("$." + IChipInfo.class.getName()).equals(serviceId)
				|| IChipInfo.class.getName().equals(serviceId)
				|| (chipInfo != null && chipInfo.getId().equals(serviceId))) {
			if (chipInfo == null) {
				chipInfo = createChipInfo();
				return chipInfo;
			}
			return chipInfo;
		}
		if(IResource.class.getName().equals(serviceId)){
			return this.container.getResource();
		}
		if (IDownriverPipeline.class.getName().equals(serviceId))
			return downriverPipeline;
		return this.container.getService(serviceId);
	}

	@Override
	public void addService(Class<?> clazz, Object service) {
		this.runtimeServiceInstanceFactory.addService(clazz, service);
	}

	@Override
	public void removeService(Class<?> clazz) {
		// TODO Auto-generated method stub
		this.runtimeServiceInstanceFactory.removeService(clazz);
	}

	@Override
	public void addService(String serviceName, Object service) {
		// TODO Auto-generated method stub
		this.runtimeServiceInstanceFactory.addService(serviceName, service);
	}

	@Override
	public void removeService(String serviceName) {
		// TODO Auto-generated method stub
		this.runtimeServiceInstanceFactory.removeService(serviceName);
	}

	protected IUpriverPipeline createUpriverPipeline(IResource resource) {
		return new ExotericalTypePipeline(resource);
	}

	@Override
	public void refresh() {
		this.container.dispose();
		nameGenerator = new ServiceInstanceNameGenerator();
		IResource resource = context.getResource();
		IServiceDefinitionScanner scanner = new ServiceDefinitionScanner(
				this.container, resource);
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
			String pack = ((IProperty) e.getNode("package")).getValue()
					.getName();
			String extNames = ((IProperty) e.getNode("extName")).getValue()
					.getName();
			String exoterical = ((IProperty) e.getNode("exoterical"))
					.getValue().getName();
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
			upriverPipeline.addExotericalTypeName(etn, isPackage);
		}
		// 释放到临时类加载器
		annotationDSresolve.dispose();
		IServiceMethodInstanceFactory methodFactory = new ServiceMethodInstanceFactory();
		methodFactory.initialize(container, this.nameGenerator);
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
		ServiceTypeWeaverChain weaverChain = new ServiceTypeWeaverChain(
				weaverlist);
		compiler.setWeaverChain(weaverChain);
		compiler.compile();
		compiler.dispose();
		this.registerAndRefreshInstanceFactories();
	}

	protected void registerAndRefreshInstanceFactories() {
		runtimeServiceInstanceFactory = new RuntimeServiceInstanceFactory();
		runtimeServiceInstanceFactory.parent(parent);
		IServiceInstanceFactory singleon = new SingleonServiceInstanceFactory(
				this.getSite());
		singleon.parent(parent);
		IServiceInstanceFactory multiton = new MultitonServiceInstanceFactory();
		multiton.parent(parent);
		IServiceInstanceFactory jss = new JssServiceInstanceFactory();
		jss.parent(this);

		this.container.registerServiceInstanceFactory(singleon);
		this.container.registerServiceInstanceFactory(multiton);
		this.container
				.registerServiceInstanceFactory(runtimeServiceInstanceFactory);
		this.container.registerServiceInstanceFactory(jss);

		singleon.initialize(container, nameGenerator);
		multiton.initialize(container, nameGenerator);
		runtimeServiceInstanceFactory.initialize(this.container, nameGenerator);
		jss.initialize(null, nameGenerator);

		singleon.refresh();
		multiton.refresh();
		runtimeServiceInstanceFactory.refresh();
		jss.refresh();
		// 合并或组建组件
		try {
			this.combineParts();
		} catch (Exception e) {
			throw new EcmException(e);
		}
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
	protected void combineParts() throws SecurityException,
			IllegalArgumentException, NoSuchFieldException,
			IllegalAccessException, ClassNotFoundException {

		// Field contextField = Pin.class.getDeclaredField("_site");
		// contextField.setAccessible(true);

		chipInfo = createChipInfo();
		// 开放给容器内部使用，这样在服务定义中可见
		this.addService(IChipInfo.class, chipInfo);
		ICollection<IServiceAfter> col = this.getServices(IServiceAfter.class);
		for (IServiceAfter a : col) {
			a.onAfter(serviceSite);
		}
	}

	@Override
	public IServiceSite getSite() {
		if (this.serviceSite == null)
			this.serviceSite = this.createServiceSite();
		return this.serviceSite;
	}

	protected IServiceSite createServiceSite() {
		return new ServiceSite();
	}

	protected void dispose(boolean isDisposing) {
		if (isDisposing) {
			this.container.dispose();
			// this.context = null;
			this.nameGenerator = null;
			if (runtimeServiceInstanceFactory != null)
				this.runtimeServiceInstanceFactory.dispose();
			this.upriverPipeline.dispose();
			// this.runtimeServiceInstanceFactory = null;
			this.serviceSite = null;
			this.chipInfo = null;
			this.downriverPipeline.dispose();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		this.dispose(true);
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		this.dispose(true);
	}

	// 给站点提供开放服务的搜索方法
	private class SiteExotericServiceFiner implements IExotericServiceFinder {

		@Override
		public IPipeline getPipeline() {
			// TODO Auto-generated method stub
			return new IPipeline() {

				@Override
				public IValve getOwner() {
					// TODO Auto-generated method stub
					return SiteExotericServiceFiner.this;
				}
			};
		}

		@Override
		public IServiceProvider getLocalExotericServiceProvider() {
			LocalExotericServiceProvider p = new LocalExotericServiceProvider(
					container);
			return p;
		}

		@Override
		public <T> ServiceCollection<T> getExotericServices(
				Class<T> serviceClazz) {
			return downriverPipeline.getExotericServices(serviceClazz);
		}
	};

	private class ServiceSite implements IServiceSite {
		private IExotericServiceFinder finder;

		public ServiceSite() {
			finder = new SiteExotericServiceFiner();
		}

		public String getProperty(String key) {
			return ModuleSite.this.getProperty(key);
		}

		public String[] enumProperty() {
			return ModuleSite.this.enumProperty();
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
			if (IExotericServiceFinder.class.isAssignableFrom(serviceClazz)) {
				List<T> list = new ArrayList<T>();
				list.add((T) finder);
				return new ServiceCollection<T>(list);
			}
			return ModuleSite.this.getServices(serviceClazz);
		}

		@Override
		public Object getService(String serviceId) {
			if (IExotericServiceFinder.class.getName().equals(serviceId))
				return finder;
			return ModuleSite.this.getService(serviceId);
		}

		@Override
		public void addService(Class<?> clazz, Object service) {
			// TODO Auto-generated method stub
			ModuleSite.this.addService(clazz, service);
		}

		@Override
		public void removeService(Class<?> clazz) {
			// TODO Auto-generated method stub
			ModuleSite.this.removeService(clazz);
		}

		@Override
		public void addService(String serviceName, Object service) {
			// TODO Auto-generated method stub
			ModuleSite.this.addService(serviceName, service);
		}

		@Override
		public void removeService(String serviceName) {
			// TODO Auto-generated method stub
			ModuleSite.this.removeService(serviceName);
		}

	}

	private class ServiceDefinitionNameGenerator implements
			IServiceNameGenerator {

		@Override
		public String generateServiceName(IServiceDefinition definition,
				IServiceDefinitionRegistry registry) {
			ServiceDescriber sd = definition.getServiceDescriber();
			String id = sd.getServiceId();
			if ((null == id) || "".equals(id)) {
				// 如果没有ID则将类型名付给它
				id = "$." + sd.getClassName();
				sd.setServiceId(id);
			}
			if (registry.isServiceNameInUse(id)) {
				IServiceDefinition old = registry.getServiceDefinition(id);
				throw new EcmException(String.format(
						"已存在服务ID:%s,冲突class:%s与class:%s", id,
						sd.getClassName(), old.getServiceDescriber()
								.getClassName()));
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
		public String generateServiceName(IServiceDefinition definition,
				IServiceDefinitionRegistry registry) {
			// IServiceContainer container = registry.getOwner();

			String name = "";
			String defId = definition.getServiceDescriber().getServiceId();
			if (definition.getServiceDescriber().getScope() == Scope.singleon) {
				// 如果是单例保证全局唯一，
				if (!names.containsKey(defId))
					names.put(defId, 0);
				return defId;
			}
			if (definition.getServiceDescriber().getScope() == Scope.runtime) {
				if (names.containsKey(defId))
					throw new RuntimeException("已存在名为" + defId + "的服务");
			}
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
			// TODO Auto-generated method stub
			return assemblyCompany;
		}

		@Override
		public String getCopyright() {
			// TODO Auto-generated method stub
			return assemblyCopyright;
		}

		@Override
		public String getDeveloperHome() {
			// TODO Auto-generated method stub
			return assemblyDeveloperHome;
		}

		@Override
		public String getProduct() {
			// TODO Auto-generated method stub
			return assemblyProduct;
		}

		@Override
		public InputStream getIconStream() {
			// TODO Auto-generated method stub
			String url = "";
			if (CHIP_DEFAULT_ICON_KEY.equals(icon)) {// 找不到默认资源还得再调
				url = icon;
				InputStream in = container.getResource().getResourceAsStream(
						url);
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
				InputStream in = container.getResource().getResourceAsStream(
						url);
				return in;
			}

		}

		@Override
		public String[] enumProperty() {
			// TODO Auto-generated method stub
			return ModuleSite.this.enumProperty();
		}

		@Override
		public String getProperty(String name) {
			// TODO Auto-generated method stub
			return ModuleSite.this.getProperty(name);
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
						throw new EcmException(
								String.format(
										"程序集%s上下文中配置的resource欲配置成key=value对形式，而格式错误。正确格式为:key1=value1;key2=value2",
										this.getName()));
					}
					String[] arr = pair.split("=");
					if (StringUtil.isEmpty(arr[0])) {
						throw new EcmException(String.format(
								"程序集%s上下文中配置的resource欲配置成key=value对形式,而key名为空",
								this.getName()));
					}
					if (arr[0].equals("@")) {
						throw new EcmException(
								String.format(
										"程序集%s上下文中配置的resource欲配置成key=value对形式,而key名不能指定为@",
										this.getName()));
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
			// TODO Auto-generated method stub
			return id;
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return name;
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return description;
		}

		@Override
		public String getVersion() {
			// TODO Auto-generated method stub
			return version;
		}

	}

	private class MyChip implements IChip {
		@Override
		public IServiceSite site() {
			return getSite();
		}

		@Override
		public IChipInfo info() {
			return chipInfo;
		}

	}
}
