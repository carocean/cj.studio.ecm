package cj.studio.ecm.net.nio.netty.local;

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
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ServerChannel;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;

//local/any 本地通讯协议，其端口是假的，可以任何字符串都行，仅能用于在同一VM内通讯(A {@link ServerChannel} for the local transport which allows in VM communication.)
public class LocalNettyServer extends NettyServer<LocalChannel>
		implements NetConstans {
	@Override
	public String simple() {
		// TODO Auto-generated method stub
		return "local";
	}

	@Override
	protected Class<? extends ServerChannel> getParentChannelClass() {
		return LocalServerChannel.class;
	}

	@Override
	protected GraphCreator createNetGraphCreator() {
		// TODO Auto-generated method stub
		return new GraphCreator() {
			@Override
			protected IProtocolFactory newProtocol() {
				// TODO Auto-generated method stub
				return AnnotationProtocolFactory.factory(NetConstans.class);
			}

			@Override
			public ISink createSink(String sink) {
				if ("acceptor".equals(sink)) {
					return new LocalServerAcceptor();
				}
				if ("sender".equals(sink)) {
					return new LocalServerSender();
				}
				if ("diverter".equals(sink)) {
					return new LocalServerDiverter();
				}
				return null;
			}
		};
	}

	@Override
	protected IChannelInitializer<LocalChannel> createChildChannel() {
		// TODO Auto-generated method stub
		return new NioServerInitializer();
	}

	class NioServerInitializer implements IChannelInitializer<LocalChannel> {

		@Override
		public IHandleBinder initChannel(LocalChannel ch, long idleCheckin)
				throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			String logs = props.get("log");
			if (!StringUtil.isEmpty(logs))
				pipeline.addLast(new LoggingHandler(
						LogLevel.valueOf(logs.toUpperCase())));
			// pipeline.addLast("framer", new DelimiterFrameCodec2());
			IHandleBinder hb = new NioServerHandler();
			pipeline.addLast("handler", hb);
			return hb;
		}
	}

	class NioServerHandler extends ChannelHandlerAdapter
			implements IHandleBinder {
		// 可用的通道引出来了，服务器端可以用之主动向客户端发送消息
		IPin acceptpin;
		IPin senderIn;

		@Override
		public void init(INettyGraph g) {
			this.acceptpin = g.newAcceptorPin();
			this.senderIn = g.in("senderIn");
		}

		private void initCircuit(Circuit circuit, ChannelHandlerContext ctx) {
			circuit.attribute("transfer-protocol", "net/1.1");
			circuit.attribute("select-type", "server");
			circuit.attribute("local-address",
					ctx.channel().localAddress().toString());
			circuit.attribute("remote-address",
					ctx.channel().remoteAddress().toString());
			circuit.attribute("select-name", buildNetGraph().name());
			circuit.attribute("select-simple", "local");
			circuit.attribute("select-id", ctx.channel().id().asShortText());
		}
		private void readFlow(ChannelHandlerContext ctx, Object msg) {
			Frame frame = (Frame) msg;
			Circuit circuit = null;
			try {
				circuit = new Circuit(frame.protocol() + " 200 ok");
				initCircuit(circuit, ctx);
				String frameId = frame.head(FRAME_HEADKEY_FRAME_ID);
				if (frame.containsHead(FRAME_HEADKEY_FRAME_ID)) {
					// 用于通知下游的net对该侦采用同步模式发送。如果开发者在链路中去除了该属性，则源头的调用者将等待超时。因为后续没有同步返回结果。
					frame.head(FRAME_HEADKEY_CIRCUIT_SYNC, "true");
					circuit.piggybacking(true);
					AttributeKey<String> key = AttributeKey
							.valueOf(NetConstans.FRAME_HEADKEY_FRAME_ID);
					ctx.attr(key).set(frameId);
					// 侦的编号对开发者不可见。
					frame.removeHead(FRAME_HEADKEY_FRAME_ID);
				}
				FeedbackHelper.localServerFeedback(ctx, frame, circuit);
				acceptpin.flow(frame, circuit);

				if (circuit.isPiggybacking()) {// 捎带信息
					Frame back = null;
					boolean isunwrap = false;
					if (circuit.containsContentType()) {
						String v = circuit.contentType();
						if ("frame/bin".equals(v)
								|| "frame/json".equals(v)) {
							isunwrap = true;
						}
					}
					if (isunwrap) {
						back = new Frame(circuit.content().readFully());
					} else {
						back = circuit.snapshot("piggyback / net/1.1");
					}
					back.head(FRAME_HEADKEY_FRAME_ID, frameId);
					ctx.writeAndFlush(back);// 因为netty是双向引用，故而注掉
					if ("disconnect"
							.equals(circuit.attribute("net-action"))) {
						ctx.close();
					}
				}
			} catch (Exception e) {
				CircuitException.print(e, log);
				throw new EcmException(e);
			}finally{
				frame.dispose();
				circuit.dispose();
			}
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg)
				throws Exception {
			/*
			 * 采用线程池的原因：
			 * 本地服务器在读取时始终使用同一个线程，哪怕在启动时指定了工作线程也不行
			 * 因此在启动类中干脆不为它指定工作线程了，而将工作线程用于此处读取程序。
			 */
//			workerGroup.submit(new MyChannelReader(ctx, msg));//注掉原因：使用线程后错误传不出来，因在线程内，比如对于远程拒绝连接的异常，由于捕获不到，如果客户端同步等待响应，造成堵塞。2016.0707.1555
			readFlow(ctx, msg);
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
				frame.dispose();
			} catch (Exception e) {
				CircuitException.print(e, log);
				throw new RuntimeException(e);
			}
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx)
				throws Exception {
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
				throw new RuntimeException(e);
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
				throws Exception {
			Channel ch = ctx.channel();
			Frame frame = new Frame("piggyback / net/1.1");
			AttributeKey<String> key = AttributeKey
					.valueOf(NetConstans.FRAME_HEADKEY_FRAME_ID);
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
				ch.writeAndFlush(frame.copy());
				return;
			}
			frame.head("status", "503");
			frame.head("message", cause.getMessage().replace("\r\n", ""));
			ch.writeAndFlush(frame.copy());
		}

		// @Override
		// public void channelReadComplete(ChannelHandlerContext ctx)
		// throws Exception {
		// ctx.flush();
		// }
		class MyChannelReader implements Runnable {
			ChannelHandlerContext ctx;
			Object msg;

			public MyChannelReader(ChannelHandlerContext ctx, Object msg) {
				this.ctx = ctx;
				this.msg = msg;
			}

			@Override
			public void run() {
				Frame frame = (Frame) msg;
				Circuit circuit = null;
				try {
					circuit = new Circuit(frame.protocol() + " 200 ok");
					initCircuit(circuit, ctx);
					String frameId = frame.head(FRAME_HEADKEY_FRAME_ID);
					if (frame.containsHead(FRAME_HEADKEY_FRAME_ID)) {
						// 用于通知下游的net对该侦采用同步模式发送。如果开发者在链路中去除了该属性，则源头的调用者将等待超时。因为后续没有同步返回结果。
						frame.head(FRAME_HEADKEY_CIRCUIT_SYNC, "true");
						circuit.piggybacking(true);
						AttributeKey<String> key = AttributeKey
								.valueOf(NetConstans.FRAME_HEADKEY_FRAME_ID);
						ctx.attr(key).set(frameId);
						// 侦的编号对开发者不可见。
						frame.removeHead(FRAME_HEADKEY_FRAME_ID);
					}
					FeedbackHelper.localServerFeedback(ctx, frame, circuit);
					acceptpin.flow(frame, circuit);

					if (circuit.isPiggybacking()) {// 捎带信息
						Frame back = null;
						boolean isunwrap = false;
						if (circuit.containsContentType()) {
							String v = circuit.contentType();
							if ("frame/bin".equals(v)
									|| "frame/json".equals(v)) {
								isunwrap = true;
							}
						}
						if (isunwrap) {
							back = new Frame(circuit.content().readFully());
						} else {
							back = circuit.snapshot("piggyback / net/1.1");
						}
						back.head(FRAME_HEADKEY_FRAME_ID, frameId);
						ctx.writeAndFlush(back);// 因为netty是双向引用，故而注掉
						if ("disconnect"
								.equals(circuit.attribute("net-action"))) {
							ctx.close();
						}
					}
				} catch (Exception e) {
					CircuitException.print(e, log);
					throw new EcmException(e);
				}finally{
					frame.dispose();
					circuit.dispose();
				}
			}

		}
	}

}
