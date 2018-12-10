package cj.studio.ecm.net.nio.netty.http;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.nio.IChannelPool;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.nio.TimeoutException;

public class HttpClientSender implements ISink {
	Channel ch;
	IChannelPool pool;

	private void genChannel() throws CircuitException {
		try {
			ch = pool.get();
		} catch (TimeoutException e) {
			throw new CircuitException(NetConstans.STATUS_610, e);
		}
	}

	@Override
	public void flow(Frame frame,Circuit circuit, IPlug plug) throws CircuitException {
		Circuit cir = circuit;
		if ("setChannel/1.0".equalsIgnoreCase(cir.protocol())) {
			pool = (IChannelPool) cir.attribute("channel-pool");
			genChannel();
			return;
		}
		String c = frame.command();
		if ("connect".equals(c))
			return;
		if ("disconnect".equals(c)) {
			ch.disconnect();
			return;
		}

		// ByteBuf buf = Unpooled.directBuffer();
		if (!ch.isActive() || !ch.isOpen()) {
			genChannel();
		}
		send(ch, frame);

		frame.dispose();
	}

	private void send(Channel ch, Frame frame) {
		DefaultFullHttpRequest request = new DefaultFullHttpRequest(
				HttpVersion.HTTP_1_1, HttpMethod.GET, frame.url());
		// request.headers().set(HttpHeaders.Names.HOST, "localhost:8080");
		if (!frame.containsHead("Connection")) {
			request.headers().set(HttpHeaders.Names.CONNECTION,
					HttpHeaders.Values.CLOSE);
		}
		request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING,
				HttpHeaders.Values.GZIP);
		copyToReq(request, frame);
		ch.writeAndFlush(request);
	}

	private void copyToReq(DefaultFullHttpRequest req, Frame frame) {
		HttpHeaders headers = req.headers();
		for (String key : frame.enumHeadName()) {
			String v = "";
			if ("url".equals(key)) {
				// v = frame.url();
				// headers.add(HttpHeaders.Names.LOCATION, v);
			} else {
				v = frame.head(key);
				headers.add(key, v);
			}

		}

		// 拷贝head
		if (req.content().readableBytes() > 0) {
			frame.content().writeBytes(req.content());
			// fr.release();

		}
	}
}
