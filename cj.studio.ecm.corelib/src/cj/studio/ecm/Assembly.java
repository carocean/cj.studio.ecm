package cj.studio.ecm;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cj.studio.ecm.annotation.CjExotericalType;
import cj.studio.ecm.context.Element;
import cj.studio.ecm.context.IElement;
import cj.studio.ecm.context.IExotericalServiceSite;
import cj.studio.ecm.context.IModuleContext;
import cj.studio.ecm.context.INode;
import cj.studio.ecm.context.IProperty;
import cj.studio.ecm.context.JsonAssemblyContext;
import cj.studio.ecm.context.ModuleSite;
import cj.studio.ecm.resource.IResource;
import cj.studio.ecm.resource.JarClassLoader;
import cj.studio.ecm.resource.SystemResource;
import cj.ultimate.IClosable;
import cj.ultimate.util.StringUtil;

public class Assembly implements IAssembly, IAssemblyInfo, IClosable {
	// 一个服务容器对应一个芯片，如果容器中无芯片，则在芯片数组中对应元素为null
	private IModuleContext moduleContext;
	private IResource resource;
	private String file;
	private IAssemblyContext assemblyContext;
	private IWorkbin workbin;
	private AssemblyState state;
	private IEntryPoint entryPoint;
	private ProviderLinker providerLinker;

	Assembly(String file) {
		this.file = file;
	}

	@Override
	public String getProperty(String name) {
		return assemblyContext.getProperty(name);
	}

	public String fileName() {
		return workbin.getProperty("assembly.fileName");
	}

	@Override
	public String home() {
		return workbin.getProperty("home.dir");
	}

	@Override
	public IAssemblyInfo info() {
		return this;
	}

	protected IResource createResource(ClassLoader parent) {
		// ***********以下恢复线程上下文加载器
		if (parent != null) {
			Thread.currentThread().setContextClassLoader(parent);
			JarClassLoader loader = new JarClassLoader(parent);
			SystemResource sr = new SystemResource(loader);
			Thread.currentThread().setContextClassLoader(sr);
			return sr;
		}
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		while ((cl instanceof IResource) || (cl instanceof JarClassLoader)) {
			cl = cl.getParent();
		}
		Thread.currentThread().setContextClassLoader(cl);
		JarClassLoader loader = new JarClassLoader(cl);
		SystemResource sr = new SystemResource(loader);
		/*
		 * 这一行不能带啊，如果带了将在反复启动缺载时出大问题.
		 * 因为：当程序集第一次运行时，将当前程序集的类装载器设为当前线程的上下文，此后关闭，而当前线程的类加载器还是当前已关闭程序集的。 当第二次运行时
		 * ，由于上面两行代码的作用，将当前线程的程序集又作为当前程序集的父，即先前关闭的程序集加载器为当前程序集类加载器的父集了，故事出现同一类冲突
		 * 。因此必须注释掉。
		 * 
		 * ＃注意： 当注释掉之后，脚本引擎scriptContainer虽然传入了自定义类加载器，但仍不起作用，可能是以当前线程上下文的加载器也有关系，
		 * 因此导致在jss中获取不到自定义的java类型 所以现改为：在此赋予线程上下文，而在stop方法里恢复线程上下文。如果不恢复则导致以上描述的问题。
		 * 
		 * 目前：同时也在scriptContainer.engine()方法中为线程上下文赋了资原加载器。实际上此步多余，但为了保障起见。
		 */
		Thread.currentThread().setContextClassLoader(sr);
		return sr;
	}

	protected IModuleContext createModuleContext(IAssemblyContext context) {
		return new ModuleSite(context);
	}

	protected IAssemblyContext createAssemblyContext(IResource resource) {
		String ctxConfFile = IResource.CONTEXT_CONFIG_FILE;
		String at = resource.getResourcefile();
		File atf = new File(at);
		String home = atf.getParent();
		File h = new File(String.format("%s/properties/Assembly.json", home));
		if (h.exists()) {
			ctxConfFile = h.getAbsolutePath();
		}
		return new JsonAssemblyContext(resource, ctxConfFile);
	}

	@Override
	public String getName() {
		IAssemblyContext c = assemblyContext;
		IElement root = c.getElement();
		INode infoNode = root.getNode("assemblyInfo");
		INode at = ((Element) infoNode).getNode("assemblyTitle");

		return ((IProperty) at).getValue().getName();
	}

	@Override
	public String getCompany() {
		IAssemblyContext c = assemblyContext;
		if (c == null) {
			throw new EcmException("上下文为空：" + file);
		}
		IElement root = c.getElement();
		INode infoNode = root.getNode("assemblyInfo");
		INode gProp = ((Element) infoNode).getNode("assemblyCompany");
		return ((IProperty) gProp).getValue().getName();
	}

	@Override
	public String getCopyright() {
		IAssemblyContext c = assemblyContext;
		if (c == null) {
			throw new EcmException("上下文为空：" + file);
		}
		IElement root = c.getElement();
		INode infoNode = root.getNode("assemblyInfo");
		INode gProp = ((Element) infoNode).getNode("assemblyCopyright");
		return ((IProperty) gProp).getValue().getName();
	}

	@Override
	public InputStream getIconStream() {
		IAssemblyContext c = assemblyContext;
		if (c == null) {
			throw new EcmException("上下文为空：" + file);
		}
		IElement root = c.getElement();
		INode infoNode = root.getNode("assemblyInfo");
		INode gProp = ((Element) infoNode).getNode("assemblyIcon");
		String icon = ((IProperty) gProp).getValue().getName();
		if (StringUtil.isEmpty(icon))
			return null;

		String url = "";
		url = String.format("cj/properties/%s", icon);
		InputStream in = resource.getResourceAsStream(url);
		return in;
	}

	@Override
	public String getFileVersion() {
		IAssemblyContext c = assemblyContext;
		if (c == null) {
			throw new EcmException("上下文为空：" + file);
		}
		IElement root = c.getElement();
		INode infoNode = root.getNode("assemblyInfo");
		INode gProp = ((Element) infoNode).getNode("assemblyFileVersion");
		return ((IProperty) gProp).getValue().getName();
	}

	@Override
	public String getProduct() {
		IAssemblyContext c = assemblyContext;
		if (c == null) {
			throw new EcmException("上下文为空：" + file);
		}
		IElement root = c.getElement();
		INode infoNode = root.getNode("assemblyInfo");
		INode gProp = ((Element) infoNode).getNode("assemblyProduct");
		return ((IProperty) gProp).getValue().getName();
	}

	@Override
	public String getGuid() {
		IAssemblyContext c = assemblyContext;
		if (c == null) {
			throw new EcmException("上下文为空：" + file);
		}
		IElement root = c.getElement();
		INode infoNode = root.getNode("assemblyInfo");
		INode gProp = ((Element) infoNode).getNode("guid");
		return ((IProperty) gProp).getValue().getName();
	}

	@Override
	public String getDescription() {
		IAssemblyContext c = assemblyContext;
		IElement root = c.getElement();
		INode infoNode = root.getNode("assemblyInfo");
		INode info = ((Element) infoNode).getNode("assemblyDescription");
		IProperty ct = (IProperty) info;
		String description = ct.getValue().getName();
		return description;
	}

	@Override
	public String getVersion() {
		IAssemblyContext c = assemblyContext;
		IElement root = c.getElement();
		INode infoNode = root.getNode("assemblyInfo");
		INode info = ((Element) infoNode).getNode("assemblyVersion");
		IProperty ct = (IProperty) info;
		String version = ct.getValue().getName();
		return version;
	}

	@Override
	public AssemblyState state() {
		// TODO Auto-generated method stub
		return state;
	}

	public static IAssemblyInfo viewAssembly(String file) {
		Assembly assembly = new Assembly(file);
		assembly.init(null);
		return assembly.info();
	}

	private void init(ClassLoader parent) {
		this.resource = this.createResource(parent);
		Thread.currentThread().setContextClassLoader((ClassLoader) resource);
		this.resource.load(file);
		assemblyContext = this.createAssemblyContext(resource);
		this.moduleContext = this.createModuleContext(assemblyContext);
		this.workbin = this.createWorkBin(moduleContext);
		this.providerLinker = new ProviderLinker();
		this.moduleContext.parent(providerLinker);
		state = AssemblyState.inited;
	}

	@Override
	public void load(ClassLoader parent) {
		if ((state == null) || state == AssemblyState.inited || (state == AssemblyState.unloaded)) {
			if ((state == null) || state == AssemblyState.unloaded) {
				init(parent);
			}

			state = AssemblyState.loaded;
		}
	}

	@Override
	public void load() {
		load(null);
	}

	@Override
	public void unload() {
		this.dispose(true);
	}

	protected void dispose(boolean isDisposing) {
		if (isDisposing) {
			if (state != AssemblyState.unloaded) {
				if (state == AssemblyState.actived) {
					stop();
				}
				this.moduleContext.dispose();
				this.assemblyContext = null;
				// this.file=null;
				this.resource.dispose();
				this.resource = null;
				this.workbin = null;
				this.entryPoint = null;
				state = AssemblyState.unloaded;

			}
		}
	}

	@Override
	public void close() {
		this.dispose(true);
	}

	/**
	 * 新建程序集
	 * 
	 * @param file such as c:/temp/assembly1.jar#cj.properties.Assembly.json<br>
	 *             如果省略了#号，则在默认的位置找，asembly1.jar中的cj.properties下搜Assembly.json
	 * @return
	 */
	public static Assembly loadAssembly(String file) {
		Assembly assembly = new Assembly(file);
		// assembly.init();
		assembly.load();
		return assembly;
	}

	/**
	 * 新建程序集
	 * 
	 * @param file such as c:/temp/assembly1.jar#cj.properties.Assembly.json<br>
	 *             如果省略了#号，则在默认的位置找，asembly1.jar中的cj.properties下搜Assembly.json
	 * @return
	 */
	public static Assembly loadAssembly(String file, ClassLoader parent) {
		Assembly assembly = new Assembly(file);
		// assembly.init();
		assembly.load(parent);
		return assembly;
	}
	// 想法：扩展服务，这比连接机制性能要好。
	// 由于这种扩展具有依赖关系，所以扩展点的jar放在程序集的辅加载器下，而扩展作为程序集的主加载器下
	// 或放到主加载器也行
	// 从另一程序集扩展，被扩展程序集只暴露扩展点，并不爆漏其它服务。
	// 在开发角度讲，扩展与被扩展是项目的依赖关系，类型暴露，但容器内服务是封闭的
	// 另一个特点是，扩展间的对象调用是可强制转换的，一般是原始实例，但也可提供高性能代理方式
	// 这种方式需要将两者的类加载器连接起来，否则强类型转换都会失败
	// 两个程序集是采用扩展法还是采用连接法由作者自行选择
	// 当然这造成的了概念的模糊，好的做法是在程序集内部可支持扩展法，既一个程序集或对应几个jar，jar之间相互扩展，而在两个程序集之间只有连接方式。
	// 程序集内jar之间扩展，必然要将几个jar放到同一个加载器下，这样在不同的程序集中几个模块均从同一模块扩展时
	// 则涉及到模块的共享问题，否则同一模块将在不同的集中加载多次，占用内存，而区分共享如何共享又是个问题。
	// 会导致使用者的使用难度。

	// 因此，还是采用第一种方式妥当，一个程序集，既可被别的扩展，也可被别的连接，或二者同时都可用，更为灵活和规范。
	// 如果程序集已被装载，则调用refresh()刷新程序集，以使得类型重新装载
	// @@@@遵循有限开放类型的原则，可使得开发者定义开放给别的芯片的包或指定类型或自定匹配
	@Override
	public void dependency(IAssembly assembly) {
		// 注意：moduleContext.refresh的反复调用或start可能会导致容器加载多次，使得服务被反复创建。因此如之后修改此处代码时一定要修后测试以验证新的改动没有产生这个问题
		// 是否被重复加载，一可将断点设在moduleContext.refresh内，二可以新建一个非代理和桥的普通服务，在其构造中打印，如果是一次输出则没问题。
		Assembly another = (Assembly) assembly;
		resource.dependency(another.resource);
		if (another.state() != AssemblyState.actived) {
			another.start();
		}
		IExotericalServiceSite exoSite = (IExotericalServiceSite) another.moduleContext.getDownSite();
		exoSite.setExotericalServiceProvider(moduleContext.getDownSite());
		// $.exoteric.service.finder
		Object finder = exoSite.getService("$.exoteric.service.finder");
		if (finder != null) {
			another.moduleContext.getCoreSite().addService("$.exoteric.service.finder", finder);
		}
	}

	@Override
	public void parent(Assembly assembly) {
		// undependency(assembly);
		if (assembly.state != AssemblyState.actived) {
			assembly.start();
		}
		this.moduleContext.parent(assembly.moduleContext.getDownSite());
	}

	@Override
	public void parent(IServiceProvider parent) {
		this.providerLinker.setParent(parent);
	}

	// 解除依赖
	public void undependency(Assembly assembly) {
		// 由于依赖后类型会加载到本地资源中，可以用返射ClassLoader基类和JarClassLoader取出资源的集合，并从中仅移除依赖的资源，解除依赖可留待之后在此添加
		// 可以在JarClassLoader.dispose方法中反射基类中的类型集合以撤底消毁加载器的资源
		resource.undependency(assembly.resource);
		moduleContext.refresh();
	}

	@Override
	public IWorkbin workbin() {
		if ((state == null) || ((state != AssemblyState.actived) && (state != AssemblyState.loaded)))
			throw new RuntimeException("程序集未启动，且服务容器未激活");
		return workbin;
	}

	@Override
	public void refresh() {
		stop();
		start();
	}

	// 在服务停掉后，只清容器不卸载资源，从停止到启动只重新解析资源，生成服务。
	@Override
	public void start() {
		// 非得重新加载再启用服务才有，说明有问题。不能在停卡状态恢复时再去加载一遍资源。原因已找到，moduleContext.refresh()在调用第二遍时服务不加载
		// if(state==AssemblyState.inactived){
		// reload();
		// }
		if (state != AssemblyState.actived) {
			if (state == AssemblyState.unloaded) {
				load();
			}
			if (getEntryPoint() != null) {
				this.entryPoint.load(assemblyContext);
			}
			this.moduleContext.refresh();
			if (getEntryPoint() != null) {
				this.entryPoint.start(assemblyContext);
			}
			state = AssemblyState.actived;
		}
	}

	protected IEntryPoint getEntryPoint() {
		if (entryPoint == null)
			entryPoint = new EntryPoint();
		return entryPoint;
	}

	@Override
	public void stop() {
		if ((state != AssemblyState.inactived) && (state != AssemblyState.unloaded)) {
			if (this.entryPoint != null)
				this.entryPoint.stop(assemblyContext);
			this.moduleContext.dispose();
			// ***********以下恢复线程上下文加载器
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			while ((cl instanceof IResource) || (cl instanceof JarClassLoader)) {
				cl = cl.getParent();
			}
			Thread.currentThread().setContextClassLoader(cl);
			// ********end
			this.state = AssemblyState.inactived;
		}
	}

	protected IWorkbin createWorkBin(IModuleContext moduleContext) {
		return new Workbin(moduleContext);
	}

	@Override
	public void reload() {
		// TODO Auto-generated method stub
		this.unload();
		this.load();
	}

	class ProviderLinker implements IServiceProvider {
		IServiceProvider parent;

		public void setParent(IServiceProvider parent) {
			this.parent = parent;
		}

		@Override
		public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
			if (parent == null)
				return new ServiceCollection<>();
			return parent.getServices(serviceClazz);
		}

		@Override
		public Object getService(String serviceId) {
			Object obj = entryPoint.getService(serviceId);
			if (obj != null) {
				return obj;
			}
			if (parent == null)
				return null;
			return parent.getService(serviceId);
		}
	}

	// 限制普通服务的输出
	private class Workbin implements IWorkbin {
		private IModuleContext moduleContext;

		public Workbin(IModuleContext moduleContext) {
			this.moduleContext = moduleContext;
		}

		@Override
		public String[] enumProperty() {
			return assemblyContext.enumProperty();
		}

		@Override
		public String getProperty(String key) {
			return assemblyContext.getProperty(key);
		}

		@Override
		public IChipInfo chipInfo() {
			// 不经过getDownSite()直接取出
			IChip chip = (IChip) moduleContext.getService(IChip.class.getName());
			return chip.info();
		}

		@Override
		public List<Class<?>> exotericalType(String typeName) {
			List<Class<?>> list = resource.getPipeline().enumExotericalType();
			if (StringUtil.isEmpty(typeName)) {
				return list;
			}
			List<Class<?>> ret = new ArrayList<Class<?>>();
			for (Class<?> c : list) {
				CjExotericalType ex = c.getAnnotation(CjExotericalType.class);
				if (typeName.equals(ex.typeName())) {
					ret.add(c);
				}
			}
			return ret;
		}

		@Override
		public Object part(String name) {
			IServiceProvider sp = moduleContext.getDownSite();
			Object service = sp.getService(name);
			if (service != null)
				return service;
			if (IChipInfo.class.getName().equals(name)) {
				service = moduleContext.getDownSite().getService(name);
			}
			return service;
		}

		@Override
		public <T> ServiceCollection<T> part(Class<T> type) {
			IServiceProvider sp = moduleContext.getDownSite();
			ServiceCollection<T> services = sp.getServices(type);
			if (services != null && !services.isEmpty())
				return services;

			return services;
		}

	}

	private class EntryPoint implements IEntryPoint {

		private List<IEntryPointActivator> activators;
		private Map<String, IChipPlugin> plugins;

		public EntryPoint() {
			this.activators = new ArrayList<>();
			this.plugins = new HashMap<>();
		}

		@Override
		public Object getService(String serviceId) {
			if (plugins.isEmpty())
				return null;
			int pos = serviceId.indexOf(".");
			if (pos < 1) {
				throw new EcmException("请求插件服务格式错误，应该为：插件名.服务名");
			}
			String key = serviceId.substring(0, pos);
			String sid = serviceId.substring(pos + 1, serviceId.length());
			IChipPlugin plugin = plugins.get(key);
			if (plugin == null)
				return null;
			Object obj = plugin.getService(sid);
			if (obj != null)
				return obj;
			return null;
		}

		@Override
		public void load(IAssemblyContext ctx) {
			IElement entryPoint = (IElement) ctx.getElement().getNode("entryPoint");
			IElement plugins = (IElement) entryPoint.getNode("plugins");
			if (plugins != null) {
				try {
					for (String pluginName : plugins.enumNodeNames()) {
						IElement plugin = (IElement) plugins.getNode(pluginName);
						String plugin_class = ((IProperty) plugin.getNode("class")).getValue().getName();
						String plugin_name = ((IProperty) plugin.getNode("name")).getValue().getName();
						Class<?> clazz = Class.forName(plugin_class, true, (ClassLoader) resource);
						Object obj = clazz.newInstance();
						if (obj instanceof IChipPlugin) {
							CJSystem.logging().warn(getClass(),
									String.format("名为%s插件无效，因为未实现IChipPlugin，在类：%s", plugin_name, clazz));
							continue;
						}
						IChipPlugin a = (IChipPlugin) obj;
						IElement args = (IElement) plugin.getNode("parameters");
						a.load(ctx, args);
						this.plugins.put(plugin_name, a);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}

		@Override
		public void start(IAssemblyContext ctx) {
			IModuleContext mc = Assembly.this.moduleContext;
			ServiceCollection<IEntryPointActivator> col = mc.getServices(IEntryPointActivator.class);
			for (IEntryPointActivator a : col) {
				IElement args = new Element("parameters");
				a.activate((IServiceSite) mc, args);
				this.activators.add(a);
			}
			IElement entryPoint = (IElement) ctx.getElement().getNode("entryPoint");

			// 以下激活第三方活动器
			IElement activators = (IElement) entryPoint.getNode("activators");
			if (activators != null) {
				try {
					for (String activatorName : activators.enumNodeNames()) {
						IElement activator = (IElement) activators.getNode(activatorName);
						String activator_class = ((IProperty) activator.getNode("class")).getValue().getName();
						String activator_name = ((IProperty) activator.getNode("name")).getValue().getName();
						Class<?> clazz = Class.forName(activator_class, true, (ClassLoader) resource);
						Object obj = clazz.newInstance();
						if (obj instanceof IEntryPointActivator) {
							CJSystem.logging().warn(getClass(),
									String.format("名为%s活动器无效，因为未实现IEntryPointActivator，在类：%s", activator_name, clazz));
							continue;
						}
						IEntryPointActivator a = (IEntryPointActivator) obj;
						IElement args = (IElement) activator.getNode("parameters");
						a.activate((IServiceSite) mc, args);
						this.activators.add(a);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

		}

		@Override
		public void stop(IAssemblyContext ctx) {
			IModuleContext mc = Assembly.this.moduleContext;
			for (IEntryPointActivator a : activators) {
				a.inactivate((IServiceSite) mc);
			}
			for (IChipPlugin a : plugins.values()) {
				a.unload();
			}
		}

	}

}
