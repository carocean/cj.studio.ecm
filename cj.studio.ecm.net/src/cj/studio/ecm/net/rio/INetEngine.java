package cj.studio.ecm.net.rio;

import java.io.IOException;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Map;

import cj.studio.ecm.net.IConnectCallback;
import cj.studio.ecm.net.graph.INetGraph;

public interface INetEngine {

	void setNetProperties(Map<String, String> props);

	/**
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param moniter
	 *            监视者
	 * @param workThreads
	 * @param heartbeatInterval
	 *            心跳间隔时间只有服务器端有效，因为心跳是服务器发心跳由客户端确认模式。<br>
	 *            只有heartbeatInterval>0时服务器和客户端才开启心跳程序检测。
	 * @param m
	 * @param callback
	 * @throws IOException
	 */
	void moniter(Object moniter, AbstractSelectableChannel javach,
			Map<String, String> props, IConnectCallback callback)
			throws IOException;

	void stopMoniter();
	Object owner();

	INetGraph getNetGraph();


	

	String getSimple();


	ChannelNioMoniterPool getChannelNioMoniterPool();

	Map<String, String> getProps();

	String bossThreadCount();

	String workThreadCount();

	long heartbeatInterval();
}
