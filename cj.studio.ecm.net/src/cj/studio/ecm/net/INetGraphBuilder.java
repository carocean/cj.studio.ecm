package cj.studio.ecm.net;

import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.graph.CircuitException;
/**
 * 组件程序集中的网络通讯有向图
 * <pre>
 * 每个程序集须从此类派生并定义为cj服务。
 * </pre>
 * @author carocean
 *
 */
public  interface INetGraphBuilder {
	/**
	 * 组装网络通讯图
	 * <pre>
	 * 先决条件：程序集内的服务器和客户端必须配置成cj服务方可使用本架构
	 * 
	 * 该方法在程序集启动后且服务器和客户端初始化完方执行
	 * </pre>
	 * @param site 服务站，在此搜索server,client,业务逻辑logicGraph并和netgraph组装
	 */
	 void connectNetGraphs(IServiceSite site) throws CircuitException;
	/**
	 * 释放网络通讯图
	 * <pre>
	 * 先决条件：程序集内的服务器和客户端必须配置成cj服务方可使用本架构
	 * 
	 * 该方法在程序集停止前解组netgraph与用户定义的logicgraph之间的通迅，并释放资源
	 * </pre>
	 * @param site  服务站，在此搜索server,client,业务逻辑logicGraph并和netgraph组装
	 */
	 void disconnectNetGraphs(IServiceSite site);
}
