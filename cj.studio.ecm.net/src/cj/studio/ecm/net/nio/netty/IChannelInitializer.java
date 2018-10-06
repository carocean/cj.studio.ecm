package cj.studio.ecm.net.nio.netty;

import io.netty.channel.Channel;
import cj.studio.ecm.net.graph.IHandleBinder;
/**
 * 初始化信道
 * <pre>
 * 
 * </pre>
 * @author carocean
 *
 * @param <T>
 */
public interface IChannelInitializer<T extends Channel> {
	/**
	 * 初始化信道
	 * <pre>
	 * 说明：netty每建立一次连接，便会执行一次该方法，
	 * 但http兼容websocket模式会导致一次请求（http请求html页稍后在html页中又发起websocket请求，则会导致两次调用，实际上一次请求建立了两个连接。
	 * 另外，由于http协议是默认情况下都是短连接，每次请求均需建立连接，因此http+websocket模式针对websocket的性能不佳，每次均得建立websocket连接。
	 * 因此推荐独立模式，websocket独自使用，可建立长连接。
	 * 
	 * 每个连接netty会为之分配一个pipeline
	 * 
	 * 注意：netty的开发者仅需要在此方法添加netty的handler和自定义的handler，netgraph相关的handler在该方法被系统调用完后，由系统自动添加到pipeline尾部。
	 * </pre>
	 * @param channel
	 */
	IHandleBinder initChannel(T channel,long idleCheckin)throws Exception;
}
