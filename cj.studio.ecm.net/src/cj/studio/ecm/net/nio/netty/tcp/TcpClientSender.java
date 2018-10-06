package cj.studio.ecm.net.nio.netty.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.nio.IChannelPool;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.nio.TimeoutException;
import cj.ultimate.util.NumberUtil;

/**
 * 绑定物理通道，用于发送信息，并将graph输出的协议转换为物理通道认识的消息。
 * 
 * <pre>
 *
 * </pre>
 * 
 * @author carocean
 *
 */
public class TcpClientSender implements ISink {
	Channel ch;
	IChannelPool pool;
	private void genChannel() throws CircuitException{
		try {
			ch=pool.get();
		} catch (TimeoutException e) {
			throw new CircuitException(NetConstans.STATUS_610,e);
		}
	}
	@Override
	public void flow(Frame frame, Circuit circuit,IPlug plug) throws CircuitException {
		
		Circuit cir=circuit;
		if("setChannel/1.0".equalsIgnoreCase(cir.protocol())){
			pool=(IChannelPool)cir.attribute("channel-pool");
			genChannel();
			return;
		}
		String c = frame.command();
		if ("connect".equals(c))
			return;
		if ("disconnect".equals(c)) {
			ch.disconnect();
			return;
		}
//		frame.head("peer-senderId", frame.head("senderId"));
//		frame.head("peer-senderType", frame.head("senderType"));
//		frame.removeHead("peer-localaddress");
//		frame.removeHead("peer-remoteaddress");
//		frame.head("senderId", ch.id().asShortText());
//		frame.head("senderType", "tcp");
		ByteBuf buf = Unpooled.directBuffer();
		byte b[]=frame.toBytes();
		b=box(b);
		buf.writeBytes(b);
		if(!ch.isActive()||!ch.isOpen()){
			genChannel();
		}
		ch.writeAndFlush(buf);
		frame.dispose();
	}
	private byte[] box(byte[] b){
		int len=b.length;
		byte[] res=new byte[len+4];
		byte[] head=NumberUtil.intToByte4(len);
		System.arraycopy(head, 0, res, 0, 4);
		System.arraycopy(b, 0, res, 4, len);
		return res;
	}
}
