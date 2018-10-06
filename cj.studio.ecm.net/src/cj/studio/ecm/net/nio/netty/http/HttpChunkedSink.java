package cj.studio.ecm.net.nio.netty.http;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.ultimate.util.StringUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

public class HttpChunkedSink implements ISink {

	@Override
	public void flow(Frame frame, Circuit circuit, IPlug plug)
			throws CircuitException {
		if ("CHUNKED/1.0".equals(frame.protocol())) {
			// throw new CircuitException("505",
			// "http晌应回馈回路只支持chuncked/1.0协议");
			// }
			Circuit mainCircuit = (Circuit) plug.option("mainCircuit");
			Frame mainFrame = (Frame) plug.option("mainFrame");
			ChannelHandlerContext ctx = (ChannelHandlerContext) plug
					.option("ctx");
			if (mainFrame.containsHead("Connection")) {
				frame.head("Connection", mainFrame.head("Connection"));
			}
			if ("open".equals(frame.command())) {
				HttpResponse resp = new DefaultHttpResponse(
						HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
				String mime = frame.contentType();// 在httpSink.setMime的方法中已设置mime类型
				if (StringUtil.isEmpty(mime)) {
					mime = "text/html";
				}
				resp.headers().set(HttpHeaders.Names.CONTENT_TYPE, mime);
				resp.headers().set(HttpHeaders.Names.TRANSFER_ENCODING,
						HttpHeaders.Values.CHUNKED);
				if (!"keep-alive".equals(mainFrame.head("Connection"))) {
					resp.headers().set(HttpHeaders.Names.CONNECTION,
							HttpHeaders.Values.KEEP_ALIVE);
				}
				ctx.writeAndFlush(resp);
				mainCircuit.piggybacking(false);
				return;
			} else if ("close".equals(frame.command())) {
				LastHttpContent last = LastHttpContent.EMPTY_LAST_CONTENT;// new
																			// DefaultLastHttpContent();
				ChannelFuture lastContentFuture = ctx.writeAndFlush(last);
				// Decide whether to close the connection or not.
				if (!"keep-alive".equals(mainFrame.head("Connection"))) {
					// Close the connection when the whole content is written
					// out.
					lastContentFuture.addListener(ChannelFutureListener.CLOSE);
				}
				mainCircuit.piggybacking(false);
				return;
			} else {
				DefaultHttpContent backmsg = new DefaultHttpContent(
						frame.content().raw());
				ctx.writeAndFlush(backmsg);
			}
			return;
		}
		plug.flow(frame, circuit);
	}

}