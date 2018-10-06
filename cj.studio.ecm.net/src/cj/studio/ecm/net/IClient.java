package cj.studio.ecm.net;

import cj.studio.ecm.net.graph.INetGraph;


/**
 * 客户端，程序集中用于连接其它的远程端
 * <pre>
 * 1.客户端表示与一个远程终端点所有连接,因此它可以看成是对一个远程终端的长连接。
 *   对于远程来说，客户端就是远程的一个终结点，所以调用者并不关心多少连接，用哪个连接收发了信息。
 * 2.因为客户端就是一个连接池(准确来讲，更像个cluster，发送时消息将由算法随机负载到相应信道），
 *   随时可返回可用连接,不论其同是否是短连接，这个连接掉了，则返回其它或自动新建连接。
 * 3.客户端分为受管和非受管，受管使用clientManager管理和连接，
 *   因为可以使用cj服务形式，非受管可直接新建client使用
 * 4.如果connectMaxCount为1,就等同于一个连接信道。
 * 注意：netty拥有一个工作线程池，每次调用连接方法则为之分配一个信道和一个线程，
 *      它不管每次调用传入的是否是同一个address还是不同。因此client实现了同一个address连接池架构.
 * </pre>
 * @author carocean
 *
 */

public interface IClient {
	String status();
	public void setProperty(String key,String value);
	public String getPort();
	public String getHost();
	void close();
	String getProperty(String name);
	String[] enumProp();
	
	/**
	 * 建立连接
	 * <pre>
	 * 指定客户端连接目标
	 * </pre>
	 * @param ip
	 * @param port
	 * @return TODO
	 * @throws InterruptedException
	 */
	Object connect(String ip,String port)throws InterruptedException ;
	/**
	 * 建立连接
	 * <pre>
	 * 指定客户端连接目标
	 * </pre>
	 * @param ip
	 * @param port
	 * @return TODO
	 * @throws InterruptedException
	 */
	Object connect(String ip,String port,IConnectCallback callback)throws InterruptedException ;
	/**
	 * 构建网络通迅图
	 * <pre>
	 * 推荐在启动前建立图的连接。
	 * 如果图已初始化，则执行初始化，否则就返回图
	 * </pre>
	 * @return
	 */
	//虽然客户端拥有一个图，但可以为每个信道建立一个回路。但目前是采用聚合式。
	public INetGraph buildNetGraph();
	/**
	 * 简名：如：udt,udp,tcp,http,websocket
	 * <pre>
	 *
	 * </pre>
	 * @return
	 */
	String simple();
	String netName();
	
}
