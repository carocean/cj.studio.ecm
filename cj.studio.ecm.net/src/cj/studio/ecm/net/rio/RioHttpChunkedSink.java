package cj.studio.ecm.net.rio;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.ultimate.util.StringUtil;
import io.netty.handler.codec.http.HttpHeaders;

public class RioHttpChunkedSink implements ISink {

	@Override
	public void flow(Frame frame, Circuit circuit, IPlug plug)
			throws CircuitException {
		// 采用在主回路中捎带而非主动回发，这是因为netty的功能限制
		if ("CHUNKED/1.0".equals(frame.protocol())) {
			Circuit mainCircuit = (Circuit) plug.option("mainCircuit");
			Frame mainFrame = (Frame) plug.option("mainFrame");
			if (mainFrame.containsHead("Connection")) {
				frame.head("Connection", mainFrame.head("Connection"));
			}
			if ("open".equals(frame.command())) {
				String mime = frame.contentType();// 在httpSink.setMime的方法中已设置mime类型
				if (StringUtil.isEmpty(mime)) {
					mime = "text/html";
				}
				
				frame.head(HttpHeaders.Names.CONTENT_TYPE.toString(),
						mime);
				frame.head(HttpHeaders.Names.TRANSFER_ENCODING.toString(),
						HttpHeaders.Values.CHUNKED.toString());
				if (!"keep-alive".equals(mainFrame.head("Connection"))) {
					frame.head(HttpHeaders.Names.CONNECTION.toString(),
							HttpHeaders.Values.KEEP_ALIVE.toString());
				}
				mainCircuit.content().writeBytes(frame.toByteBuf());
			} else if ("close".equals(frame.command())) {
				if (!"keep-alive".equals(mainFrame.head("Connection"))) {
					frame.head(HttpHeaders.Names.CONNECTION.toString(),
							HttpHeaders.Values.KEEP_ALIVE.toString());
				}
				mainCircuit.content().writeBytes(frame.toByteBuf());
			} else {
				mainCircuit.content().writeBytes(frame.toByteBuf());
			}
			mainCircuit.contentType("frame/bin");
			mainCircuit.piggybacking(true);
			return;
		}
		plug.flow(frame, circuit);
	}

}
