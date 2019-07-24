package cj.studio.ecm.context;

import cj.studio.ecm.IServiceSite;
/**
 * 监视服务容器的刷新事件
 * @author caroceanjofers
 *
 */
public interface IServiceContainerMonitor {
	/**
	 * 在服务容器刷新前
	 * @param site
	 */
	void onBeforeRefresh(IServiceSite site);
	/**
	 * 在服务容器刷新后
	 * @param site
	 */
	void onAfterRefresh(IServiceSite site);

}
