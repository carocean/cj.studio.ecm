package cj.studio.ecm.net.nio.netty.local;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.HashMap;
import java.util.Map;

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
public class LocalServerSender implements ISink {
	// DefaultChannelGroup channels;
		Map<String, Channel> channels;

		public LocalServerSender() {
			// channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
			channels = new HashMap<String, Channel>();
		}

		@Override
		public void flow(Frame frame,Circuit circuit, IPlug plug) throws CircuitException {
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
			String id=(String)circuit.attribute("select-id");
			Channel ch = channels.get(id);
			if (ch == null) {
				throw NetConstans.newCircuitException(NetConstans.STATUS_602, circuit.attribute("select-id"));
			}
//			if (circuit.head("select-simple").equals("local")) {
			frame.head("status",circuit.head("status"));
			frame.head("message",circuit.message());
			frame.head("cause",circuit.cause());
				ch.writeAndFlush(frame.copy());
				frame.dispose();
//			} else {
//				throw new CircuitException("504", "httpServerSender不支持的协议。"
//						+ frame.protocol());
//			}
		}

}
