package cj.studio.ecm.net.nio.netty.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.AnnotationProtocolFactory;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.GraphCreator;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.IProtocolFactory;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.graph.IHandleBinder;
import cj.studio.ecm.net.graph.INetGraph;
import cj.studio.ecm.net.nio.ChannelPool;
import cj.studio.ecm.net.nio.IChannelPool;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.nio.netty.IChannelInitializer;
import cj.studio.ecm.net.nio.netty.INettyGraph;
import cj.studio.ecm.net.nio.netty.NettyClient;
import cj.studio.ecm.net.web.HttpCircuit;
import cj.studio.ecm.net.web.HttpFrame;
import cj.ultimate.util.StringUtil;

//客户端可以是浏览器，也可为自己开发的客户端代码。
public class HttpNettyClient extends NettyClient<SocketChannel> {
	Logger log = LoggerFactory.getLogger(HttpNettyClient.class);

	@Override
	protected Class<? extends Channel> getChannelClass() {
		return NioSocketChannel/* NioUdtMessageConnectorChannel */.class;
	}

	@Override
	public String simple() {
		// TODO Auto-generated method stub
		return "http";
	}

	@Override
	protected IChannelInitializer<SocketChannel> createChannel() {
		return new ConnectorInitializer();
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
					return new HttpClientAcceptor();
				}
				if ("sender".equals(sink)) {
					return new HttpClientSender();
				}
				if ("sync".equals(sink)) {
					return new HttpClientSync();
				}
				return null;
			}
		};
	}

	class ConnectorInitializer implements IChannelInitializer<SocketChannel> {

		@Override
		public IHandleBinder initChannel(SocketChannel ch, long idleCheckin)
				throws Exception {
			ChannelPipeline p = ch.pipeline();

			// p.addLast("log", new LoggingHandler(LogLevel.ERROR));
			// Enable HTTPS if necessary.
			String logs = props.get("log");
			if (!StringUtil.isEmpty(logs))
				p.addLast(new LoggingHandler(LogLevel.INFO));
			// p.addLast("decoder", new HttpResponseDecoder());
			// p.addLast("encoder", new HttpRequestEncoder());
			p.addLast("codec", new HttpClientCodec());
			// Remove the following line if you don't want automatic content
			// decompression.
			p.addLast("inflater", new HttpContentDecompressor());

			// Uncomment the following line if you don't want to handle
			// HttpChunks.
			p.addLast("aggregator", new HttpObjectAggregator(1048576));
			// 客户端的逻辑
			ConnectorHandler hb = new ConnectorHandler();
			if (idleCheckin >= 0) {
				p.addLast(new Idle(idleCheckin, idleCheckin, idleCheckin,
						TimeUnit.MILLISECONDS), hb);
				// pipeline.addLast(new Read(idleCheckin, TimeUnit.SECONDS),hb);
				// pipeline.addLast(new Write(idleCheckin, TimeUnit.SECONDS));
			} else {
				p.addLast(hb);
			}
			return hb;
		}
	}
	@Override
	protected IChannelPool newPool(Map<String, String> props) {
		int capcity = 2;
		long idleTimeout = -1;// 空闲休眠时间，空闲在池中超出此时间则关闭连接。默认－1永远休眠，表示空闲连接不被释放
		long idleCheck = -1;
		long activeTimeout = -1;
		String pidle = props.get("idle");
		if (!StringUtil.isEmpty(pidle)) {
			idleTimeout = new Long(pidle);
		}
		String pidleCheck = props.get("idleCheck");
		if (!StringUtil.isEmpty(pidleCheck)) {
			idleCheck = new Long(pidleCheck);
		}
		String pactiveTimeout = props.get("activeTimeout");
		if (!StringUtil.isEmpty(pactiveTimeout)) {
			activeTimeout = new Long(pactiveTimeout);
		}
		String works = props.get("work");
		if (!StringUtil.isEmpty(works)) {
			capcity = new Integer(works);
		}
		IChannelPool pool = new ChannelPool(this, capcity, idleTimeout,
				idleCheck, activeTimeout);

		return pool;
	}
	public static void main(String... log) throws InterruptedException,
			CircuitException {
		HttpNettyClient c = new HttpNettyClient();
		Channel ch = c.connect("localhost", "8080");
		INetGraph g = c.buildNetGraph();
		g.netOutput().plugFirst("connect", new ISink() {

			@Override
			public void flow(Frame frame,Circuit circuit, IPlug plug) throws CircuitException {
				System.out.println(frame);
				if(frame.content().readableBytes()>0){
					System.out.println(new String(frame.content().readFully()));
				}
			}
		});

		HttpFrame f = new HttpFrame("get /website/ http/1.1");
		// HttpFrame f=new
		// HttpFrame("get ws://localhost:8080/website/wssite http/1.1");
		// f.head("Connection","keep-alive");
		Circuit circuit = new HttpCircuit("http/1,1 200 ok");
		g.netInput().flow(f, circuit);
		// DefaultHttpRequest request = new DefaultHttpRequest(
		// HttpVersion.HTTP_1_1, HttpMethod.GET, "/website/");
		// request.headers().set(HttpHeaders.Names.HOST, "localhost:8080");
		// request.headers().set(HttpHeaders.Names.CONNECTION,
		// HttpHeaders.Values.CLOSE);
		// request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING,
		// HttpHeaders.Values.GZIP);
		/*
		 * HTTP解码器现在在每个HTTP消息中总生成多个消息对象： ? 1 2 3 1 * HttpRequest / HttpResponse
		 * 0 - n * HttpContent 1 * LastHttpContent
		 * 要看更多的细节，请到转到已更新了的HttpSnoopServer例子
		 * 。如果你希望为一个单一的HTTP消息处理多个消息，你可以把HttpObjectAggregator放入管道里
		 * 。HttpObjectAggregator会把多个消息转换为一个单一的FullHttpRequest或是FullHttpResponse。
		 */
		// Set some example cookies.
		// request.headers().set(
		// HttpHeaders.Names.COOKIE,
		// ClientCookieEncoder.encode(
		// new DefaultCookie("my-cookie", "foo"),
		// new DefaultCookie("another-cookie", "bar")));
		// ch.writeAndFlush(request);
		System.out.println(ch);
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
	
	class ConnectorHandler extends SimpleChannelInboundHandler<HttpObject>
			implements IHandleBinder {
		IPin pin;

		@Override
		public void init(INettyGraph g) {
			this.pin = g.newAcceptorPin();
		}

		private void copyToFrame(FullHttpResponse res, Frame frame) {
			HttpHeaders headers = res.headers();
			Iterator<Entry<String, String>> it = headers.entries().iterator();
			while (it.hasNext()) {
				Entry<String, String> e = it.next();
				frame.head(e.getKey(), e.getValue());
			}
			// 拷贝head
			if (res.content().readableBytes() > 0) {
				frame.content().writeBytes(res.content());
				// fr.release();

			}
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, HttpObject msg)
				throws Exception {
			if (!(msg instanceof HttpResponse)) {
				return;
			}
			HttpResponse response = (HttpResponse) msg;
			try {
				String line = String.format("response / %s", response
						.getProtocolVersion().toString());
				Frame frame = new HttpFrame(line);
				copyToFrame((FullHttpResponse) response, frame);
				line = String.format("%s %s %s", response.getProtocolVersion()
						.toString(), response.getStatus().code(), response
						.getStatus().reasonPhrase());

				Circuit circuit = new Circuit(line);
				initCircuit(circuit, ctx);
				pin.flow(frame, circuit);
			} catch (Exception e) {
				CircuitException.print(e, log);
				throw new EcmException(e);
			}

			// if (HttpHeaders.isTransferEncodingChunked(response)) {
			// System.out.println("CHUNKED CONTENT {");
			// } else {
			// System.out.println("CONTENT {");
			// }

			// if (msg instanceof HttpContent)
			// {//直接是字节内容，而非response对象，这需要HttpObjectAggregator放入管道里
			// HttpContent content = (HttpContent) msg;
			// System.out.print(content.content().toString(CharsetUtil.UTF_8));
			// System.out.flush();
			//
			// if (content instanceof LastHttpContent) {
			// System.out.println("} END OF CONTENT");
			// }
			// }

		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			pool().add(ctx.channel());
			Frame frame = new HttpFrame("connect / net/1.1");
			Circuit circuit = new Circuit("net/1.1 200 ok");
			initCircuit(circuit, ctx);
			try {
				pin.flow(frame, circuit);
				frame.dispose();
			} catch (Exception e) {
				CircuitException.print(e, log);
				throw new EcmException(e);
			}
		}

		private void initCircuit(Circuit circuit, ChannelHandlerContext ctx) {
			circuit.attribute("select-type", "client");
			circuit.attribute("local-address", ctx.channel().localAddress()
					.toString());
			circuit.attribute("remote-address", ctx.channel().remoteAddress()
					.toString());
			circuit.attribute("select-name", buildNetGraph().name());
			circuit.attribute("select-simple", "http");
			circuit.attribute("select-id", ctx.channel().id().asShortText());
		}

		// @Override
		// public void channelReadComplete(ChannelHandlerContext ctx)
		// throws Exception {
		// ctx.flush();
		// }

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			pool().remove(ctx.channel());
			Frame frame = new HttpFrame("disconnect / net/1.1");
			Circuit circuit = new Circuit("net/1.1 200 ok");
			initCircuit(circuit, ctx);
			try {
				pin.flow(frame, circuit);
				frame.dispose();
			} catch (Exception e) {
				CircuitException.print(e, log);
				throw new EcmException(e);
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
				throws Exception {
			super.exceptionCaught(ctx, cause);
			log.error(cause.getMessage());
			throw new EcmException(cause);
		}

	}
}
