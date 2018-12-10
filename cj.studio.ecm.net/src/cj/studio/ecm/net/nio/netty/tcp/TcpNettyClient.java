package cj.studio.ecm.net.nio.netty.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

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
import cj.studio.ecm.net.nio.netty.udt.UdtNettyClient;
import cj.ultimate.util.StringUtil;

public class TcpNettyClient extends NettyClient<SocketChannel> {
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
					return new TcpClientAcceptor();
				}
				if ("sender".equals(sink)) {
					return new TcpClientSender();
				}
				if ("sync".equals(sink)) {
					return new TcpClientSync();
				}
				return null;
			}
		};
	}

	@Override
	public String simple() {
		// TODO Auto-generated method stub
		return "tcp";
	}

	@Override
	protected Class<? extends Channel> getChannelClass() {
		return NioSocketChannel.class;
	}

	@Override
	protected IChannelInitializer<SocketChannel> createChannel() {
		return new ConnectorInitializer();
	}

	class ConnectorInitializer implements IChannelInitializer<SocketChannel> {

		@Override
		public IHandleBinder initChannel(SocketChannel ch, long idleCheckin)
				throws Exception {
			ChannelPipeline pipeline = ch.pipeline();

			/*
			 * 这个地方的 必须和服务端对应上。否则无法正常解码和编码
			 * 
			 * 解码和编码 我将会在下一张为大家详细的讲解。再次暂时不做详细的描述
			 */
			pipeline.addLast(new LengthFieldBasedFrameDecoder(
					Integer.MAX_VALUE, 0, 4, 0, 4));
			// pipeline.addLast("decode", new
			// DelimiterBasedFrameDecoder(Integer.MAX_VALUE,
			// Delimiters.lineDelimiter()));
			// pipeline.addLast("framer", new DelimiterFrameCodec2());//分隔符解释器
			String logs = props.get("log");
			if (!StringUtil.isEmpty(logs))
				pipeline.addLast(new LoggingHandler(LogLevel.valueOf(logs
						.toUpperCase())));
			// 客户端的逻辑
			IHandleBinder hb = new ConnectorHandler();
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

		private void initCircuit(Circuit circuit, ChannelHandlerContext ctx) {
			circuit.head("select-type", "client");
			circuit.head("local-address", ctx.channel().localAddress()
					.toString());
			circuit.head("remote-address", ctx.channel().remoteAddress()
					.toString());
			circuit.head("select-name", buildNetGraph().name());
			circuit.head("select-simple", "tcp");
			circuit.head("select-id", ctx.channel().id().asShortText());
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg)
				throws Exception {
			ByteBuf buf = (ByteBuf) msg;
			byte[] b = new byte[buf.readableBytes()];
			buf.readBytes(b);
			buf.release();
			try {
				Frame frame = new Frame(b);
				Circuit circuit = new Circuit("net/1.1 200 ok");
				initCircuit(circuit, ctx);
				circuit.attribute("ctx", ctx);
				pin.flow(frame, circuit);
				// frame.dispose();
			} catch (Exception e) {
				CircuitException.print(e, log);
				throw new RuntimeException(e);
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
				throws Exception {
			// TODO Auto-generated method stub
			super.exceptionCaught(ctx, cause);
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			Frame frame = new Frame("connect / net/1.1");
			Circuit circuit = new Circuit("net/1.1 200 ok");
			initCircuit(circuit, ctx);
			circuit.attribute("ctx", ctx);
			try {
				pin.flow(frame, circuit);
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
			initCircuit(circuit, ctx);
			circuit.attribute("ctx", ctx);
			try {
				pin.flow(frame, circuit);
				frame.dispose();
			} catch (Exception e) {
				CircuitException.print(e, log);
				throw new RuntimeException(e);
			}
		}

		// @Override
		// public void channelReadComplete(ChannelHandlerContext ctx)
		// throws Exception {
		// ctx.flush();
		// }
	}

}
