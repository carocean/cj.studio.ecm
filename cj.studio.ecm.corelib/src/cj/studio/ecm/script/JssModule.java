package cj.studio.ecm.script;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.log4j.Logger;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.Scope;
import cj.studio.ecm.resource.IResource;
import cj.studio.ecm.resource.JarClassLoader;
import cj.ultimate.util.FileHelper;
import cj.ultimate.util.StringUtil;
import cj.ultimate.util.UnzipUtil;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

class JssModule implements IJssModule {
	String ext;
	String pack;
	boolean unzip;
	String name;
	private String searchMode;
	private String linkHome;
	private boolean unziped;
	IResource resource;
	boolean isWebModule;
	Logger log = Logger.getLogger(JssModule.class);
	String jssmodruntimeHome;
	private IServiceProvider serviceProvider;
	static String headkey = "$.head";

	public JssModule(IServiceProvider container,IResource resource, String name, String ext, String pack,
			boolean unzip, String searchMode) {
		this.resource = resource;
		this.serviceProvider=container;
		this.ext = ext;
		this.pack = pack;
		this.unzip = unzip;
		this.name = name;
		this.searchMode = searchMode;
	}

	public JssModule() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void setUnziped(boolean unziped) {
		// TODO Auto-generated method stub
		this.unziped = unziped;
	}

	@Override
	public boolean isWebModule() {
		// TODO Auto-generated method stub
		return isWebModule;
	}
	@Override
	public void serviceProvider(IServiceProvider parent) {
		// TODO Auto-generated method stub
		this.serviceProvider=parent;
	}
	@Override
	public void resource(IResource res) {
		// TODO Auto-generated method stub
		resource = res;
	}

	@Override
	public String moduleHome() {
		// TODO Auto-generated method stub
		return linkHome;
	}

	@Override
	public void chipHome(String chipHome) {
		if (FIXED_MODULENAME_HTTP_JSS.equals(name)
				|| FIXED_MODULENAME_WS_JSS.equals(name)) {
			linkHome = String.format("%s/%s/%s", chipHome, RUNTIME_SITE_DIR,
					pack.replace(".", File.separator));
			linkHome = linkHome.replace("/", File.separator).replace("//",
					File.separator);
			return;
		}
		if (StringUtil.isEmpty(jssmodruntimeHome)) {
			linkHome = String.format("%s/%s/%s/%s", chipHome, RUNTIME_JSS_DIR,
					name, pack.replace(".", File.separator));
		} else {
			linkHome = String.format("%s/%s/%s", chipHome, jssmodruntimeHome,
					pack.replace(".", File.separator));
		}

		linkHome = linkHome.replace("/", File.separator).replace("//",
				File.separator);
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return name;
	}

	@Override
	public String getExtName() {
		// TODO Auto-generated method stub
		return ext;
	}

	@Override
	public String pack() {
		// TODO Auto-generated method stub
		return pack;
	}

	@Override
	public boolean unzip() {
		// TODO Auto-generated method stub
		return unzip;
	}

	@Override
	public String searchMode() {
		return searchMode;
	}

	public void searchMode(String searchMode) {
		this.searchMode = searchMode;
	}

	@Override
	public boolean isUnziped() {
		return unziped;
	}

	@Override
	public void doUnzip() {
		if ("inner".equals(searchMode) && unzip)
			log.info(String.format("模块%s的searchMode=inner，虽然配置为解压，但外部jss无效。",
					name));
		File f = new File(linkHome);
		if (f.exists()) {
			// boolean s = FileHelper.deleteDir(f);
			// log.info(String.format("清除jss模块目录%s.\r\n\t位置：%s。", s ? "成功" :
			// "失败",
			// f));
			f.mkdir();
		}
		try {
			UnzipUtil.unzip(resource.getResourcefile(), pack,
					f.getAbsolutePath());
			log.info(String.format("解压到jss模块目录,\r\n\t位置：%s。", f));
			unziped = true;
		} catch (IOException e) {
			throw new EcmException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object loadJss(IJssDefinition def,
			IScriptContainer container) {
		InputStream in = null;
		try {
			String moduleHome = "";
			switch (searchMode) {
			case SEARCHMODE_BOTH:
			case SEARCHMODE_LINK:
				if (unzip) {
					String jssfile = String.format("%s/%s", linkHome,
							def.location(ext));
					try {
						in = new FileInputStream(
								jssfile.replace("/", File.separator));
					} catch (FileNotFoundException e) {
						return null;
					}
					moduleHome = linkHome;
				} else {
					String jssRealName = String.format("%s.%s", pack,
							def.relateName());
					jssRealName = String.format("%s%s",
							jssRealName.replace(".", "/"), ext);
					in = resource.getResourceAsStream(jssRealName);
					if (in == null)
						return null;
					moduleHome = String.format("%s/!jar/%s",
							resource.getResourcefile(),
							pack().replace(".", "/"));
					log.info(String.format(
							"模块%s的searchMode=link，但unzip=false，因此启用inner模式查询资源",
							name));
				}
				break;
			case SEARCHMODE_INNER:
				String jssRealName = String.format("%s.%s", pack,
						def.relateName());
				jssRealName = String.format("%s%s",
						jssRealName.replace(".", "/"), ext);
				in = resource.getResourceAsStream(jssRealName);
				if (in == null)
					return null;
				moduleHome = String.format("%s/!jar/%s",
						resource.getResourcefile(), pack().replace(".", "/"));
				break;
			}
			byte[] b = FileHelper.readFully(in);
			String userJssCode = new String(b);
			Bindings importsDomain = new SimpleBindings();
			def.importsDomain(importsDomain);
			// 输入域，如果想共享关键看imports被哪些eval绑定
			/* 注意importsDomain：
			 * 缺陷：java8 nashorn 在jdk8 65u之后的版本存在缺陷（测到91,92,102u)，65u正常
			描述：绑定域可见性缺陷，比如一个jss服务的imports域(importsDomain绑定），在非函数代码中可以打印出来，在函数（如：exports.test=function())代码段内却报imports未定义异常。
			ScriptContext.ENGINE_SCOPE
			老外分析：jdk8 102版也存在此问题，95版也存在
			http://stackoverflow.com/questions/37611959/java-8-passing-a-function-through-bindings
			 */
			HashMap<String, Object> impMap = new HashMap<String, Object>(4);
			importsDomain.put("imports", impMap);
			impMap.put("module_name", this.name());
			impMap.put("module_home", moduleHome);
			impMap.put("module_extName", this.getExtName());
			impMap.put("module_unzip", this.unzip());
			impMap.put("module_package", this.pack());
			String moduleType = "logic";
			if (name.equals(FIXED_MODULENAME_HTTP_JSS))
				moduleType = "http.jss";
			if (name.equals(FIXED_MODULENAME_WS_JSS))
				moduleType = "ws.jss";
			impMap.put("module_type", moduleType);
			// 以下是当前jss服务参数
			impMap.put("source",
					"both".equals(searchMode) ? "link" : searchMode);// searchMode
			impMap.put("locaction", def.location(ext));
			impMap.put("proxy", null);// 如果设置了过滤器，则java服务被拦截时在此传递代理对象给它。

			impMap.put("selectKey1", def.selectName());// 对象在脚本引擎中的查询表达式
			impMap.put("selectKey2", def.selectScriptName());// 脚本在服务容器中的查询键
			Map<String, String> objectProps = new HashMap<String, String>();// 以exports对象属性。

			String bodycode = container.loadJssHead(name, userJssCode,
					def.relateName(), importsDomain, objectProps);

			ScriptObjectMirror head = container.JssObjectDefine(name,
					def.relateName(), importsDomain);
			impMap.put("head", head);
			def.setHead(head);

			container.eval(bodycode, importsDomain);

			ScriptObjectMirror m = container.jssObject(name, def.relateName(),
					importsDomain);
			if (m == null) {
				throw new EcmException(
						String.format("服务对象初始化:%s 失败，为空。%s", def.selectName()));
			}

			if (m.containsKey(headkey)) {
				throw new EcmException(String.format(
						"服务对象:%s 初始化失败，head是系统保留关键字。error: exports.$.head",
						def.selectName()));
			}
			m.put(headkey, head);// 将jss服务的头赋给对象
			ScriptObjectMirror services = (ScriptObjectMirror) head.get("services");//jss的属性对象，它用于注入服务
			if(services!=null){
				Set<Entry<String, Object>> set=services.entrySet();
				for(Entry<String, Object> en:set){
					Object v=en.getValue();
					if(v instanceof ScriptObjectMirror){
						throw new EcmException("jss头中的services对象属性的值不能是js对象，它被限定为服务名");
					}
					String serviceId=(String)v;
					if(StringUtil.isEmpty(serviceId)){
						serviceId=en.getKey();
					}
					Object obj=serviceProvider.getService(serviceId);
					services.put(en.getKey(), obj);
				}
			}
			ScriptObjectMirror jsshead = (ScriptObjectMirror) head.get("jss");
			if (jsshead != null) {
				Object isStronglyJss = jsshead.get("isStronglyJss");
				if (isStronglyJss != null && ("true".equals(isStronglyJss)
						|| isStronglyJss.equals(true))) {
					String extendclass=(String)jsshead.get("extends");
					if(StringUtil.isEmpty(extendclass)){
						throw new EcmException(String.format(
								"jss服务对象:%s 初始化失败，指定了强jss类型，但head.jss.extends实现的接口未指定。",
								def.selectName()));
					}
					
					Class<Object> clazz=null;
					try {
						clazz = (Class<Object>) container.classloader().loadClass(extendclass);
						if(clazz==null){
							throw new ClassNotFoundException(extendclass);
						}
					} catch (ClassNotFoundException e) {
						throw new EcmException(String.format(
								"jss服务对象:%s 初始化失败，head.jss.extends接口类型: %s 不存在。",
								def.selectName(),extendclass));
					}
					if (!clazz.isInterface())
						throw new EcmException(String.format("不是接口。%s", extendclass));
					return container.getInterface(m, clazz);
				}
			}
			return m;
		} catch (IOException e) {
			throw new EcmException(e);
		} catch (ScriptException e) {
			throw new EcmException(e);
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public ScriptObjectMirror newJssInstance(IJssDefinition def,
			IScriptContainer container) {
		ScriptObjectMirror m;
		try {
			m = container.eval(def.selectScriptName(), def.importsDomain());
			if (m == null)
				return m;
			return m;
		} catch (ScriptException e) {
			log.error(e);
			throw new EcmException(e);
		}

	}

	@Override
	public Object[] loadJss(String relateName, IScriptContainer container,
			IJssModuleCallback cb, boolean forceLoad) {
		String jssFile = String
				.format("%s/%s%s", linkHome, relateName.replace(".", "/"), ext)
				.replace("/", File.separator);
		String selectName = String.format("%s.%s", this.moduleSelectName(),
				relateName);
		InputStream in = null;

		try {
			String moduleHome = "";
			switch (searchMode) {
			case SEARCHMODE_BOTH:
			case SEARCHMODE_LINK:
				if (unzip) {
					try {
						in = new FileInputStream(jssFile);
					} catch (FileNotFoundException e) {
						return new Object[0];
					}
					moduleHome = linkHome;
				} else {
					String jssRealName = String.format("%s.%s", pack,
							relateName);
					jssRealName = String.format("%s%s",
							jssRealName.replace(".", "/"), ext);
					in = resource.getResourceAsStream(jssRealName);
					if (in == null)
						return new Object[0];
					moduleHome = String.format("%s/!jar/%s",
							resource.getResourcefile(),
							pack().replace(".", "/"));
					log.info(String.format(
							"模块%s的searchMode=link，但unzip=false，因此启用inner模式查询资源",
							name));
				}
				break;
			case SEARCHMODE_INNER:
				String jssRealName = String.format("%s.%s", pack, relateName);
				jssRealName = String.format("%s%s",
						jssRealName.replace(".", "/"), ext);
				in = resource.getResourceAsStream(jssRealName);
				if (in == null)
					return new Object[0];
				moduleHome = String.format("%s/!jar/%s",
						resource.getResourcefile(), pack().replace(".", "/"));
				break;
			}
			JssServiceDefinition def = new JssServiceDefinition();
			byte[] b = FileHelper.readFully(in);
			String userJssCode = new String(b);
			Bindings importsDomain = new SimpleBindings();
			def.importsDomain(importsDomain);
			// 输入域，如果想共享关键看imports被哪些eval绑定
			/* 注意importsDomain：
			 * 缺陷：java8 nashorn 在jdk8 65u之后的版本存在缺陷（测到91,92,102u)，65u正常
			描述：绑定域可见性缺陷，比如一个jss服务的imports域(importsDomain绑定），在非函数代码中可以打印出来，在函数（如：exports.test=function())代码段内却报imports未定义异常。
			ScriptContext.ENGINE_SCOPE
			老外分析：jdk8 102版也存在此问题，95版也存在
			http://stackoverflow.com/questions/37611959/java-8-passing-a-function-through-bindings
			 */
			HashMap<String, Object> impMap = new HashMap<String, Object>(4);
			importsDomain.put("imports", impMap);
			impMap.put("module_name", this.name());
			impMap.put("module_home", moduleHome);
			impMap.put("module_extName", this.getExtName());
			impMap.put("module_unzip", this.unzip());
			impMap.put("module_package", this.pack());
			String moduleType = "logic";
			if (name.equals(FIXED_MODULENAME_HTTP_JSS))
				moduleType = "http.jss";
			if (name.equals(FIXED_MODULENAME_WS_JSS))
				moduleType = "ws.jss";
			impMap.put("module_type", moduleType);
			// 以下是当前jss服务参数
			impMap.put("source",
					"both".equals(searchMode) ? "link" : searchMode);// searchMode
			String location = String.format("%s%s",
					relateName.replace(".", File.separator), ext);
			impMap.put("locaction", location);
			impMap.put("proxy", null);// 如果设置了过滤器，则java服务被拦截时在此传递代理对象给它。

			impMap.put("selectKey1", selectName);// 对象在脚本引擎中的查询表达式
			impMap.put("selectKey2",
					String.format("%s['%s']", moduleSelectName(), relateName));// 脚本在服务容器中的查询键

			def.importsDomain(importsDomain);
			def.ownerModule = name;
			def.relateName = relateName;
			def.source = searchMode.equals("both") ? "link" : searchMode;

			Map<String, String> objectProps = new HashMap<String, String>();// 以exports对象属性。

			String bodycode = container.loadJssHead(name, userJssCode,
					def.relateName(), importsDomain, objectProps);

			ScriptObjectMirror head = container.JssObjectDefine(name,
					def.relateName(), importsDomain);
			impMap.put("head", head);
			if (head != null)
				def.setHead(head);
			boolean isLoadBody = forceLoad ? forceLoad
					: def.getDecriber() != null
							&& (def.getDecriber().scope() == Scope.singleon||def.getDecriber().scope() == Scope.multiton);
			if (isLoadBody) {
				container.eval(bodycode, importsDomain);

				ScriptObjectMirror m = container.jssObject(name,
						def.relateName(), importsDomain);
				if (m == null) {
					throw new EcmException(String.format("服务对象:%s 初始化失败，为空。",
							def.selectName()));
				}
				if (m.containsKey(headkey)) {
					throw new EcmException(String.format(
							"服务对象:%s 初始化失败，head是系统保留关键字。error: exports.$.head",
							def.selectName()));
				}
				m.put(headkey, head);// 将jss服务的头赋给对象
				ScriptObjectMirror services = (ScriptObjectMirror) head.get("services");//jss的属性对象，它用于注入服务
				if(services!=null){
					Set<Entry<String, Object>> set=services.entrySet();
					for(Entry<String, Object> en:set){
						Object v=en.getValue();
						if(v instanceof ScriptObjectMirror){
							throw new EcmException("jss头中的services对象属性的值不能是js对象，它被限定为服务名");
						}
						String serviceId=(String)v;
						if(StringUtil.isEmpty(serviceId)){
							serviceId=en.getKey();
						}
						Object obj=serviceProvider.getService(serviceId);
						services.put(en.getKey(), obj);
					}
				}
				if (cb != null) {
					cb.newJss(def, m);
				}
				return new Object[] { def, m };
			} else {
				if (cb != null) {
					cb.newJss(def, null);
				}
				return new Object[0];
			}
		} catch (IOException e) {
			throw new EcmException(e);
		} catch (ScriptException e) {
			log.error(String.format("jjs服务%s加载失败，检查脚本是否正确。error:%s", selectName,
					e.getMessage()));
			throw new EcmException(e);
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
			}
		}

	}

	@Override
	public Object moduleScriptObj(IScriptContainer container) {
		Bindings b = new SimpleBindings();
		try {
			return container.jssModule(name, b);
		} catch (ScriptException e) {
			throw new EcmException(e);
		}
	}

	@Override
	public void scans(IScriptContainer container, IJssModuleCallback cb) {
		switch (searchMode) {
		case SEARCHMODE_BOTH:
		case SEARCHMODE_LINK:
			scanDir(new File(linkHome), container, cb);
			break;
		case SEARCHMODE_INNER:
			scanJar(container, cb);
			break;
		}

	}

	// cj/studio/cc;cj/studio/ccc/ddm.jss.js,cj/studio/ccc.jss.js
	private boolean isParentPackage(String parentdir, String itemPath) {
		String name = parentdir.contains("/") ? parentdir.substring(
				parentdir.lastIndexOf("/"), parentdir.length()) : parentdir;
		boolean contains = itemPath.contains(name + "/") ? true : false;
		if (contains && itemPath.startsWith(parentdir))
			return true;
		return false;
	}

	protected void scanJar(IScriptContainer container, IJssModuleCallback cb) {
		JarClassLoader cl = (JarClassLoader) resource.getClassLoader();
		for (JarFile jfile : cl.getLstJarFile()) {
			Enumeration<JarEntry> en = jfile.entries();
			while (en.hasMoreElements()) {
				JarEntry je = en.nextElement();
				if (je.isDirectory()) {
					continue;
				}
				if (isParentPackage(pack, je.getName())
						&& je.getName().endsWith(getExtName())) {
					String relateName = je.getName().replace(pack, "")
							.replace("/", ".");
					relateName = relateName.startsWith(".")
							? relateName.substring(1, relateName.length())
							: relateName;
					relateName = relateName.replace(ext, "");
					loadJss(relateName, container, cb, false);
				}

			}
		}
	}

	protected void scanDir(File location, IScriptContainer container,
			IJssModuleCallback cb) {
		File[] files = location.listFiles(new FileFilter() {

			@Override
			public boolean accept(File f) {

				return f.getName().endsWith(ext) || f.isDirectory();
			}
		});
		if (files == null || files.length < 1)
			return;
		for (File f : files) {
			if (f.isDirectory()) {
				scanDir(f, container, cb);
				continue;
			}
			String relateName = f.getAbsolutePath().replace(linkHome, "")
					.replace(File.separator, ".");
			relateName = relateName.startsWith(".")
					? relateName.substring(1, relateName.length()) : relateName;
			relateName = relateName.replace(ext, "");
			loadJss(relateName, container, cb, false);
		}
	}
}