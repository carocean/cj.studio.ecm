package cj.studio.ecm.context;

import cj.studio.ecm.IAssemblyContext;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.script.IScriptContainer;
import cj.ultimate.IDisposable;

/**
 * 模块上下文，开放给用户使用。
 * @author Administrator
 *
 */
public interface IModuleContext extends IServiceProvider,IDisposable{
	void refresh();
	IServiceSite getSite();
	IAssemblyContext getAssemblyContext();
	void parent(IServiceProvider parent);
	IScriptContainer getScriptContainer();
}
