package cj.studio.ecm.net.nio.netty.http;

import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Map.Entry;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.frame.IFeedback;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.net.FeedbackHelper;
import cj.studio.ecm.net.graph.IHandleBinder;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.nio.netty.INettyGraph;
import cj.studio.ecm.net.web.HttpCircuit;
import cj.studio.ecm.net.web.HttpFrame;
import cj.ultimate.util.StringUtil;
import io.netty.channel.ChannelFuture;
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

/**
 * netty中有两种实现websocket的方式，一种是兼容http协议方式（因为要处理http请求的html页面）；一种是不兼容http，
 * 直接就是用websocket。如果不需要兼容，最好用第二种，因为它只处理websocket，所以性能好。
 * 本例是第一种实现。此种实现即可向客户端写html文件，也可处理websocket请求。要了解详情可参见netty simples例程。
 */
@CjService(name = "httpHandle", scope = Scope.multiton)
public class HttpHandlerBinder extends SimpleChannelInboundHandler<Object>
		implements IHandleBinder, NetConstans {
	public final String DEFAULT_WS_PATH = "/wssite";
	private String socketPath;
	private WebSocketServerHandshaker handshaker;
	ILogging log;
	private String serverName;
	IPin acceptpin;
	IPin senderIn;
	private boolean isCompatibleHttp;
	SenderFrameHelper helper;
	@Override
	public void init(INettyGraph g) {
		this.acceptpin = g.newAcceptorPin();
		this.senderIn = g.in("senderIn");
		log = CJSystem.current().environment().logging();
		helper=new SenderFrameHelper();
	}

	public HttpHandlerBinder(String serverName, boolean isCompatibleHttp) {
		this.serverName = serverName;
		this.isCompatibleHttp = isCompatibleHttp;
	}

	public void setWsPath(String path) {
		socketPath = StringUtil.isEmpty(path) ? DEFAULT_WS_PATH : path;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		// 以下用来跳转http和websocket请求的处理。
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
	// 以下代码已在MyHttpObjectAggregator实现

	// 当使用非HttpObjectAggregator类时请求会产生多次事件，像formData巨大文件上传都是分次多次处理，因此可采用：
	// circuit的回馈机制将这些多次处理事件发给程序处理，让开发者去实现如何合并大文件。
	// 因此此处不用netty的upload例程中那样的机制，也不用它的api
	/*
	 * HttpObjectAggregator只是聚合到内存了，我可以采用一次请求，输出给程序，而后续的块则通过circuit的回馈通道多次的通知给程序
	 * 这可以保证程序对api访问的一致性，但一样无法改变大文件传输与小文件转输要使用两套httpserver的现实，
	 * 因为netty的pipeline无法动态改变注册的handler,如果用了HttpObjectAggregator就不能用块的方式，它提前把块都聚合了。
	 * HttpObjectAggregator为什么没留个接口扩展呢？也可以考虑派生它试试，如果可以就不必在此实现了，而通过派生HttpObjectAggregator
	 * 将后续的块通过回路回馈通道传出即可，这样可做到与HttpObjectAggregator的兼容而能采用同一个httpserver实现大、小文件的传输
	 * 
	 * 因此，先研究能否通过派生HttpObjectAggregator实现。如果能则去掉该方法
	 */
	// private void handleDefaultHttpRequest(ChannelHandlerContext ctx,
	// Object msg) {
	//
	// }

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx)
			throws Exception {
		ctx.flush();
	}

	// 注：代码是基于Netty的
	private boolean isWebSocketReq(HttpRequest req) {
		if (req.headers().get("Connection") == null)
			return false;
		return ((req.headers().get("Connection").contains("Upgrade"))
				&& "websocket".equalsIgnoreCase(req.headers().get("Upgrade")));
	}

	private void handleHttpRequest(ChannelHandlerContext ctx,
			FullHttpRequest req) throws Exception {
		// System.out.println("this time:" + ctx);
		// Handle a bad request.
		if (!req.getDecoderResult().isSuccess()) {
			helper.sendHttpResponse(ctx, req,
					new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
			return;
		}
		// client.channel().writeAndFlush(req).sync();
		// Allow only GET methods.
		if (!req.getMethod().equals(GET)
				&& !req.getMethod().equals(HttpMethod.POST)
				&& !req.getMethod().equals(HttpMethod.PUT)
				&& !req.getMethod().name().toLowerCase().equals("part")) {
			helper.sendHttpResponse(ctx, req,
					new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN));
			return;
		}
		// /什么是favicon.ico？它是指用于作为缩略图的网站标志,主要显示位于浏览器的地址栏或者在标签上,用于显示网站的专属性。由浏览器自动发起。
		// 它的发起不从属于任何web应用上下文，实际上用于标识web容器本身。当然这是默认情况下，也可自定义。
		// 而且打开一次浏览器只加载一次。
		if ("/favicon.ico".equals(req.getUri())) {
			FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1,
					HttpResponseStatus.NOT_FOUND);
			helper.sendHttpResponse(ctx, req, res);
			return;
		}
		// String pn = req.getProtocolVersion().protocolName().toLowerCase();
		if (isCompatibleHttp && (!isWebSocketReq(req))) {
			flowHttpRequest(ctx, req);// 如果是http请求则不能执行下面的握手协议，否则报missing错误
			return;
		}
		// if ("/".equals(req.getUri())) {
		// ByteBuf content = WebSocketServerIndexPage
		// .getContent(getWebSocketLocation(req));
		// FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK,
		// content);
		// res.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
		// setContentLength(res, content.readableBytes());
		//
		// sendHttpResponse(ctx, req, res);
		// return;
		// }

		// Handshake，下面代码是实现websocket协议的代码，如果没有这段，则客户端打不开websocket连接。
		// 每次客户端的请求，只要上面不return下面的代码均会执行，只有符合getWebSocketLocation(req)地理匹配的请求，才会被视为是websocket请求，才会被跳到handleWebSocketFrame方法中处理
		// 因此，每次请求不论是http还是websocket，均会执行上面的代码，一定不能将getWebSocketLocation(req)地址在前面拦截了，否则websocket不能处理。
		// 因此，如果前面的代码过多，会影响websocket的性能，因此建议将http和websocket服务单独部署，并为websocket实现更简的handle
		String relwsPath = getWebSocketLocation(req);
		String wsLocation = "ws://" + req.headers().get(HOST) + relwsPath;
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
				wsLocation, null, false);
		handshaker = wsFactory.newHandshaker(req);
		if (handshaker == null) {
			WebSocketServerHandshakerFactory
					.sendUnsupportedWebSocketVersionResponse(ctx.channel());
		} else {
			ChannelFuture cf = handshaker.handshake(ctx.channel(), req);
			cf.addListener(new GenericFutureListener<Future<? super Void>>() {
				public void operationComplete(Future<? super Void> future)
						throws Exception {
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

	private void initCircuit(HttpCircuit circuit, ChannelHandlerContext ctx,
			String type) {
		circuit.attribute("transfer-protocol", "net/1.1");
		circuit.attribute("select-type", "server");
		circuit.attribute("local-address",
				ctx.channel().localAddress().toString());
		circuit.attribute("remote-address",
				ctx.channel().remoteAddress().toString());
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
			System.out
					.println("-----xxx:" + dr.getDecoderResult().isFinished());
			// throw new HttpException("暂不知道咋弄");
		}
		frame.head("url", req.getUri());
	}

	// 无/结尾的目录，则使浏览器重发请求，则发起重写请求
	// private boolean checkIsDirOfNoSlant(Frame frame, Circuit c) {
	// if (!frame.isDirectory())
	// return false;
	// String url = frame.url();
	// if (url.endsWith("/"))
	// return false;
	//
	// String relativedUrl = frame.relativePath();
	// c.status("302");
	// c.message("redirectByDirOfNoSlant");
	// if (!relativedUrl.startsWith("/")) {
	// relativedUrl = "/" + relativedUrl;
	// }
	// if (!relativedUrl.endsWith("/"))
	// relativedUrl = relativedUrl + "/";
	// String fullUrl = String.format("%s://%s/%s%s",
	// frame.protocol().replace("/1.1", ""), frame.head("Host"),
	// frame.rootName(), relativedUrl);
	// c.head("Location", fullUrl);
	// return true;
	// }
	public void httpServerFeedback(ChannelHandlerContext ctx,
			Frame mainFrame, Circuit mainCircuit) {
		mainCircuit.feedbackSetSource(IFeedback.KEY_OUTPUT_FEEDBACK);
		final ISink end = new HttpChunkedSink();
		IPlug plug = mainCircuit.feedback(IFeedback.KEY_OUTPUT_FEEDBACK)
				.plugSink(IFeedback.KEY_ENDSINK_FEEDBACK, end);
		plug.option("mainCircuit", mainCircuit);
		plug.option("mainFrame", mainFrame);
		plug.option("ctx", ctx);

	}
	private void flowHttpRequest(ChannelHandlerContext ctx, HttpRequest req) {
		HttpCircuit circuit = new HttpCircuit("http/1.1 200 ok");
		try {
			String url = URLDecoder.decode(req.getUri(), "utf-8");
			/*
			 * 注意：Chrome浏览器对于客户端的#请求不在地址后加#
			 * > 在一个文档页面中，如果包含的资源地址指定是#时，请求到来不能视为文档，因为它是资源，如果未正确判断是资源请求，则会进入文档处理，导致文档被处理多次（请求地址后均有#)
			 * > 方法是WebUtil.documentMatch(frame, plug)方法中识别文档和资源类型，并以accept头信息判断
			 */
			// String positionAtPage = "";
			// if (!url.endsWith("/") && url.contains("#")) {
			// if (url.endsWith("#")) {
			// positionAtPage = "0";
			// } else {
			// positionAtPage = url.substring(url.indexOf("#"),
			// url.length());
			// }
			// url = url.substring(0, url.indexOf("#"));
			// }
			HttpFrame http = new HttpFrame(
					String.format("%s %s http/1.1", req.getMethod(), url));
			// http.head("Position-At-Page", positionAtPage);
			copyToFrame(req, http);
			initCircuit(circuit, ctx, "http");
			circuit.piggybacking(true);// http协议一定捎带
			// 用于按块写，对下载超大文件非常有用,在调用回馈回路前，需要在主回路中将响应侦编码设为块模式：
			// resp.setHeader(HttpHeaders.Names.TRANSFER_ENCODING,
			// HttpHeaders.Values.CHUNKED);
			httpServerFeedback(ctx, http, circuit);
			/*
			 * 
			 * 维护：
			 * 
			 * 1.修复了当请求地址后是目录但没有/的情况下，浏览器将之视为文件，此时浏览器将页面的静态资源路径解析为父路径，如果是网站的站点名，
			 * 则静态资源便没有上下文了，导致404.因此改为了让浏览器重定向地址，且后加/
			 */
			// if (!checkIsDirOfNoSlant(http, circuit)) {
			acceptpin.flow(http, circuit);
//			if (NetConstans.NEURON_CHUNKED
//					.equals(circuit.head(NetConstans.NEURON_CHUNKED))) {
//				doNeuronChunked(acceptpin, http, circuit);
//				return;
//			}
			// }
			// 因为浏览器不管捎不捎带每次请求必须有返回，否则悬停等待。
			// 同样如果有值回写，要设定内容长度，如果内容长度不匹配则浏览器视为还未接收完，将悬停待收。
			// 所以注释掉了下面。
			// if (circuit.isPiggybacking()) {// 捎带信息
			helper.sendHttpMsg(ctx, req, circuit,acceptpin);
			
			if ("disconnect".equals(circuit.attribute("net-action"))) {
				ctx.close();
			}
			// SenderFrameHelper.sendHttpMsg(ctx, req, circuit);
			// }
			http.dispose();
			circuit.dispose();
		} catch (Exception e) {
			if (e instanceof CircuitException) {
				CircuitException ce = (CircuitException) e;
				if ("404".equals(ce.getStatus())) {
					log.error(getClass(), String.format("%s %s", ce.getStatus(),
							e.getMessage()));
				} else {
					log.error(getClass(), e);
				}
			} else {
				log.error(getClass(), e);
			}
			throw new HttpException(e);
		} finally {
			circuit.dispose();
		}
	}

	// open / chunked/1.0
	// close /chunked/1.0
	// check the mime type
//	private void doNeuronChunked(IPin out, HttpFrame frame, HttpCircuit circuit)
//			throws CircuitException {
//		Frame back= new Frame(circuit.content().readFully());
//		// open / chunked/1.0
//		IFeedback fb = circuit.feedback(IFeedback.KEY_OUTPUT_FEEDBACK);
//		Frame f = new Frame("open / chunked/1.0");
//		f.contentType(back.contentType());
//		fb.doBack(f, circuit);
//		long read = 0;
//		for (int i = 0; i < Long.MAX_VALUE; i++) {
//			if ("true".equals(back.head("Neuron-Chunked-End"))) {
//				// close /chunked/1.0
//				f = new Frame("close / chunked/1.0");
//				f.contentType(back.contentType());
//				fb.doBack(f, circuit);
//				break;
//			}
//			frame.head("Neuron-Chunked-Num", String.valueOf(i));
//			frame.head("Neuron-Chunked-Range", String.format("%s-8192", read));// 已读长度－要求返回的长度
//			// 发出块请求
//			out.flow(f, circuit);
//			read += circuit.content().readableBytes();
//			// back out chunked
//			f = new Frame(circuit.content().readFully());
//			f.contentType(back.contentType());
//			fb.doBack(f, circuit);
//		}
//	}

	private void flowWebsocketRequest(ChannelHandlerContext ctx,
			WebSocketFrame req) {
		// if(req.content().readableBytes()<1)return ;//那怕是空的请求，好不容易发过来了，就算是吧。
		HttpCircuit circuit = new HttpCircuit("http/1.1 200 ok");
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
			HttpFrame ws = null;
			try {
				ws = new HttpFrame(b);
				if (!StringUtil.isEmpty(ws.protocol())) {// 是侦
					// 使用ctx的属性与flowws共享信息。
					String url = String.format("%s/%s", cname, ws.url())
							.replace("//", "/");
					ws.head("url", url);
					ws.head("Host",hostStr);
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
				// Set<Cookie> set =CookieHelper.decode(cookieStr);
				// set.add(new DefaultCookie("CJTOKEN", sid));
				// cookieStr = CookieHelper.encodeToString(set);
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
			// StringBuffer sb=new StringBuffer();
			// pin.print(sb, "\t\t\t\t");
			// System.out.println(sb.toString());
			acceptpin.flow(ws, circuit);
			if (circuit.isPiggybacking()) {// 捎带信息
				Frame back = circuit.snapshot("piggyback / net/1.1");
				helper.sendWsMsg(ctx.channel(), back);
				if ("disconnect".equals(circuit.attribute("net-action"))) {
					ctx.close();
				}
			}
			ws.dispose();
			circuit.dispose();
		} catch (Exception e) {
			if (e instanceof CircuitException) {
				CircuitException ce = (CircuitException) e;
				if ("404".equals(ce.getStatus())) {
					log.error(getClass(), String.format("%s %s", ce.getStatus(),
							e.getMessage()));
				} else {
					log.error(getClass(), e);
				}
			} else {
				log.error(getClass(), e);
			}
			throw new WebsocketException(e);
		} finally {
			circuit.dispose();
		}
	}

	private void flowWebsocketOpened(ChannelHandlerContext ctx,
			FullHttpRequest req) {
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
			circuit.attribute("ctx", ctx);
			senderIn.flow(frame, circuit);
			circuit.removeAttribute("ctx");
			// 要把请求中的cookie设给侦，以供用户开发会话
			copyToFrame(req, frame);
			frame.head("sub-protocol", "ws");
			// String sid = CookieHelper.cjtoken(frame);
			// ctx.attr(cjtoken).set(sid);

			acceptpin.flow(frame, circuit);

			frame.dispose();
		} catch (Throwable e) {
			if (e instanceof NullPointerException) {
				log.error(getClass(), e);
			} else {
				log.error(getClass(), e.getMessage());
			}
			throw new WebsocketException(e);
		} finally {
			circuit.dispose();
		}

	}

	private void flowWebsocketClosed(ChannelHandlerContext ctx,
			WebSocketFrame req) {
		HttpFrame frame = new HttpFrame("disconnect / net/1.1");
		HttpCircuit circuit = new HttpCircuit("net/1.1 200 ok");
		initCircuit(circuit, ctx, "ws");
		try {
			circuit.attribute("ctx", ctx);
			senderIn.flow(frame, circuit);
			circuit.removeAttribute("ctx");
			frame.head("sub-protocol", "ws");
			acceptpin.flow(frame, circuit);
			frame.dispose();
		} catch (Exception e) {
			if (e instanceof NullPointerException) {
				log.error(getClass(), e);
			} else {
				log.error(getClass(), e.getMessage());
			}
			throw new WebsocketException(e);
		} finally {
			circuit.dispose();
		}

	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		HttpFrame frame = new HttpFrame("connect / net/1.1");
		HttpCircuit circuit = new HttpCircuit("net/1.1 200 ok");
		initCircuit(circuit, ctx, "http");
		try {
			circuit.attribute("ctx", ctx);
			senderIn.flow(frame, circuit);
			circuit.removeAttribute("ctx");

			acceptpin.flow(frame, circuit);
			frame.dispose();
		} catch (Exception e) {
			if (e instanceof NullPointerException) {
				log.error(getClass(), e);
			} else {
				log.error(getClass(), e.getMessage());
			}
			throw new HttpException(e);
		} finally {
			circuit.dispose();
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		HttpFrame frame = new HttpFrame("disconnect / net/1.1");
		HttpCircuit circuit = new HttpCircuit("net/1.1 200 ok");
		initCircuit(circuit, ctx, "http");
		try {
			circuit.attribute("ctx", ctx);
			senderIn.flow(frame, circuit);
			circuit.removeAttribute("ctx");

			acceptpin.flow(frame, circuit);
			frame.dispose();
		} catch (Exception e) {
			if (e instanceof NullPointerException) {
				log.error(getClass(), e);
			} else {
				log.error(getClass(), e.getMessage());
			}
			throw new HttpException(e);
		} finally {
			circuit.dispose();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		errorCaught(ctx, cause);
	}

	private void handleWebSocketFrame(ChannelHandlerContext ctx,
			WebSocketFrame frame) {

		// Check for closing frame
		if (frame instanceof CloseWebSocketFrame) {
			handshaker.close(ctx.channel(),
					(CloseWebSocketFrame) frame.retain());
			flowWebsocketClosed(ctx, frame);
			// System.out.println("close websocket");
			return;
		}
		if (frame instanceof PingWebSocketFrame) {
			ctx.channel()
					.write(new PongWebSocketFrame(frame.content().retain()));
			return;
		}
		if (!(frame instanceof TextWebSocketFrame)) {
			throw new UnsupportedOperationException(
					String.format("%s frame types not supported",
							frame.getClass().getName()));
		}
		flowWebsocketRequest(ctx, frame);

	}

	private void errorWebsocketCaught(ChannelHandlerContext ctx,
			Throwable cause) {
		Throwable e = null;
		e = CircuitException.search(cause);
		if (e == null) {
			e = cause;
		}
//		WebSocketFrame f = null;
		Frame frame=new Frame("report /error ws/1.0");;
		if (e instanceof CircuitException) {
			CircuitException ce = (CircuitException) e;
			
			frame.head("message",ce.getMessage().replaceAll("\r", "").replace("\n", ""));
			frame.head("status",ce.getStatus());
			String error = ce.messageCause();
			frame.content().writeBytes(error.getBytes());
//			f = new TextWebSocketFrame(error);
		} else {
//			String error = "503 " + e.getMessage() + "";
//			f = new TextWebSocketFrame(error);
			frame.head("message",e.getMessage().replaceAll("\r", "").replace("\n", ""));
			frame.head("status","503");
		}
		ctx.writeAndFlush(frame.toByteBuf());
	}

	protected void errorCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
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
			String msg = e.getMessage().replace("\r", "").replace("\n",
					"<br/>");// netty的HttpResponseStatus构造对\r\n作了错误异常
			HttpResponseStatus status = new HttpResponseStatus(
					Integer.parseInt(((CircuitException) e).getStatus()), msg);
			FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1,
					status);
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
			String error = e.getMessage().replace("\r", "").replace("\n",
					"<br/>");
			HttpResponseStatus status = new HttpResponseStatus(503, error);
			FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1,
					status);
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

			String path = contentPath + (socketPath.startsWith("/") ? socketPath
					: "/" + socketPath);
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			return path.replace("//", "/");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

}
