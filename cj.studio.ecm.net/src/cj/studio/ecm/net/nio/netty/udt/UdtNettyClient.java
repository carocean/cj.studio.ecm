package cj.studio.ecm.net.nio.netty.udt;

import io.netty.bootstrap.ChannelFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.UdtMessage;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.AnnotationProtocolFactory;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.GraphCreator;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IProtocolFactory;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.graph.IHandleBinder;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.nio.netty.IChannelInitializer;
import cj.studio.ecm.net.nio.netty.INettyGraph;
import cj.studio.ecm.net.nio.netty.NettyClient;
import cj.ultimate.util.StringUtil;

/**
 * 基于udt 消息的协议
 * 
 * @author cj
 *
 */
public class UdtNettyClient extends NettyClient<UdtChannel> {
	Logger log = LoggerFactory.getLogger(UdtNettyClient.class);

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
					return new UdtClientAcceptor();
				}
				if ("sender".equals(sink)) {
					return new UdtClientSender();
				}
				if ("sync".equals(sink)) {
					return new UdtClientSync();
				}
				return null;
			}
		};
	}

	@Override
	public String simple() {
		return "udt";
	}

	@Override
	protected NioEventLoopGroup createGroup(int threadCount) {
		ThreadFactory connectFactory = new UtilThreadFactory("connect");
		return new NioEventLoopGroup(threadCount, connectFactory,
				NioUdtProvider.MESSAGE_PROVIDER);
	}

	@Override
	protected ChannelFactory<UdtChannel> getChannelFactory() {
		return NioUdtProvider.MESSAGE_CONNECTOR;
	}

	@Override
	protected Class<? extends Channel> getChannelClass() {
		return null;
	}

	@Override
	protected IChannelInitializer<UdtChannel> createChannel() {
		return new ConnectorInitializer();
	}

	class ConnectorInitializer implements IChannelInitializer<UdtChannel> {

		@Override
		public IHandleBinder initChannel(UdtChannel ch, long idleCheckin)
				throws Exception {
			ChannelPipeline pipeline = ch.pipeline();

			/*
			 * 这个地方的 必须和服务端对应上。否则无法正常解码和编码
			 * 
			 * 解码和编码 我将会在下一张为大家详细的讲解。再次暂时不做详细的描述
			 */
			String logs = props.get("log");
			if (!StringUtil.isEmpty(logs))
				pipeline.addLast(new LoggingHandler(LogLevel.INFO));
			// 客户端的逻辑
			IHandleBinder hb = new ConnectorHandler();
			// pipeline.addLast("handler", hb);
			if (idleCheckin >= 0) {
				pipeline.addLast(new Idle(idleCheckin, idleCheckin,
						idleCheckin, TimeUnit.MILLISECONDS), hb);
				// pipeline.addLast(new Read(idleCheckin, TimeUnit.SECONDS),hb);
				// pipeline.addLast(new Write(idleCheckin, TimeUnit.SECONDS));
			} else {
				pipeline.addLast(hb);
			}

			return hb;
		}
	}

	// class Read extends ReadTimeoutHandler {
	// public Read(long timeout, TimeUnit unit) {
	// super(timeout, unit);
	// // TODO Auto-generated constructor stub
	// }
	//
	// @Override
	// protected void readTimedOut(ChannelHandlerContext ctx) throws Exception {
	// pool().idle(ctx.channel());
	// }
	// }
	//
	// class Write extends WriteTimeoutHandler {
	// public Write(long timeout, TimeUnit unit) {
	// super(timeout, unit);
	// // TODO Auto-generated constructor stub
	// }
	//
	// @Override
	// protected void writeTimedOut(ChannelHandlerContext ctx)
	// throws Exception {
	// pool().idle(ctx.channel());
	// }
	// }

	class Idle extends IdleStateHandler {

		public Idle(long readerIdleTime, long writerIdleTime, long allIdleTime,
				TimeUnit unit) {
			super(readerIdleTime, writerIdleTime, allIdleTime, unit);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e)
				throws Exception {

			if (e.state() == IdleState.ALL_IDLE) {
				// pool().remove(ctx.channel());
				pool().idle(ctx.channel());
			}
		}

	}

	class ConnectorHandler extends ChannelHandlerAdapter implements
			IHandleBinder {
		IPin pin;

		@Override
		public void init(INettyGraph g) {
			this.pin = g.newAcceptorPin();
		}

		public ConnectorHandler() {
		}

		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
				throws Exception {
			if (!(evt instanceof IdleStateEvent)) {
				return;
			}

			IdleStateEvent e = (IdleStateEvent) evt;
			if (e.state() == IdleState.READER_IDLE) {
				// The connection was OK but there was no traffic for last
				// period.
				System.out.println("Disconnecting due to no inbound traffic");
				ctx.close();
			}

		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable e)
				throws Exception {
			super.exceptionCaught(ctx, e);
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg)
				throws Exception {
			UdtMessage umsg = (UdtMessage) msg;
			Circuit circuit = new Circuit("net/1.1 200 ok");
			initCircuit(circuit, ctx);
			try {
				byte[] b = new byte[umsg.content().readableBytes()];
				umsg.content().readBytes(b);
				Frame frame = new Frame(b);
				umsg.release();

				pin.flow(frame, circuit);
				
			} catch (Exception e) {
				CircuitException.print(e, log);
			}finally{
//				if(circuit.isPiggybacking()){//如果指定为稍带，则写向服务器。由于netty是异步系统，在此发送后服务器端并不会反回给调用者，而是由一系统的handler接收了。
//					
//				}
			}
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			pool().add(ctx.channel());
			Frame frame = new Frame("connect / net/1.1");
			Circuit circuit = new Circuit("net/1.1 200 ok");
			initCircuit(circuit, ctx);
			try {
				pin.flow(frame, circuit);
				frame.dispose();
			} catch (Exception e) {
				CircuitException.print(e, log);
			}
		}

		private void initCircuit(Circuit circuit, ChannelHandlerContext ctx) {
			circuit.head("select-type", "client");
			circuit.head("local-address", ctx.channel().localAddress()
					.toString());
			circuit.head("remote-address", ctx.channel().remoteAddress()
					.toString());
			circuit.head("select-name", buildNetGraph().name());
			circuit.head("select-simple", "udt");
			circuit.head("select-id", ctx.channel().id().asShortText());
		}

		// @Override
		// public void channelReadComplete(ChannelHandlerContext ctx)
		// throws Exception {
		// ctx.flush();
		// }

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			pool().remove(ctx.channel());
			Frame frame = new Frame("disconnect / net/1.1");
			Circuit circuit = new Circuit("net/1.1 200 ok");
			initCircuit(circuit, ctx);
			try {
				pin.flow(frame, circuit);
				frame.dispose();
			} catch (Exception e) {
				CircuitException.print(e, log);
			}
		}
	}

}
