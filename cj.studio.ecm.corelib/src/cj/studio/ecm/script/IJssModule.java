package cj.studio.ecm.script;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.resource.IResource;


public interface IJssModule {
	String RUNTIME_JSS_DIR="/work/modules";
	String RUNTIME_SITE_DIR="/work/";
	String $_CJ_JSS_DOT="$.cj.jss.";
	public static final String FIXED_MODULENAME_HTTP_JSS = "http";
	public static final String FIXED_MODULENAME_WS_JSS = "ws";
	final String SEARCHMODE_LINK="link";
	final String SEARCHMODE_INNER="inner";
	final String SEARCHMODE_BOTH="both";
	String name();
	String getExtName();
	String pack();
	String searchMode();
	boolean unzip();
	String moduleHome();
	boolean isWebModule();
	void setUnziped(boolean unziped);
	void chipHome(String chipHome);
	static IJssModule create(IServiceProvider container,IResource resource,String name,String ext,String pack,boolean unzip,String searchMode,String chipHome){
		JssModule def=	new JssModule(container,resource,name,ext,pack,unzip,searchMode);
		def.chipHome(chipHome);
		return def;
	}
	static IJssModule create(IServiceProvider container,IResource resource,String name,String ext,String pack,boolean unzip,String searchMode,String chipHome,boolean isWebModule){
		JssModule def=	new JssModule(container,resource,name,ext,pack,unzip,searchMode);
		def.isWebModule=isWebModule;
		def.chipHome(chipHome);
		return def;
	}
	void resource(IResource res);
	/**
	 * $.cj.jss.moduleName
	 * <pre>
	 *
	 * </pre>
	 * @return
	 */
	default String moduleSelectName(){
		return String.format("%s%s",$_CJ_JSS_DOT, name());
	}
	boolean isUnziped();
	void doUnzip();
	/**
	 * 根据jss服务定义装载jss文件，该方法会导致更新定义，如果jjs文件头被修改的话。
	 * <pre>
	 *
	 * </pre>
	 * @param jssDef
	 * @param container
	 * @return
	 */
	Object loadJss(IJssDefinition jssDef, IScriptContainer container);
	/**
	 * 产生一个新实例，不加载脚本文件，而是根据在引擎内的js对象新建。
	 * <pre>
	 *
	 * </pre>
	 * @param jssDef
	 * @param container
	 * @return
	 */
	Object newJssInstance(IJssDefinition jssDef,
			IScriptContainer container);
	/**
	 * 根据请求名加载jss服务
	 * <pre>
	 *
	 * </pre>
	 * @param relateName 相对jss服务名，如：$.cj.jss.moduleName.test.Test,其中的test.Test即是相对于moduleName的相对名。
	 * @param container
	 * @param cb 加载的服务头和服务实例通知。
	 * @param forceLoad 强制加载服务体，如果非表示只加载声明为单例的实例
	 * @return
	 */
	Object[] loadJss(String relateName, IScriptContainer container,
			IJssModuleCallback cb,boolean forceLoad);
	/**
	 * 抛描模块
	 * <pre>
	 *
	 * </pre>
	 * @param container
	 * @param cb加载的服务头和服务实例通知。
	 */
	void scans(IScriptContainer container, IJssModuleCallback cb);
	Object moduleScriptObj(IScriptContainer container);
	void serviceProvider(IServiceProvider parent);
}
