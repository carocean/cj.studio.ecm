package cj.studio.ecm.resource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IExotericalResourcePipeline;
import cj.studio.ecm.domain.AbstractDomain;
import cj.studio.ecm.domain.ChipDomain;
import cj.studio.ecm.domain.DomainTypeFactory;
import cj.studio.ecm.domain.IDomain;
import cj.studio.ecm.util.ObjectmMedium;
import cj.studio.ecm.weaving.ServiceTypeWeaverChain;
import cj.ultimate.util.FileHelper;

public class SystemResource extends JarClassLoader implements IResource,
		IRuntimeBoundary {
	private List<String> resourceNames;
	private IExotericalResourcePipeline pipeline;
	private ClassLoader jdkLoader;
	// 仅仅存放那些有静态服务方法类型，因为这些类型需要被代理
	private ServiceTypeWeaverChain weaverChain;
	private Map<String, Class<?>> serviceTypes;
	protected IDomain domain;
	// 域的上下文
	private ObjectmMedium domainPropsRef;
	private String resourcefile;
	public SystemResource() {
		super();
		this.init();
	}

	public IDomain getDomain() {
		return domain;
	}

	@Override
	public IExotericalResourcePipeline getPipeline() {
		return pipeline;
	}

	@Override
	public void setWeaverChain(ServiceTypeWeaverChain weaverChain) {
		this.weaverChain = weaverChain;
		this.serviceTypes = new HashMap<String, Class<?>>();
	}

	@Override
	public ClassLoader getClassLoader() {
		// TODO Auto-generated method stub
		return this;
	}

	public SystemResource(ClassLoader parent) {
		super(parent);
		this.init();
	}

	@Override
	protected void dispose(boolean disposing) {
		if (disposing) {
			this.jdkLoader = null;
			this.pipeline = null;
			this.resourceNames.clear();
			this.serviceTypes.clear();
			this.domainPropsRef.clear();
			super.dispose(disposing);
		}
	}

	private void init() {
		this.resourceNames = new ArrayList<String>();
		jdkLoader = this.findJDKClassLoader(this);
		this.serviceTypes = new HashMap<String, Class<?>>();
		loadDomain();
	}

	@Override
	public Enumeration<String> enumResourceNames() {
		return Collections.enumeration(this.resourceNames);
	}

	@Override
	protected void onLoadJar(JarEntry entry) {
		if (resourceNames == null)
			resourceNames = new ArrayList<String>();
		this.resourceNames.add(entry.getName());
	}

	private ClassLoader findJDKClassLoader(ClassLoader loader) {
		ClassLoader parent = loader.getParent();
		if ((parent == null)) {
			return loader;
		}
		return this.findJDKClassLoader(parent);
	}

	// 资源的加载顺序改变为：优先JDK加载器，其次为本资源加载器，再次是依赖的资源加载器
	// 目的是：不能覆盖JDK的同名资源，但在不同的芯片依赖中，优先自己的资源加载，将会覆盖父芯片中的同名资源，即资源有限共享：本中无则用父，父本中有则用本
	// 注意：如果在eclipse中启动，会将eclipse项目依赖的芯片项目都装载到sun.misc.Launcher$AppClassLoader中，这样同样导至同名资源被上级芯片所覆盖
	// 上述问题解决方法：由于eclipse启动和java外部启动或web
	// container启动其应用类加载器加载的项目不同，所以应先排除应用类加载器，而直接使用根加载器
	@Override
	public URL getResource(String name) {
		if(jdkLoader==null) return null;
		URL url = jdkLoader.getResource(name);
		if (url == null)
			url = this.findResource(name);
		if ((url == null) && (pipeline != null)) {
			url = pipeline.searchResource(name);
		}
		return url;
	}

	private Class<?> weave(String className) throws ClassNotFoundException {
		if (this.weaverChain == null)
			return null;
		Class<?> c = null;
		if (this.serviceTypes.containsKey(className)) {
			c = this.serviceTypes.get(className);
		}
		if (c != null)
			return c;
		// 作用是避免非本加载器的类的侵入
		String classResource = className.replace(".", "/") + ".class";
		InputStream is = this.getResourceAsStream(classResource);
		try {
			byte[] b = FileHelper.readFully(is);//注意，如果字节码没有完全读取则会报出怪异的asm错误
			byte[] newServiceTypeBytes = weaverChain.weave(className, b);
			if (newServiceTypeBytes == null)
				throw new RuntimeException("编织器没起作用");
			// 在解析前判断要装入的类资源是否服务定义且含有静态方法定义，如果有则为之生成代理类加入
			c = this.resovleClass(className, newServiceTypeBytes, 0,
					newServiceTypeBytes.length);
		} catch (IOException e) {
			throw new ClassNotFoundException(className);
		}
		if (c == null)
			throw new ClassNotFoundException(className);
		this.serviceTypes.put(className, c);
		return c;
	}

	private Class<?> resovleClass(String name, byte[] b, int off, int len) {
		Class<?> c = findLoadedClass(name);
		if (c != null)
			return c;
		return this.defineClass(name, b, off, len);
	}

	private Class<?> loadDomainClass(String className) {
		byte[] b = DomainTypeFactory.generate(className);
		Class<?> c = defineClass(className, b, 0, b.length);
		resolveClass(c);
		domainPropsRef.set(className, c);
		return c;
	}

	protected void loadDomain() {
		domainPropsRef = new ObjectmMedium();
		String cl1 = AbstractDomain.class.getName();
		loadDomainClass(cl1);
		String cl2 = ChipDomain.class.getName();
		Class<?> c = loadDomainClass(cl2);
		try {
			Constructor<?> con = c.getDeclaredConstructor(ObjectmMedium.class);
			domain = (IDomain) con.newInstance(domainPropsRef);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	// 类的加载顺序改变为：优先JDK加载器，其次是依赖的资源加载器，再次为本资源加载器
	// 目的是：不能覆盖JDK的同名类，在不同的芯片依赖中，优先依赖的同名类型加载，是为了类型在所依赖的芯片中转换使用，即：覆盖本地同名类型，在依赖双方中共享了类型的使用
	// 限制开发者通过类型的.getClassLoader获取加载器创建非开放类型，方法：
	@Override
	protected synchronized Class<?> loadClass(String sClassName,
			boolean bResolve) throws ClassNotFoundException {
		// 获取方法的调用者，这样能获得调用者的类加载器，如果与本加载器不同，则限制对该类型的访问权限。
		// Throwable t = new Throwable();
		// for (StackTraceElement ste : t.getStackTrace()) {
		// System.out.println("SystemResource...fetch 当前方法的调用者"
		// + ste.getClassName() + "." + ste.getMethodName());
		// }
		if (sClassName.equals(ChipDomain.class.getName())) {
			return (Class<?>) domainPropsRef.get(ChipDomain.class.getName());
		}
		if (sClassName.equals(AbstractDomain.class.getName())) {
			return (Class<?>) domainPropsRef
					.get(AbstractDomain.class.getName());
		}
		Class<?> c = null;
		c = findLoadedClass(sClassName);
		if (c != null)
			return c;
		try{
			ClassLoader cl=getParent();
			c=cl.loadClass(sClassName);
			if(c!=null)return c;
		}catch(ClassNotFoundException e){
			
		}finally{
		try {
			
			// 先加载织入类
			if (this.weaverChain != null) {
				weaverChain.reset();
				if (this.weaverChain.hasWeavingType(sClassName)) {
					c = this.weave(sClassName);
				}
			}
			if (c == null) {// 加载芯片类型
				c = super.loadClass(sClassName, bResolve);
			}
		} catch (ClassNotFoundException e) {
			// 管道中搜
			if ((c == null) && (pipeline != null)) {
				c = pipeline.searchReferenceType(sClassName);
			}
		}
		}
		return c;
	}

	@Override
	public boolean isContainsExotericalTypeName(String name) {
		return pipeline.isContainsExotericalTypeName(name);
	}

	//
	// @Override
	// protected URL findResource(String sName) {
	// URL uri = super.findResource(sName);
	// if ((uri == null)&&(policy!=null)) {
	// uri=policy.searchResource(sName);
	// }
	// return uri;
	// }

	@Override
	public void load(String file) {
		resourcefile=file;
		super.loadJar(file);
	}
	public String getResourcefile() {
		return resourcefile;
	}
	@Override
	public void dependency(IResource resource) {
		if (this == resource)
			throw new EcmException(String.format("不能依赖自身:%s",resourcefile));
		if (pipeline == null)
			throw new EcmException("不支持依赖");
		if (this.pipeline.contains(resource))
			throw new EcmException("已依赖了指定的资源");
		pipeline.addReference(resource);
	}

	@Override
	public void undependency(IResource resource) {
		this.pipeline.removeReference(resource);
		// 由于依赖后类型会加载到本地资源中，可以用返射ClassLoader基类和JarClassLoader取出资源的集合，并从中仅移除依赖的资源，解除依赖可留待之后在此添加
	}

	// 由ModuleSite初始化它，因为只有模块站点才知道如何定义其资源的加载顺序
	@Override
	public void setPipeline(IExotericalResourcePipeline pipeline) {
		this.pipeline = pipeline;
	}

	// 此处虽然叫拷贝，实际上是属性的引用，所以在清除对象时容易造成混淆，所以必须深表拷贝
	public void copyTo(SystemResource resource) {
		resource.jdkLoader = this.jdkLoader;
		resource.pipeline = this.pipeline;
		resource.weaverChain = this.weaverChain;
		resource.resourceNames.addAll(this.resourceNames);
		Iterator<String> it = this.serviceTypes.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			resource.serviceTypes.put(key, this.serviceTypes.get(key));
		}
		resource.getLstJarFile().addAll(this.getLstJarFile());
		resource.domain = domain;
	}

}
