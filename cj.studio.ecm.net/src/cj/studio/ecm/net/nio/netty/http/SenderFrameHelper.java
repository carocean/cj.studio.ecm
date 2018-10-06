package cj.studio.ecm.net.nio.netty.http;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.frame.IFeedback;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPin;
import cj.ultimate.util.StringUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class SenderFrameHelper {
	public void sendWsMsg(Channel ch, Frame frame) {
		TextWebSocketFrame f = null;
		if (!frame.containsHead("content-chartset"))
			frame.contentChartset("utf-8");
		f = new TextWebSocketFrame(frame.toByteBuf());
		// if ("frame/json".equals(frame.contentType())) {
		// f = new TextWebSocketFrame(frame.toByteBuf());
		// } else if ("frame/bin".equals(frame.contentType())) {
		// f = new TextWebSocketFrame(frame.toByteBuf());
		// } else {//    否则按二进制传
		// f = new TextWebSocketFrame(frame.toByteBuf());
		//// if (frame.content().readableBytes() > 0) {
		//// frame.contentType("text/bin");
		//// f = new TextWebSocketFrame(frame.content().raw());
		//// } else {
		//// frame.contentType("frame/bin");
		//// f = new TextWebSocketFrame(frame.toByteBuf());
		//// }
		// }
		ch.writeAndFlush(f);
	}

	public void sendHttpMsg(ChannelHandlerContext ctx, HttpRequest req,
			Circuit circuit, IPin out) {
		if (!circuit.isPiggybacking()) {
			return;
		}
		if (circuit.containsHead(HttpHeaders.Names.CONTENT_TYPE.toString())) {
			String v = circuit.contentType();
			if ("frame/bin".equals(v) || "frame/json".equals(v)) {
				byte[] b = circuit.content().readFully();
				Frame f = new Frame(b);
				String current_State = circuit.status();
				String current_cause = "";
				boolean current_error = !"200".equals(current_State);
				if (current_error) {// 如果当前回路也出错了。则将错误信息拼在其后。
					current_cause = circuit.cause();
					circuit.cause(String.format("%s\r\n\r\n链路第一节报错：\r\n%s",
							circuit.cause(), current_cause));
				} 
				circuit = new Circuit(f);
			}
		}
		String statusStr = circuit.status();
		if (StringUtil.isEmpty(statusStr)) {
			statusStr = "503";
		}
		int intstate = Integer.parseInt(statusStr);
		HttpResponseStatus status = new HttpResponseStatus(intstate,
				circuit.message().replace("\r", "").replace("\n", "<br/>"));
		FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, status);
		if (intstate >= 200 && intstate < 400) {
			// 设置cookie
			if (circuit.containsHead(HttpHeaders.Names.SET_COOKIE.toString())) {
				String v = circuit
						.head(HttpHeaders.Names.SET_COOKIE.toString());
				res.headers().set(HttpHeaders.Names.SET_COOKIE.toString(), v);
			}
			if (circuit
					.containsHead(HttpHeaders.Names.CONTENT_TYPE.toString())) {
				String v = circuit
						.head(HttpHeaders.Names.CONTENT_TYPE.toString());
				res.headers().set(HttpHeaders.Names.CONTENT_TYPE.toString(), v);
			}
			// if
			// (circuit.containsHead(HttpHeaders.Names.CONTENT_TYPE.toString()))
			// {
			// String v = circuit.head(HttpHeaders.Names.CONTENT_TYPE
			// .toString());
			// res.headers().set(HttpHeaders.Names.CONTENT_TYPE.toString(), v);
			// }
			if (circuit.containsHead(
					HttpHeaders.Names.CONTENT_ENCODING.toString())) {
				String v = circuit
						.head(HttpHeaders.Names.CONTENT_ENCODING.toString());
				res.headers().set(HttpHeaders.Names.CONTENT_ENCODING.toString(),
						v);
			}
			if (intstate == 302 || intstate == 301) {
				String v = circuit.head("Location");
				res.headers().set("Location", v);
				res.content().writeBytes(circuit.content().copy());
			}
			res.content().writeBytes(circuit.content().copy());
		} else {
			if ("true".equals(circuit.attribute("error-custom"))) {
				// 设置cookie
				// if (circuit.containsHead(HttpHeaders.Names.SET_COOKIE
				// .toString())) {
				// String v = circuit.head(HttpHeaders.Names.SET_COOKIE
				// .toString());
				// res.headers().set(HttpHeaders.Names.SET_COOKIE.toString(),
				// v);
				// }
				res.content().writeBytes(circuit.content().copy());
			} else {
				circuit.content().clear();
				String error = String.format("%s %s\r\n", circuit.status(),
						circuit.message(), circuit.message());
				res.content().writeBytes(error.getBytes());
			}
		}
		sendHttpResponse(ctx, req, res);
	}

	/**
	 * 注意：对于一些http状态号，浏览器不解析，比如204,浏览器得到此状态码后什么也不做。因此要想将错误呈现到界面上，需依据http协议的状态码定义
	 * 。
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param ctx
	 * @param req
	 * @param res
	 */
	public void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req,
			FullHttpResponse res) {
		// Generate an error page if response getStatus code is not OK (200).
		// if (res.getStatus().code() != 200) {
		// ByteBuf buf = Unpooled.copiedBuffer(
		// res.getStatus().toString()
		// + "<br><br>"
		// + (content == null ? "" : content.replace("\r\n",
		// "<br>")), CharsetUtil.UTF_8);
		// res.content().clear();
		// res.content().writeBytes(buf);
		// buf.release();
		// setContentLength(res, res.content().readableBytes());
		// } else {
		// 必须为上下文设定发送的长度，否则浏览器悬停等待。
		setContentLength(res, res.content().readableBytes());
		// }
		if (StringUtil.isEmpty(res.headers().get("Content-Type")))
			res.headers().add("Content-Type", "text/html; charset=utf-8");
		// Send the response and close the connection if necessary.
		ChannelFuture f = ctx.channel().writeAndFlush(res);
		boolean isKeepAlive = req == null ? false : isKeepAlive(req);
		if (!isKeepAlive || res.getStatus().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}
}
