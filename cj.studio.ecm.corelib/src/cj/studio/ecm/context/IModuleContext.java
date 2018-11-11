package cj.studio.ecm.context;

import cj.studio.ecm.IAssemblyContext;
import cj.studio.ecm.IServiceDefinitionRegistry;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.script.IScriptContainer;
import cj.ultimate.IDisposable;

/**
 * 模块上下文，开放给用户使用。
 * 
 * @author Administrator
 *
 */
public interface IModuleContext extends IServiceProvider, IDisposable {
	void refresh();
	IServiceSite getDelegateSite() ;
	IServiceProvider getDownSite();

	IAssemblyContext getAssemblyContext();

	IScriptContainer getScriptContainer();
	IServiceDefinitionRegistry getRegistry();
	void parent(IServiceProvider parent);
	IServiceSite getCoreSite();
}
