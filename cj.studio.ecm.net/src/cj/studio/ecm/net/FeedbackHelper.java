package cj.studio.ecm.net;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.frame.IFeedback;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.nio.netty.http.HttpChunkedSink;
import cj.studio.ecm.net.nio.netty.http.SenderFrameHelper;
import cj.studio.ecm.net.nio.netty.tcp.TcpFrameBox;
import cj.studio.ecm.net.web.HttpCircuit;
import cj.studio.ecm.net.web.HttpFrame;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.udt.UdtMessage;

public class FeedbackHelper {

	

	public static void udtServerFeedback(ChannelHandlerContext ctx, Frame frame,
			Circuit circuit) {
		circuit.feedbackSetSource(IFeedback.KEY_OUTPUT_FEEDBACK);
		final ISink end = new ISink() {

			@Override
			public void flow(Frame frame, Circuit circuit, IPlug plug)
					throws CircuitException {
				UdtMessage backmsg = new UdtMessage(frame.toByteBuf());
				ctx.writeAndFlush(backmsg);
			}
		};
		circuit.feedback(IFeedback.KEY_OUTPUT_FEEDBACK)
				.plugSink(IFeedback.KEY_ENDSINK_FEEDBACK, end);

	}

	// 用于块写
	public static void httpServerFeedback(ChannelHandlerContext ctx,
			Frame mainFrame, Circuit mainCircuit) {
		mainCircuit.feedbackSetSource(IFeedback.KEY_OUTPUT_FEEDBACK);
		final ISink end = new HttpChunkedSink();
		IPlug plug = mainCircuit.feedback(IFeedback.KEY_OUTPUT_FEEDBACK)
				.plugSink(IFeedback.KEY_ENDSINK_FEEDBACK, end);
		plug.option("mainCircuit", mainCircuit);
		plug.option("mainFrame", mainFrame);
		plug.option("ctx", ctx);

	}

	public static void tcpServerFeedback(ChannelHandlerContext ctx, Frame frame,
			Circuit circuit) {
		circuit.feedbackSetSource(IFeedback.KEY_OUTPUT_FEEDBACK);
		final ISink end = new ISink() {

			@Override
			public void flow(Frame frame, Circuit circuit, IPlug plug)
					throws CircuitException {
				byte[] bt = frame.toBytes();
				bt = TcpFrameBox.box(bt);
				ByteBuf btbuf = Unpooled.directBuffer();
				btbuf.writeBytes(bt);
				ctx.writeAndFlush(btbuf);
				if (btbuf.refCnt() > 0)
					btbuf.release();
			}
		};
		circuit.feedback(IFeedback.KEY_OUTPUT_FEEDBACK)
				.plugSink(IFeedback.KEY_ENDSINK_FEEDBACK, end);

	}

	public static void localServerFeedback(ChannelHandlerContext ctx,
			Frame frame, Circuit circuit) {
		circuit.feedbackSetSource(IFeedback.KEY_OUTPUT_FEEDBACK);
		final ISink end = new ISink() {

			@Override
			public synchronized void flow(Frame frame, Circuit circuit,
					IPlug plug) throws CircuitException {
				ctx.writeAndFlush(frame);
			}
		};
		circuit.feedback(IFeedback.KEY_OUTPUT_FEEDBACK)
				.plugSink(IFeedback.KEY_ENDSINK_FEEDBACK, end);

	}

	public static void wsServerFeedback(ChannelHandlerContext ctx, HttpFrame ws,
			HttpCircuit circuit) {
		circuit.feedbackSetSource(IFeedback.KEY_OUTPUT_FEEDBACK);
		SenderFrameHelper helper=new SenderFrameHelper();
		final ISink end = new ISink() {

			@Override
			public void flow(Frame frame, Circuit circuit, IPlug plug)
					throws CircuitException {
				helper.sendWsMsg(ctx.channel(), frame);
			}
		};
		circuit.feedback(IFeedback.KEY_OUTPUT_FEEDBACK)
				.plugSink(IFeedback.KEY_ENDSINK_FEEDBACK, end);
	}

}
