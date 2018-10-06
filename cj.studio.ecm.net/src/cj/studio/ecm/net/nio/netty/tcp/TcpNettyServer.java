package cj.studio.ecm.net.nio.netty.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;
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

public class TcpNettyServer extends NettyServer<SocketChannel> implements NetConstans{
	@Override
	public String simple() {
		// TODO Auto-generated method stub
		return "tcp";
	}
	@Override
	protected IChannelInitializer<SocketChannel> createChildChannel() {
		return new NioServerInitializer<SocketChannel>();
	}
	@Override
	protected Class<? extends ServerChannel> getParentChannelClass() {
		return NioServerSocketChannel.class;
	}
	@Override
	protected void initPorperties(ServerBootstrap b) {
		super.initPorperties(b);
		String SO_KEEPALIVE = props.get("child.SO_KEEPALIVE");
		if (StringUtil.isEmpty(SO_KEEPALIVE))
			SO_KEEPALIVE = "true";
		b.childOption(ChannelOption.SO_KEEPALIVE, Boolean.valueOf(SO_KEEPALIVE));
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
					return new TcpServerAcceptor();
				}
				if ("sender".equals(sink)) {
					return new TcpServerSender();
				}
				if ("diverter".equals(sink)) {
					return new TcpServerDiverter();
				}
				return null;
			}
		};
	}
	class NioServerInitializer<T extends SocketChannel> implements
			IChannelInitializer<T> {

		@Override
		public IHandleBinder initChannel(T ch,long idleCheckin) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4,0,4));
//			pipeline.addLast("framer", new DelimiterFrameCodec2());
//			pipeline.addLast("decode", new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, Delimiters.lineDelimiter()));//此为一种回撤符分隔流的解码器
			// pipeline.addLast("decoder", new DelimiterFrameDecode());
			// pipeline.addLast("encoder", new DelimiterFrameEncode());
			String logs=props.get("log");
			if(!StringUtil.isEmpty(logs))
				pipeline.addLast(new LoggingHandler(LogLevel.valueOf(logs.toUpperCase())));
			IHandleBinder hb=new NioServerHandler();
			pipeline.addLast("handler", hb);
			return hb;
		}
	}

	class NioServerHandler extends ChannelHandlerAdapter implements IHandleBinder{
		// 可用的通道引出来了，服务器端可以用之主动向客户端发送消息
		IPin acceptpin;
		IPin senderIn;
		@Override
		public void init(INettyGraph g) {
			this.acceptpin = g.newAcceptorPin();
			this.senderIn=g.in("senderIn");
		}
		private void initCircuit(Circuit circuit ,ChannelHandlerContext ctx){
			circuit.attribute("transfer-protocol","net/1.1");
			circuit.attribute("select-type","server");
			circuit.attribute("local-address", ctx.channel().localAddress()
					.toString());
			circuit.attribute("remote-address", ctx.channel().remoteAddress()
					.toString());
			circuit.attribute("select-name", buildNetGraph().name());
			circuit.attribute("select-simple", "tcp");
			circuit.attribute("select-id", ctx.channel().id().asShortText());
		}
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg)
				throws Exception {
			try {
				ByteBuf buf=(ByteBuf)msg;
				byte[] b=new byte[buf.readableBytes()];
				buf.readBytes(b);
				buf.release();
				Frame frame = new Frame(b);
				Circuit circuit = new Circuit(frame.protocol() + " 200 ok");
				initCircuit(circuit, ctx);
				// umsg.release();
				String frameId=frame.head(NetConstans.FRAME_HEADKEY_FRAME_ID);
				if(frame.containsHead(FRAME_HEADKEY_FRAME_ID)){
					circuit.piggybacking(true);
					//用于通知下游的net对该侦采用同步模式发送。如果开发者在链路中去除了该属性，则源头的调用者将等待超时。因为后续没有同步返回结果。
					frame.head(FRAME_HEADKEY_CIRCUIT_SYNC,"true");
					AttributeKey<String> key = AttributeKey
							.valueOf(FRAME_HEADKEY_FRAME_ID);
					ctx.attr(key).set(frameId);
					//侦的编号对开发者不可见。
					frame.removeHead(FRAME_HEADKEY_FRAME_ID);
				}
				FeedbackHelper.tcpServerFeedback(ctx, frame, circuit);
				acceptpin.flow(frame, circuit);
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
					back.head(FRAME_HEADKEY_FRAME_ID, frameId);
					byte[] bt=back.toBytes();
					bt=TcpFrameBox.box(bt);
					ByteBuf btbuf = Unpooled.directBuffer();
					btbuf.writeBytes(bt);
					ctx.writeAndFlush(btbuf);
					if(btbuf.refCnt()>0)
					btbuf.release();
					if("disconnect".equals(circuit.attribute("net-action"))){
						ctx.close();
					}
				}
				frame.dispose();
			} catch (Exception e) {
				CircuitException.print(e, log);
				throw new RuntimeException(e);
			}
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
			initCircuit(circuit,ctx);
			try {
				circuit.attribute("ctx",ctx);
				senderIn.flow(frame, circuit);
				circuit.removeAttribute("ctx");
				
				acceptpin.flow(frame, circuit);
				frame.dispose();
			} catch (Exception e) {
				CircuitException.print(e, log);
				throw new RuntimeException(e);
			}
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			Frame frame = new Frame("disconnect / net/1.1");
			Circuit circuit = new Circuit("net/1.1 200 ok");
			initCircuit(circuit,ctx);
			try {
				circuit.attribute("ctx",ctx);
				senderIn.flow(frame, circuit);
				circuit.removeAttribute("ctx");
				
				acceptpin.flow(frame, circuit);
				frame.dispose();
			} catch (Exception e) {
				CircuitException.print(e, log);
				throw new RuntimeException(e);
			}
		}
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
				throws Exception {
			Channel ch = ctx.channel();
			Frame frame = new Frame("piggyback / net/1.1");
			AttributeKey<String> key = AttributeKey.valueOf(FRAME_HEADKEY_FRAME_ID);
			String mid = ctx.attr(key).get();
			if (!StringUtil.isEmpty(mid)) {
				frame.head(FRAME_HEADKEY_FRAME_ID, mid);
			}
			CircuitException cir = CircuitException.search(cause);
			if (cir != null) {
				frame.head("status", cir.getStatus());
				frame.head("message", cause.getMessage().replace("\r\n", ""));
				String error = CircuitException.print(cause, null);
				if (error != null) {
					frame.content().writeBytes(error.getBytes());
				}
				byte[] b=frame.toBytes();
				b=TcpFrameBox.box(b);
				ch.writeAndFlush(b);
				return;
			}
			frame.head("status", "503");
			frame.head("message", cause.getMessage().replace("\r\n", ""));
			byte[] b=frame.toBytes();
			b=TcpFrameBox.box(b);
			ch.writeAndFlush(b);
		}
//		@Override
//		public void channelReadComplete(ChannelHandlerContext ctx)
//				throws Exception {
//			ctx.flush();
//		}
	}
	
}
