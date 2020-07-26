package cj.studio.ecm;

import cj.studio.ecm.context.IElement;
import cj.studio.ecm.resource.IResource;

public interface IAssemblyContext {
	
	public abstract IResource getResource();
	//宜隐藏此对象，对程序集上下文采用强结构方式使得外部调用更为透明，当配置名变化时，引用它的就不必变更
	public abstract IElement getElement();

	public abstract IElement[] getScans();
	public abstract IElement getJss();
	public String switchFilter();
	public String[] enumProperty();
	public String getProperty(String key);
//	String getClassLoaderName();
	/**
	 * 服务容器启动前监视器，返回为监视器实现类
	 * @return
	 */
	String serviceContainerMonitors();
}