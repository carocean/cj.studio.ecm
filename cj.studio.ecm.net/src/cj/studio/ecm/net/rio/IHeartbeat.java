package cj.studio.ecm.net.rio;

import java.nio.channels.SocketChannel;

import cj.ultimate.IDisposable;

public interface IHeartbeat extends IDisposable{
	void visitChannel(SocketChannel socketChannel);
	/**
	 * 发送心跳侦间隔时间
	 * <pre>
	 * 如果小于等于0表示不支持心跳，但仅限于服务器端有此限制。客户端自适配，因此心跳算法是由服务器发起的。
	 * 客户端只要收到sb心跳包就处理，未收到就不处理。
	 * </pre>
	 * @param timeSpliter
	 */
	void moniter(long timeSpliter);
	void setChannel(SocketChannel ch);
	void event(IHeartbeatEvent e);
	@Override
	public void dispose();
	void ackFrame(SocketChannel ch, String frameId);
	IWorker getAckHeartbeatCBFrameWork();
	IWorker getAckHeartbeatSBFrameWork();
}
