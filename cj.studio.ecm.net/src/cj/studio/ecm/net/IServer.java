package cj.studio.ecm.net;

import cj.studio.ecm.net.graph.INetGraph;


/**
 * 服务器
 * <pre>
 *	即支持定义为受管理的服务器，也可在代码中直接使用
 * </pre>
 * @author carocean
 *
 */
public interface IServer{
	String netName();
	/**
	 * 简名：如：udt,udp,tcp,http,websocket
	 * <pre>
	 *
	 * </pre>
	 * @return
	 */
	String simple();
	void start(String port);
	void start(String inetHost,String port);
	void stop();
	String getPort();
	void removeProperty(String propName);
	void setProperty(String propName,String value);
	String getProperty(String name);
	String[] enumProp();
	/**
	 * 构建网络通迅图
	 * <pre>
	 * 推荐在启动前建立图的连接。
	 * 如果图已初始化，则执行初始化，否则就返回图
	 * </pre>
	 * @return
	 */
	INetGraph buildNetGraph();
	void eventNetGraphInited(INetGraphInitedEvent event);
	void eventNetGraphStoped(INetGraphStopedEvent event);
	String getINetHost();
}
