package cj.studio.ecm.net;

import cj.studio.ecm.net.graph.INetGraph;

public interface IConnectCallback {
	/**
	 * 构建net的图
	 * <pre>
	 *
	 * </pre>
	 * @param owner 图的持有者，可能是客户端也可能是服务器，目前仅有客户端支持异步构建
	 * @param ng
	 */
	void buildGraph(Object owner,INetGraph ng);

}
