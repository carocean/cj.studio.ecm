package cj.studio.ecm.net.nio.netty.udt;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.udt.UdtMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.nio.NetConstans;

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
public class UdtServerSender implements ISink {
//	DefaultChannelGroup channels;

	 Map<String, Channel> channels;

	public UdtServerSender() {
//		channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
		 channels = new ConcurrentHashMap<String, Channel>(4);
	}

	@Override
	public void flow(Frame frame, Circuit circuit,IPlug plug) throws CircuitException {
		if ("NET/1.1".equals(frame.protocol())) {
			if ("connect".equals(frame.command())) {
				if ("senderIn".equals(plug.owner())) {
					ChannelHandlerContext ctx = (ChannelHandlerContext) circuit.attribute("ctx");
					channels.put(ctx.channel().id().asShortText(),ctx.channel());
				} else {
					throw NetConstans.newCircuitException(NetConstans.STATUS_613);
				}
				return;
			}
			if (frame.command().equals("disconnect")) {
				if ("senderIn".equals(plug.owner())) {
					ChannelHandlerContext ctx = (ChannelHandlerContext) circuit.attribute("ctx");
					channels.remove(ctx.channel().id().asShortText());
				} else {// 调用者请求关闭连接
					String sid=(String)circuit.attribute("select-id");
					Channel ch = channels.get(sid);
					if(ch==null){
						circuit.status("404");
						circuit.message("要关闭的连接不存在。");
						return;
					}
					ch.close();
					channels.remove(sid);
				}
				return;
			}
			return;
		}
		// 其下是客户主动向客户端发送数据侦

		String id=(String)circuit.attribute("select-id");
		Channel ch = channels.get(id);
		if (ch == null) {
			throw NetConstans.newCircuitException(NetConstans.STATUS_602, circuit.attribute("select-id"));
		}
		// if ("udt".equals(circuit.head("select-simple"))) {
		frame.head("status", circuit.head("status"));
		frame.head("message", circuit.message());
		frame.head("cause", circuit.cause());
		UdtMessage newFrame =null;
		newFrame = new UdtMessage(frame.toByteBuf());
		ch.writeAndFlush(newFrame);
		// } else {
		// throw new CircuitException("504", "httpServerSender不支持的协议。"
		// + frame.protocol());
		// }
	}
	
}
