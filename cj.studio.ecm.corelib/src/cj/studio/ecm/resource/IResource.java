package cj.studio.ecm.resource;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import cj.studio.ecm.IExotericalResourcePipeline;
import cj.studio.ecm.weaving.ServiceTypeWeaverChain;
import cj.ultimate.IDisposable;


public interface IResource extends IDisposable{
	public static final String CONTEXT_CONFIG_FILE="cj/properties/Assembly.json";
	public static final String CONTEXT_PROP_FILE="cj/properties/Assembly.properties";
	public static final String REFRECENCE_LIBPATH_FILE="cj/references";
	/**
	 * 此包将优先装载，用于加载一些被系统依赖包，它拦截到父的查找，而先装载它
	 */
	public static final String REFOVERRIDE_LIBPATH_FILE ="cj/refoverrides";
	/**
	 * 内嵌的包路径，内嵌包和芯片使用同一个类加载器，而引用包则被芯片的父级加载器装载。
	 */
	public static final String REFEMBED_LIBPATH_FILE="cj/refembeds";
	 public URL getResource(String name);
	 public InputStream getResourceAsStream(String name);
	 public Enumeration<String> enumResourceNames();
	 Class<?> loadClass(String name)throws ClassNotFoundException;
	 //加载服务类型，用于静态方法的拦截，适配器的织入等
	 void  setWeaverChain(ServiceTypeWeaverChain weaverChain) ;
	 void load(String jarFile);
	 ClassLoader getClassLoader();
	 //使得类加载器可以访问指定的资源的类加载器
	public void dependency(IResource resource);
	public void undependency(IResource resource);
	/**
	 * 上行管理设置，芯片内部可为之增加类搜索链路
	 * <pre>
	 *
	 * </pre>
	 * @param pipeline
	 */
	void setPipeline(IExotericalResourcePipeline pipeline);
	//是否包含开放类型名
	boolean isContainsExotericalTypeName(String name);
	IExotericalResourcePipeline getPipeline();
	 void copyTo(SystemResource resource);
	 String getResourcefile();
}
