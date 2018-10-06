package cj.studio.ecm.net.layer;

import cj.studio.ecm.frame.Frame;

/**
 * 路由表
 * 
 * <pre>
 * netsite有会话层和路由层两层，路由层在会话层之后。
 * 是终端与平台之间的通路
 * 在平台网络内导向到终端
 * 
 * 自动导向功能：
 * 1.链路从客户端向服务器过程，叫向上，路由表自动追加
 * 2.链路从服务器向客户端过程，叫向上，路由表自动next
 * 3.仔细想想，如果服务器能向客户端发信息，则一定事先存在客户端向服务器的连接，即：不存在客户端没有向服务器报告路由的情况。
 * 所以整个网络，不论是向上向下，每个链路均有路由表。
 * 4.路由表挂在通讯的frame的head上,key=routing-table。
 * 5.路由功能由netsite来实现，因为是netsite的功能，由netsite附加到服务器端上，sink为routingSink
 * 6.从互动站开始报路由，直到hup,路由表的机制为将来添加中间层提供自由空间
 * 
 * 路由表由节构成，每个节包含cjtoken、select-name，cjtoken为必须
 * 路由表头是终端节，尾是hub节，如果hub在lns平台中只有一个，因此此节可省略，即尾节是接入hub的直接驿站station
 * 
 * 说明：
 * 因为只有主从模式下，主想主动发往从的时候才需要路由，客端则不需要路由，它想发谁就发谁。
 * 因此，路由表的作用是让主端一级一能知道往哪发。
 * 在信道层，是通过select-name,select-id知道发往哪里，但这仅是两个节点间的通讯基础，在netsite上加上了路由功能，能实现链路无缝通讯。
 * 
 * <b>注意：</b>
 * 
 *   * 路由表采用上发时经过每个节点添加表项，下发时经过每个节点收缩表项，到初始上发点正好表项用完，
 *   * 这样做的目的是：让终端接收到的侦不再包含服务器上的路由信息，如果让终端拿到这个，那会使服务器网络环境暴露，极大的威肋着网络的安全。
 * </pre>
 * 
 * @author carocean
 *
 */
public interface IRoutingTable {
	AC pop();

	/**
	 * 返回acl
	 * 
	 * <pre>
	 * 头即路由表的初始点，在lns中代表着终端的会话
	 * </pre>
	 * 
	 * @return
	 */
	AC[] acl();

	AC get(int i);
	void append(AC ac);

	void clear();

	boolean isEmpty();

	int count();


	AC find(String nodeName, String cjToken);


	int locate(String nodeName, String cjToken);

	public String toJson();

	void fromJson(String text);

	void setFrame(Frame frame);
}
