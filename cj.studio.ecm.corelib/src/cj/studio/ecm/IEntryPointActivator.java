package cj.studio.ecm;

import cj.studio.ecm.context.IElement;

/**
 * 程序集入口点激活器<br>
 * 程序集加载完毕后启动激活器
 * @author Administrator
 *
 */
public interface IEntryPointActivator extends IActivable {
	/**
	 * 激活
	 */
	@Override
	void activate(IServiceSite site,IElement args);
	/**
	 * 去活
	 */
	@Override
	void inactivate(IServiceSite site);
}
