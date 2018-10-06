package cj.studio.ecm.net.nio.netty;

import static io.netty.handler.codec.http.HttpHeaders.is100ContinueExpected;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ServerChannel;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;

import java.net.InetAddress;

import cj.studio.ecm.net.graph.IHandleBinder;
import cj.ultimate.util.StringUtil;

//@see netty http-websocketx协议示例，它实现了握手功能
public class WebsocketxNettyServer extends NettyServer<SocketChannel> {

	@Override
	public String simple() {
		// TODO Auto-generated method stub
		return "websocket";
	}
	@Override
	protected Class<? extends ServerChannel> getParentChannelClass() {
		// TODO Auto-generated method stub
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
	protected IChannelInitializer<SocketChannel> createChildChannel() {
		return new NioServerInitializer<SocketChannel>();
	}


	class NioServerInitializer<T extends SocketChannel> implements
			IChannelInitializer<T> {
		@Override
		public IHandleBinder initChannel(T ch,long idleCheckin) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			
			pipeline.addLast(new HttpServerCodec());
			pipeline.addLast(new HttpObjectAggregator(65536));// 最大内容长度
			pipeline.addLast(new ChunkedWriteHandler());
			// Remove the following line if you don't want automatic content
			// compression.
			pipeline.addLast(new HttpContentCompressor());
			pipeline.addLast(new WebSocketServerProtocolHandler("/websocket"));
			pipeline.addLast("handler", new NioServerHandler());
			return null;
		}
	}

	class NioServerHandler extends SimpleChannelInboundHandler<Object> {
		private WebSocketServerHandshaker handshaker;

		// 可用的通道引出来了，服务器端可以用之主动向客户端发送消息
		@Override
		public void messageReceived(ChannelHandlerContext ctx, Object msg) {
			if (msg instanceof FullHttpRequest) {
				handleHttpRequest(ctx, (FullHttpRequest) msg);
			} else if (msg instanceof WebSocketFrame) {
				handleWebSocketFrame(ctx, (WebSocketFrame) msg);
			}
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) {
			ctx.flush();
		}

		private void handleHttpRequest(ChannelHandlerContext ctx,
				FullHttpRequest req) {
			if (is100ContinueExpected(req)) {
				ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
			}
			boolean keepAlive = isKeepAlive(req);
			// pin.output(frame);
			// 这段代码一般放到最后保持连接与否
			// if (!keepAlive) {
			// ctx.write(res)
			// .addListener(ChannelFutureListener.CLOSE);// 关闭连接
			// } else {
			// res.headers().set(CONNECTION, Values.KEEP_ALIVE);
			// ctx.write(res);
			// }
			// Handle a bad request.
			if (!req.getDecoderResult().isSuccess()) {
				sendHttpResponse(ctx, req, new DefaultFullHttpResponse(
						HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
				return;
			}

			// Allow only GET methods.
			if (req.getMethod() != HttpMethod.GET) {
				sendHttpResponse(ctx, req, new DefaultFullHttpResponse(
						HTTP_1_1, HttpResponseStatus.FORBIDDEN));
				return;
			}

			// Send the demo page and favicon.ico
			if ("/".equals(req.getUri())) {
				// ByteBuf content = WebSocketServerIndexPage
				// .getContent(getWebSocketLocation(req));
				// FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1,
				// OK, content);
				//
				// res.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
				// setContentLength(res, content.readableBytes());
				//
				// sendHttpResponse(ctx, req, res);
				return;
			}
			if ("/favicon.ico".equals(req.getUri())) {
				FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1,
						HttpResponseStatus.NOT_FOUND);
				sendHttpResponse(ctx, req, res);
				return;
			}

			// Handshake
			WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
					getWebSocketLocation(req), null, false);
			handshaker = wsFactory.newHandshaker(req);
			if (handshaker == null) {
				WebSocketServerHandshakerFactory
						.sendUnsupportedWebSocketVersionResponse(ctx.channel());
			} else {
				handshaker.handshake(ctx.channel(), req);
			}

		}

		private void handleWebSocketFrame(ChannelHandlerContext ctx,
				WebSocketFrame frame) {

			// Check for closing frame
			if (frame instanceof CloseWebSocketFrame) {
				handshaker.close(ctx.channel(),
						(CloseWebSocketFrame) frame.retain());
				return;
			}
			if (frame instanceof PingWebSocketFrame) {
				ctx.channel().write(
						new PongWebSocketFrame(frame.content().retain()));
				return;
			}
			if (!(frame instanceof TextWebSocketFrame)) {
				throw new UnsupportedOperationException(String.format(
						"%s frame types not supported", frame.getClass()
								.getName()));
			}

			// Send the uppercase string back.
			String request = ((TextWebSocketFrame) frame).text();
			System.err.printf("%s received %s%n", ctx.channel(), request);
			ctx.channel().write(new TextWebSocketFrame(request.toUpperCase()));
		}

		private void sendHttpResponse(ChannelHandlerContext ctx,
				FullHttpRequest req, FullHttpResponse res) {
			// Generate an error page if response getStatus code is not OK
			// (200).
			if (res.getStatus().code() != 200) {
				ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(),
						CharsetUtil.UTF_8);
				res.content().writeBytes(buf);
				buf.release();
				// setContentLength(res, res.content().readableBytes());
			}

			// Send the response and close the connection if necessary.
			ChannelFuture f = ctx.channel().writeAndFlush(res);
			if (!isKeepAlive(req) || res.getStatus().code() != 200) {
				f.addListener(ChannelFutureListener.CLOSE);
			}
		}

		private String getWebSocketLocation(FullHttpRequest req) {
			String location = req.headers().get(HttpHeaders.Names.HOST)
					+ "/dir/dir";
			// if (WebSocketServer.SSL) {
			// return "wss://" + location;
			// } else {
			return "ws://" + location;
			// }
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg)
				throws Exception {
			// 返回客户端消息 - 我已经接收到了你的消息
			if (msg instanceof HttpRequest) {
				HttpRequest req = (HttpRequest) msg;

				if (is100ContinueExpected(req)) {
					ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
				}
				boolean keepAlive = isKeepAlive(req);
				// pin.output(m);
				FullHttpResponse response = new DefaultFullHttpResponse(
						HTTP_1_1, OK, Unpooled.wrappedBuffer("<div>这是测试</div>"
								.getBytes()));
				response.headers().set(CONTENT_TYPE, "text/html;charset=UTF-8");
				response.headers().set(CONTENT_LENGTH,
						response.content().readableBytes());

				if (!keepAlive) {
					ctx.write(response)
							.addListener(ChannelFutureListener.CLOSE);// 关闭连接
				} else {
					response.headers().set(CONNECTION, Values.KEEP_ALIVE);
					ctx.write(response);
				}
			}

		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			cause.printStackTrace();
			ctx.close();
		}

		/*
		 * 
		 * 覆盖 channelActive 方法 在channel被启用的时候触发 (在建立连接的时候)
		 * 
		 * channelActive 和 channelInActive 在后面的内容中讲述，这里先不做详细的描述
		 */
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {

			System.out.println("RamoteAddress : "
					+ ctx.channel().remoteAddress() + " active !");

			ctx.writeAndFlush("Welcome to "
					+ InetAddress.getLocalHost().getHostName() + " service!\n");

		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		}
	}

}
