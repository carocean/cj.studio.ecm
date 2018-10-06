package cj.studio.ecm.net.rio.http;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Map.Entry;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.frame.IFeedback;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.ICablePin;
import cj.studio.ecm.graph.ICableWireCreateCallback;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.graph.IWirePin;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.net.FeedbackHelper;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.nio.netty.http.HttpChunkedSink;
import cj.studio.ecm.net.nio.netty.http.HttpException;
import cj.studio.ecm.net.nio.netty.http.SenderFrameHelper;
import cj.studio.ecm.net.nio.netty.http.WebsocketException;
import cj.studio.ecm.net.web.HttpCircuit;
import cj.studio.ecm.net.web.HttpFrame;
import cj.ultimate.util.StringUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

//将httpbinder的实现考过来
public class HttpServerHandler extends SimpleChannelInboundHandler<Object> implements NetConstans {
	public final String DEFAULT_WS_PATH = "/wssite";
	private String socketPath;
	private WebSocketServerHandshaker handshaker;
	ILogging log;
	private String serverName;
	ICablePin input;
	ICablePin output;
	private boolean isCompatibleHttp;
	SenderFrameHelper helper;

	public HttpServerHandler(HttpCjNetServer server, String isCompatibleHttp, String wsPath) {
		this.isCompatibleHttp = StringUtil.isEmpty(isCompatibleHttp) ? true : Boolean.valueOf(isCompatibleHttp);
		this.socketPath = StringUtil.isEmpty(wsPath) ? DEFAULT_WS_PATH : wsPath;
		this.input = (ICablePin) server.buildNetGraph().netInput();
		this.output = (ICablePin) server.buildNetGraph().netOutput();
		log = CJSystem.current().environment().logging();
		helper = new SenderFrameHelper();
		this.serverName = server.netName();
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
		try {
			if (msg instanceof FullHttpRequest) {// 当使用HttpObjectAggregator类时请求会包装为FullHttpRequest
				handleHttpRequest(ctx, (FullHttpRequest) msg);
			} else if (msg instanceof WebSocketFrame) {
				handleWebSocketFrame(ctx, (WebSocketFrame) msg);
			}
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	// 注：代码是基于Netty的
	private boolean isWebSocketReq(HttpRequest req) {
		if (req.headers().get("Connection") == null)
			return false;
		return ((req.headers().get("Connection").contains("Upgrade"))
				&& "websocket".equalsIgnoreCase(req.headers().get("Upgrade")));
	}

	private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
		if (!req.getDecoderResult().isSuccess()) {
			helper.sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
			return;
		}
		// Allow only GET methods.
		if (!req.getMethod().equals(GET) && !req.getMethod().equals(HttpMethod.POST)
				&& !req.getMethod().equals(HttpMethod.PUT) && !req.getMethod().name().toLowerCase().equals("part")) {
			helper.sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN));
			return;
		}
		if ("/favicon.ico".equals(req.getUri())) {
			FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.NOT_FOUND);
			helper.sendHttpResponse(ctx, req, res);
			return;
		}
		// String pn = req.getProtocolVersion().protocolName().toLowerCase();
		if (isCompatibleHttp && (!isWebSocketReq(req))) {
			flowHttpRequest(ctx, req);// 如果是http请求则不能执行下面的握手协议，否则报missing错误
			return;
		}
		String relwsPath = getWebSocketLocation(req);
		String wsLocation = "ws://" + req.headers().get(HOST) + relwsPath;
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(wsLocation, null, false);
		handshaker = wsFactory.newHandshaker(req);
		if (handshaker == null) {
			WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
		} else {
			ChannelFuture cf = handshaker.handshake(ctx.channel(), req);
			cf.addListener(new GenericFutureListener<Future<? super Void>>() {
				public void operationComplete(Future<? super Void> future) throws Exception {
					if (!future.isSuccess()) {
						ctx.fireExceptionCaught(future.cause());
					} else {
						ctx.fireUserEventTriggered(
								WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE);
						try {
							flowWebsocketOpened(ctx, req);
						} catch (Exception e) {
							errorCaught(ctx, e);
						}
					}

				}
			});

		}
	}

	private void initCircuit(HttpCircuit circuit, ChannelHandlerContext ctx, String type) {
		circuit.attribute("transfer-protocol", "net/1.1");
		circuit.attribute("select-type", "server");
		circuit.attribute("local-address", ctx.channel().localAddress().toString());
		circuit.attribute("remote-address", ctx.channel().remoteAddress().toString());
		circuit.attribute("select-name", serverName);
		circuit.attribute("select-simple", type);
		circuit.attribute("select-id", ctx.channel().id().asShortText());
		circuit.attribute("websocket-path", socketPath);
		circuit.attribute("isCompatibleHttp", isCompatibleHttp);
	}

	private void copyToFrame(HttpRequest req, HttpFrame frame) {
		HttpHeaders headers = req.headers();
		// 拷贝head
		Iterator<Entry<String, String>> it = headers.entries().iterator();
		while (it.hasNext()) {
			Entry<String, String> e = it.next();
			frame.head(e.getKey(), e.getValue());
		}
		if (req instanceof DefaultFullHttpRequest) {
			DefaultFullHttpRequest request = (DefaultFullHttpRequest) req;
			if (request.content().readableBytes() > 0) {
				frame.content().writeBytes(request.content());
			}
		} else {
			DefaultHttpRequest dr = (DefaultHttpRequest) req;
			System.out.println("-----xxx:" + dr.getDecoderResult().isFinished());
			// throw new HttpException("暂不知道咋弄");
		}
		frame.head("url", req.getUri());
	}

	public void httpServerFeedback(ChannelHandlerContext ctx, Frame mainFrame, Circuit mainCircuit) {
		mainCircuit.feedbackSetSource(IFeedback.KEY_OUTPUT_FEEDBACK);
		final ISink end = new HttpChunkedSink();
		IPlug plug = mainCircuit.feedback(IFeedback.KEY_OUTPUT_FEEDBACK).plugSink(IFeedback.KEY_ENDSINK_FEEDBACK, end);
		plug.option("mainCircuit", mainCircuit);
		plug.option("mainFrame", mainFrame);
		plug.option("ctx", ctx);

	}


	private void flowHttpRequest(ChannelHandlerContext ctx, HttpRequest req) {
		HttpCircuit circuit = new HttpCircuit("http/1.1 200 ok");
		String url = "";
		try {
			url = URLDecoder.decode(req.getUri(), "utf-8");
		} catch (UnsupportedEncodingException e1) {
		}
		// 上下文要重写 /?swsid=2323不用重写 /dd?swsid=32323要重写 /dd 要重写
		//indexOf比正则要快
//		String str = "/e?swsid=32323&s=3";
		int secondpos = url.indexOf("/", 1);
		if (url.length() != 1 && !url.startsWith("/?") && secondpos == -1) {
			String cpath = "";
			String q = "";
			int qpos = url.indexOf("?", 1);
			if (qpos > -1) {
				cpath = url.substring(1, qpos);
				q = url.substring(qpos, url.length());
			} else {
				cpath = url.substring(1, url.length());
			}
			if (!"".equals(cpath) && cpath.indexOf(".") == -1) {
				int jhpos = cpath.lastIndexOf("#");
				String jh = "";
				if (jhpos > -1) {
					jh = cpath.substring(jhpos, cpath.length());
					cpath = cpath.substring(0, jhpos);
				}
				String rerul = String.format("/%s%s/%s", cpath, jh, q);
				DefaultFullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
						new HttpResponseStatus(301, "to redirect because url is not slant"));
				String fullUrl = String.format("%s://%s%s", req.getProtocolVersion().protocolName().toLowerCase(),
						req.headers().get("Host"), rerul);
				res.headers().set("Location", fullUrl);
				setContentLength(res, res.content().readableBytes());
				if (StringUtil.isEmpty(res.headers().get("Content-Type")))
					res.headers().add("Content-Type", "text/html; charset=utf-8");
				// Send the response and close the connection if necessary.
				ChannelFuture f = ctx.channel().writeAndFlush(res);
				boolean isKeepAlive = req == null ? false : isKeepAlive(req);
				if (!isKeepAlive || res.getStatus().code() != 200) {
					f.addListener(ChannelFutureListener.CLOSE);
				}
				return;
			}
		}

		HttpFrame http = new HttpFrame(String.format("%s %s http/1.1", req.getMethod(), url));
		try {
			copyToFrame(req, http);
			initCircuit(circuit, ctx, "http");
			circuit.piggybacking(true);// http协议一定捎带

			httpServerFeedback(ctx, http, circuit);

			output.flow(ctx.channel().id().asShortText(), http, circuit);

			helper.sendHttpMsg(ctx, req, circuit, input);

			if ("disconnect".equals(circuit.attribute("net-action"))) {
				ctx.close();
			}

		} catch (Exception e) {
			if (e instanceof CircuitException) {
				CircuitException ce = (CircuitException) e;
				if ("404".equals(ce.getStatus())) {
					log.error(getClass(), String.format("%s %s", ce.getStatus(), e.getMessage()));
				} else {
					log.error(getClass(), e);
				}
			} else {
				log.error(getClass(), e);
			}
			throw new HttpException(e);
		} finally {
			if (http != null)
				http.dispose();
			circuit.dispose();
		}
	}

	private void flowWebsocketRequest(ChannelHandlerContext ctx, WebSocketFrame req) {
		HttpCircuit circuit = new HttpCircuit("http/1.1 200 ok");
		HttpFrame ws = null;
		try {
			AttributeKey<String> chipname = AttributeKey.valueOf("contentPath");
			String cname = ctx.attr(chipname).get();
			AttributeKey<String> cookie = AttributeKey.valueOf("Cookie");
			String cookieStr = ctx.attr(cookie).get();
			AttributeKey<String> host = AttributeKey.valueOf("Host");
			String hostStr = ctx.attr(host).get();
			// AttributeKey<String> cjtoken = AttributeKey
			// .valueOf("cjtoken");
			// String sid = ctx.attr(cjtoken).get();

			byte[] b = new byte[req.content().readableBytes()];
			req.content().readBytes(b);

			try {
				ws = new HttpFrame(b);
				if (!StringUtil.isEmpty(ws.protocol())) {// 是侦
					// 使用ctx的属性与flowws共享信息。
					String url = String.format("%s/%s", cname, ws.url()).replace("//", "/");
					ws.head("url", url);
					ws.head("Host", hostStr);
					ws.head("sub-protocol", "ws");
				}
			} catch (Exception e) {
			}
			if (ws == null || StringUtil.isEmpty(ws.protocol())) {// 不是侦
				String line = String.format("flow %s/ http/1.1", cname);
				ws = new HttpFrame(line);
				ws.head("sub-protocol", "unknown");
				ws.content().writeBytes(b);
			}
			if (!StringUtil.isEmpty(cookieStr)) {
				ws.head("Cookie", cookieStr);
			}

			FeedbackHelper.wsServerFeedback(ctx, ws, circuit);// 主动无限制次数回馈机制实现。
			initCircuit(circuit, ctx, "ws");
			if (ws.containsHead(FRAME_HEADKEY_CIRCUIT_SYNC)) {
				if (!ws.containsHead(FRAME_HEADKEY_CIRCUIT_SYNC_TIMEOUT)) {
					ws.head(FRAME_HEADKEY_CIRCUIT_SYNC_TIMEOUT, "3600");
				}
				circuit.piggybacking(true);
			}

			output.flow(ctx.channel().id().asShortText(), ws, circuit);
			if (circuit.isPiggybacking()) {// 捎带信息
				Frame back = circuit.snapshot("piggyback / net/1.1");
				helper.sendWsMsg(ctx.channel(), back);
				if ("disconnect".equals(circuit.attribute("net-action"))) {
					ctx.close();
				}
			}

		} catch (Exception e) {
			if (e instanceof CircuitException) {
				CircuitException ce = (CircuitException) e;
				if ("404".equals(ce.getStatus())) {
					log.error(getClass(), String.format("%s %s", ce.getStatus(), e.getMessage()));
				} else {
					log.error(getClass(), e);
				}
			} else {
				log.error(getClass(), e);
			}
			throw new WebsocketException(e);
		} finally {
			if (ws != null) {
				ws.dispose();
			}
			circuit.dispose();
		}
	}

	private void flowWebsocketOpened(ChannelHandlerContext ctx, FullHttpRequest req) {
		HttpFrame frame = new HttpFrame("connect " + req.getUri() + " net/1.1");
		HttpCircuit circuit = new HttpCircuit("net/1.1 200 ok");

		try {
			AttributeKey<String> cookie = AttributeKey.valueOf("Cookie");
			ctx.attr(cookie).set(req.headers().get("Cookie"));
			AttributeKey<String> chipname = AttributeKey.valueOf("contentPath");
			ctx.attr(chipname).set(contentPath(req.getUri()));
			AttributeKey<String> host = AttributeKey.valueOf("Host");
			ctx.attr(host).set(req.headers().get("Host"));
			// AttributeKey<String> cjtoken = AttributeKey.valueOf("cjtoken");

			initCircuit(circuit, ctx, "ws");
			// 要把请求中的cookie设给侦，以供用户开发会话
			copyToFrame(req, frame);
			frame.head("sub-protocol", "ws");

			output.flow(ctx.channel().id().asShortText(), frame, circuit);

		} catch (Throwable e) {
			if (e instanceof NullPointerException) {
				log.error(getClass(), e);
			} else {
				log.error(getClass(), e.getMessage());
			}
			throw new WebsocketException(e);
		} finally {
			frame.dispose();
			circuit.dispose();
		}

	}

	private void flowWebsocketClosed(ChannelHandlerContext ctx, WebSocketFrame req) {
		HttpFrame frame = new HttpFrame("disconnect / net/1.1");
		HttpCircuit circuit = new HttpCircuit("net/1.1 200 ok");
		initCircuit(circuit, ctx, "ws");
		try {
			frame.head("sub-protocol", "ws");
			output.flow(ctx.channel().id().asShortText(), frame, circuit);

		} catch (Exception e) {
			if (e instanceof NullPointerException) {
				log.error(getClass(), e);
			} else {
				log.error(getClass(), e.getMessage());
			}
			throw new WebsocketException(e);
		} finally {
			frame.dispose();
			circuit.dispose();
		}

	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		HttpFrame frame = new HttpFrame("connect / net/1.1");
		HttpCircuit circuit = new HttpCircuit("net/1.1 200 ok");
		initCircuit(circuit, ctx, "http");
		String id = ctx.channel().id().asShortText();
		try {
			output.newWire(id);
			input.newWire(id, new ICableWireCreateCallback() {

				@Override
				public void createdWire(String name, IWirePin wire) {
					IPlug plug = wire.getPlug("input#http");
					plug.option("channel", ctx.channel());
				}
			});
			output.flow(id, frame, circuit);
		} catch (Exception e) {
			if (e instanceof NullPointerException) {
				log.error(getClass(), e);
			} else {
				log.error(getClass(), e.getMessage());
			}
			throw new HttpException(e);
		} finally {
			frame.dispose();
			circuit.dispose();
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		HttpFrame frame = new HttpFrame("disconnect / net/1.1");
		HttpCircuit circuit = new HttpCircuit("net/1.1 200 ok");
		initCircuit(circuit, ctx, "http");
		String id = ctx.channel().id().asShortText();
		try {

			output.flow(id, frame, circuit);
		} catch (Exception e) {
			if (e instanceof NullPointerException) {
				log.error(getClass(), e);
			} else {
				log.error(getClass(), e.getMessage());
			}
			throw new HttpException(e);
		} finally {
			frame.dispose();
			circuit.dispose();
			output.removeWire(id);
			input.removeWire(id);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		errorCaught(ctx, cause);
	}

	private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {

		// Check for closing frame
		if (frame instanceof CloseWebSocketFrame) {
			handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
			flowWebsocketClosed(ctx, frame);
			// System.out.println("close websocket");
			return;
		}
		if (frame instanceof PingWebSocketFrame) {
			ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
			return;
		}
		if (!(frame instanceof TextWebSocketFrame)) {
			throw new UnsupportedOperationException(
					String.format("%s frame types not supported", frame.getClass().getName()));
		}
		flowWebsocketRequest(ctx, frame);

	}

	private void errorWebsocketCaught(ChannelHandlerContext ctx, Throwable cause) {
		Throwable e = null;
		e = CircuitException.search(cause);
		if (e == null) {
			e = cause;
		}
		// WebSocketFrame f = null;
		Frame frame = new Frame("report /error ws/1.0");
		;
		if (e instanceof CircuitException) {
			CircuitException ce = (CircuitException) e;

			frame.head("message", ce.getMessage().replaceAll("\r", "").replace("\n", ""));
			frame.head("status", ce.getStatus());
			String error = ce.messageCause();
			frame.content().writeBytes(error.getBytes());
			// f = new TextWebSocketFrame(error);
		} else {
			// String error = "503 " + e.getMessage() + "";
			// f = new TextWebSocketFrame(error);
			frame.head("message", e.getMessage().replaceAll("\r", "").replace("\n", ""));
			frame.head("status", "503");
		}
		ctx.writeAndFlush(frame.toByteBuf());
	}

	protected void errorCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (cause instanceof WebsocketException) {
			errorWebsocketCaught(ctx, cause);
			return;
		}
		Throwable e = null;
		e = CircuitException.search(cause);
		if (e == null) {
			e = cause;
		}
		if (e instanceof CircuitException) {
			String msg = e.getMessage().replace("\r", "").replace("\n", "<br/>");// netty的HttpResponseStatus构造对\r\n作了错误异常
			HttpResponseStatus status = new HttpResponseStatus(Integer.parseInt(((CircuitException) e).getStatus()),
					msg);
			FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, status);
			String err = "";
			if (status.equals(HttpResponseStatus.NOT_FOUND)) {
				err = e.getMessage();
			} else {
				err = ((CircuitException) e).messageCause();
			}
			err = err.replaceAll("\r\n", "<br/>");
			res.content().writeBytes(err.getBytes());
			// 注意：对于一些http状态号，浏览器不解析，比如204,浏览器得到此状态码后什么也不做。因此要想将错误呈现到界面上，需依据http协议的状态码定义。
			helper.sendHttpResponse(ctx, null, res);
		} else {
			String error = e.getMessage().replace("\r", "").replace("\n", "<br/>");
			HttpResponseStatus status = new HttpResponseStatus(503, error);
			FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, status);
			helper.sendHttpResponse(ctx, null, res);
		}

	}

	private String contentPath(String uri) {
		String path = uri.startsWith("/") ? uri : ("/" + uri);
		if (path.length() == 1)
			return path;
		int nextSp = path.indexOf("/", 1);
		if (nextSp < 0) {
			return path;
		}
		path = path.substring(0, nextSp);
		return path;
	}

	private String getWebSocketLocation(FullHttpRequest req) {
		try {
			String contentPath = contentPath(req.getUri());

			String path = contentPath + (socketPath.startsWith("/") ? socketPath : "/" + socketPath);
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			return path.replace("//", "/");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
}
