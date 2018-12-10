package cj.studio.ecm.graph;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.IServiceSite;

/**
 * 图的站点
 * 
 * <pre>
 * 先在父服务提供器中搜索
 * </pre>
 * 
 * @author carocean
 *
 */
public interface IGraphServiceSite extends IServiceSite {
	/**
	 * 为图设置父服务提供器
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param parent
	 */
	void parent(IServiceProvider parent);

	/**
	 * chip站点
	 * 
	 * <pre>
	 * 由此可访问服务容器
	 * </pre>
	 * 
	 * @param chipSite
	 */
	void chipSite(IServiceSite chipSite);

	/**
	 * <pre>
	 * 支持的功能有： 
	 * 1.命令：支持入站的图的连接及申请。 $.cmdline(gc)>is -n website -o input 
	 * 2.服务对象：
	 * 	1)$.graph.creator 
	 * 	2)$.graph.options 
	 *  3)$.cmdline.console，控制台
	 * 3.可获取ecm服务
	 * </pre>
	 */
	@Override
	public Object getService(String serviceId);

	/**
	 * <pre>
	 * 增加的属性：
	 *  1）$.graph.name 图的名称 
	 *  2）$.graph.processId 进程号 
	 *  3) $.graph.protocol
	 * </pre>
	 */
	@Override
	public String getProperty(String key);
}
