package cj.studio.ecm.net.nio.netty.udt;

import io.netty.bootstrap.ServerChannelFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.UdtMessage;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;

import java.util.concurrent.ThreadFactory;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.AnnotationProtocolFactory;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.GraphCreator;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IProtocolFactory;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.FeedbackHelper;
import cj.studio.ecm.net.graph.IHandleBinder;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.nio.netty.IChannelInitializer;
import cj.studio.ecm.net.nio.netty.INettyGraph;
import cj.studio.ecm.net.nio.netty.NettyServer;
import cj.ultimate.util.StringUtil;

/**
 * 本类默认支持消息报文，如果想要二进制报文，覆盖三个方法：createBossGroup，createWorkGroup，
 * getServerChannelFactory实现报文传输即可<br>
 * 二进制报文才使用此解码器DelimiterFrameCodec2());
 * 
 * @author carocean
 *
 */
public class UdtNettyServer extends NettyServer<UdtChannel> {

	public UdtNettyServer() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String simple() {
		// TODO Auto-generated method stub
		return "udt";
	}

	@Override
	protected GraphCreator createNetGraphCreator() {
		return new GraphCreator() {
			@Override
			protected IProtocolFactory newProtocol() {
				// TODO Auto-generated method stub
				return AnnotationProtocolFactory.factory(NetConstans.class);
			}

			@Override
			public ISink createSink(String sink) {
				if ("acceptor".equals(sink)) {
					return new UdtServerAcceptor();
				}
				if ("sender".equals(sink)) {
					return new UdtServerSender();
				}
				if ("diverter".equals(sink)) {
					return new UdtServerDiverter();
				}
				return null;
			}
		};
	}

	@Override
	protected NioEventLoopGroup createBossGroup(String threadCount) {
		ThreadFactory connectFactory = new UtilThreadFactory("accept");
		return new NioEventLoopGroup(Integer.valueOf(threadCount),
				connectFactory, NioUdtProvider.MESSAGE_PROVIDER);
	}

	@Override
	protected NioEventLoopGroup createWorkGroup(String threadCount) {
		ThreadFactory acceptFactory = new UtilThreadFactory("connect");
		return new NioEventLoopGroup(Integer.valueOf(threadCount),
				acceptFactory, NioUdtProvider.MESSAGE_PROVIDER);
	}

	@Override
	protected IChannelInitializer<UdtChannel> createChildChannel() {
		return new NioServerInitializer<UdtChannel>();
	}

	@Override
	protected Class<? extends ServerChannel> getParentChannelClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ServerChannelFactory<? extends ServerChannel> getServerChannelFactory() {
		return NioUdtProvider.MESSAGE_ACCEPTOR;
	}

	class NioServerInitializer<T extends UdtChannel> implements
			IChannelInitializer<T> {

		@Override
		public IHandleBinder initChannel(T ch, long idleCheckin)
				throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			if ("true".equals(props.get("log")))
				pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
			// pipeline.addLast("framer", new
			// DelimiterFrameCodec2());//如果是二进制报文才使用此解码器
			IHandleBinder hb = new NioServerHandler();
			pipeline.addLast("handler", hb);
			return hb;
		}
	}

	class NioServerHandler extends ChannelHandlerAdapter implements
			IHandleBinder ,NetConstans{
		IPin acceptpin;
		IPin senderIn;

		@Override
		public void init(INettyGraph g) {
			this.acceptpin = g.newAcceptorPin();
			this.senderIn = g.in("senderIn");
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg)
				throws Exception {
			UdtMessage umsg = (UdtMessage) msg;
			Frame frame = null;
			Circuit circuit = null;
			try {
				byte[] b = new byte[umsg.content().readableBytes()];
				umsg.content().readBytes(b);
				frame = new Frame(b);
				circuit = new Circuit(frame.protocol() + " 200 ok");
				initCircuit(circuit, ctx);
				// umsg.release();
				String frameId=frame.head(NetConstans.FRAME_HEADKEY_FRAME_ID);
				if (frame.containsHead(NetConstans.FRAME_HEADKEY_FRAME_ID)) {
					//用于通知下游的net对该侦采用同步模式发送。如果开发者在链路中去除了该属性，则源头的调用者将等待超时。因为后续没有同步返回结果。
					frame.head(FRAME_HEADKEY_CIRCUIT_SYNC,"true");
					circuit.piggybacking(true);
					AttributeKey<String> key = AttributeKey
							.valueOf(NetConstans.FRAME_HEADKEY_FRAME_ID);
					ctx.attr(key).set(frameId);
					//侦的编号对开发者不可见。
					frame.removeHead(FRAME_HEADKEY_FRAME_ID);
				}
				FeedbackHelper.udtServerFeedback(ctx,frame,circuit);//主动无限制次数回馈机制实现。
				acceptpin.flow(frame, circuit);

				if (umsg.refCnt() > 0)
					umsg.release();
				if (circuit.isPiggybacking()) {// 捎带信息
					Frame back = null;
					boolean isunwrap=false;
					if (circuit.containsContentType()) {
						String v = circuit.contentType();
						if ("frame/bin".equals(v) || "frame/json".equals(v)) {
							isunwrap=true;
						}
					}
					if(isunwrap){
						back = new Frame(circuit.content().readFully());
					}else{
						back = circuit.snapshot("piggyback / net/1.1");
					}
					back.head(NetConstans.FRAME_HEADKEY_FRAME_ID, frameId);
					UdtMessage backmsg = new UdtMessage(back.toByteBuf());
					ctx.writeAndFlush(backmsg);
					if ("disconnect".equals(circuit.attribute("net-action"))) {
						ctx.close();
					}
				}
				frame.dispose();
			} catch (Exception e) {
				CircuitException.print(e, log);
				throw new EcmException(e);
			}
		}

		private void initCircuit(Circuit circuit, ChannelHandlerContext ctx) {
			circuit.attribute("transfer-protocol", "net/1.1");
			circuit.attribute("select-type", "server");
			circuit.attribute("local-address", ctx.channel().localAddress()
					.toString());
			circuit.attribute("remote-address", ctx.channel().remoteAddress()
					.toString());
			circuit.attribute("select-simple", "udt");
			circuit.attribute("select-name", buildNetGraph().name());
			circuit.attribute("select-id", ctx.channel().id().asShortText());
		}

		/*
		 * 
		 * 覆盖 channelActive 方法 在channel被启用的时候触发 (在建立连接的时候)
		 * 
		 * channelActive 和 channelInActive 在后面的内容中讲述，这里先不做详细的描述
		 */
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {

			Frame frame = new Frame("connect / net/1.1");
			Circuit circuit = new Circuit("net/1.1 200 ok");
			initCircuit(circuit, ctx);
			try {
				circuit.attribute("ctx", ctx);
				senderIn.flow(frame, circuit);
				circuit.removeAttribute("ctx");

				acceptpin.flow(frame, circuit);
				if (circuit.isPiggybacking()) {// 捎带信息
					Frame back = circuit.snapshot("piggyback / net/1.1");
					UdtMessage backmsg = new UdtMessage(back.toByteBuf());
					ctx.writeAndFlush(backmsg);
					// if("disconnect".equals(circuit.attribute("net-action"))){
					// ctx.close();
					// }
				}
				frame.dispose();
			} catch (Exception e) {
				CircuitException.print(e, log);
				throw new EcmException(e);
			}

		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			Frame frame = new Frame("disconnect / net/1.1");
			Circuit circuit = new Circuit("net/1.1 200 ok");
			initCircuit(circuit, ctx);
			try {
				circuit.attribute("ctx", ctx);
				senderIn.flow(frame, circuit);
				circuit.removeAttribute("ctx");

				acceptpin.flow(frame, circuit);
				frame.dispose();
			} catch (Exception e) {
				CircuitException.print(e, log);
				throw new EcmException(e);
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
				throws Exception {
			errorCaught(ctx, cause);
		}

		// @Override
		// public void channelReadComplete(ChannelHandlerContext ctx)
		// throws Exception {
		// ctx.flush();
		// }
	}

	@Override
	protected void errorCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		Channel ch = ctx.channel();
		Frame frame = new Frame("piggyback / net/1.1");
		AttributeKey<String> key = AttributeKey.valueOf(NetConstans.FRAME_HEADKEY_FRAME_ID);
		String mid = ctx.attr(key).get();
		if (!StringUtil.isEmpty(mid)) {
			frame.head(NetConstans.FRAME_HEADKEY_FRAME_ID, mid);
		}
		CircuitException cir = CircuitException.search(cause);
		if (cir != null) {
			frame.head("status", cir.getStatus());
			frame.head("message", cause.getMessage().replace("\r\n", ""));
			String error = CircuitException.print(cause, null);
			if (error != null) {
				frame.content().writeBytes(error.getBytes());
			}
			UdtMessage umsg = new UdtMessage(frame.toByteBuf());
			ch.writeAndFlush(umsg);
			return;
		}
		frame.head("status", "503");
		frame.head("message", cause.getMessage().replace("\r\n", ""));
		UdtMessage umsg = new UdtMessage(frame.toByteBuf());
		ch.writeAndFlush(umsg);
	}
}
