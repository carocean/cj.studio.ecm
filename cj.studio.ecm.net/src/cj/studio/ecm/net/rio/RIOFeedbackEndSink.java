package cj.studio.ecm.net.rio;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.nio.NetConstans;

public class RIOFeedbackEndSink extends SyncCheckSink implements ISink {

	private IChannelContext ctx;

	public RIOFeedbackEndSink(IChannelContext ctx) {
		this.ctx=ctx;
	}

	@Override
	public void flow(Frame frame,Circuit circuit, IPlug plug) throws CircuitException {
		if ("close".equals(frame.command())
				&& "FEEDBACK/1.0".equals(frame.protocol())) {
			try {
				ctx.getChannel().close();
			} catch (IOException e) {
			}
			return;
		}
		Circuit c = circuit;
		Exception error=null;
		try {
			super.flow(frame, circuit,plug);//发远端发送。
		} catch (Exception e) {
			error=e;
		}
//		if (c.isPiggybacking()) {//主动回发机制不充许捎带，40行执行了发送，而发送之后等待的是确认侦，而远程不是发送请求，而是确认侦
//			Frame back = c.snapshot();
//			byte[] b = back.toBytes();
//			ByteBuffer bb = ByteBuffer.wrap(b);
//			try {
//				while(bb.hasRemaining()){
//				ch.write(bb);
//				}
//			} catch (IOException e) {
//				throw new CircuitException("503", e);
//			}
//		}
		//执行完发送之后如果包含了关闭动作则关闭。
		if ("disconnect".equals(c.attribute("net-action"))) {
			try {
				ctx.getChannel().close();
			} catch (IOException e) {
			}
		}
		if(error!=null){
			if(error instanceof CircuitException){
				CircuitException ce=(CircuitException)error;
				throw ce;
			}else{
				throw new CircuitException(NetConstans.STATUS_603, error);
			}
		}
	}
}