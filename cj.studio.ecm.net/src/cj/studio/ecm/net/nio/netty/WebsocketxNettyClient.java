package cj.studio.ecm.net.nio.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.util.CharsetUtil;

import java.net.URI;

import cj.studio.ecm.net.graph.IHandleBinder;
import cj.studio.ecm.net.nio.DelimiterFrameCodec2;

/**
 * websocket协议是html5新增的特性，它建立客户端与服务器的双向通讯，多数浏览器已支持。 此类通过客户端模拟浏览器的调用。
 * 
 * @author cj
 * 
 */
// @see netty http-websocketx协议示例，它实现了握手功能
public class WebsocketxNettyClient extends NettyClient<SocketChannel> {

	@Override
	public String simple() {
		// TODO Auto-generated method stub
		return "websocket";
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
		public IHandleBinder initChannel(SocketChannel ch,long idleCheckin) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();

			/*
			 * 这个地方的 必须和服务端对应上。否则无法正常解码和编码
			 * 
			 * 解码和编码 我将会在下一张为大家详细的讲解。再次暂时不做详细的描述
			 */
			// pipeline.addLast("decoder", new DelimiterFrameDecode());
			// pipeline.addLast("encoder", new DelimiterFrameEncode());
			pipeline.addLast("framer", new DelimiterFrameCodec2());
			// 客户端的逻辑
			// Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08
			// or V00.
			// If you change it to V00, ping is not supported and remember to
			// change
			// HttpResponseDecoder to WebSocketHttpResponseDecoder in the
			// pipeline.
			URI uri = new URI("ws://localhost:8080/websocket");
			String protocol = uri.getScheme();
			if (!"ws".equals(protocol)) {
				throw new IllegalArgumentException("Unsupported protocol: "
						+ protocol);
			}
			HttpHeaders customHeaders = new DefaultHttpHeaders();
			customHeaders.add("MyHeader", "MyValue");
			WebSocketClientHandler handler = new WebSocketClientHandler(
					WebSocketClientHandshakerFactory.newHandshaker(uri,
							WebSocketVersion.V13, null, false, customHeaders));
			pipeline.addLast("handler", handler);
			return null;
		}
	}

	class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

		private final WebSocketClientHandshaker handshaker;
		private ChannelPromise handshakeFuture;

		public WebSocketClientHandler(WebSocketClientHandshaker handshaker) {
			this.handshaker = handshaker;
		}

		public ChannelFuture handshakeFuture() {
			return handshakeFuture;
		}

		@Override
		public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
			handshakeFuture = ctx.newPromise();
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			handshaker.handshake(ctx.channel());
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			System.out.println("WebSocket Client disconnected!");
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, Object msg)
				throws Exception {
			Channel ch = ctx.channel();
			if (!handshaker.isHandshakeComplete()) {
				handshaker.finishHandshake(ch, (FullHttpResponse) msg);
				System.out.println("WebSocket Client connected!");
				handshakeFuture.setSuccess();
				return;
			}

			if (msg instanceof FullHttpResponse) {
				FullHttpResponse response = (FullHttpResponse) msg;
				throw new Exception("Unexpected FullHttpResponse (getStatus="
						+ response.getStatus() + ", content="
						+ response.content().toString(CharsetUtil.UTF_8) + ')');
			}

			WebSocketFrame frame = (WebSocketFrame) msg;
			if (frame instanceof TextWebSocketFrame) {
				TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
				System.out.println("WebSocket Client received message: "
						+ textFrame.text());
			} else if (frame instanceof PongWebSocketFrame) {
				System.out.println("WebSocket Client received pong");
			} else if (frame instanceof CloseWebSocketFrame) {
				System.out.println("WebSocket Client received closing");
				ch.close();
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
				throws Exception {
			cause.printStackTrace();

			if (!handshakeFuture.isDone()) {
				handshakeFuture.setFailure(cause);
			}

			ctx.close();
		}
	}

}
