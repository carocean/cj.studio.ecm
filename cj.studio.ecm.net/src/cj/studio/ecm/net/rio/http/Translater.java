package cj.studio.ecm.net.rio.http;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.frame.IFeedback;
import cj.studio.ecm.frame.IFlowContent;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.nio.netty.http.HttpException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class Translater implements ISink {
	
	@Override
	public void flow(Frame frame, Circuit circuit, IPlug plug)
			throws CircuitException {
		IHttpChannel ch = (IHttpChannel) plug.optionsPin("http-channel");
		String url = frame.retrieveUrl();
		HttpURLConnection conn = ch.open(frame.command(), url);
		try {
			setConn(conn, frame);
			conn.connect();
			setCircuit(conn, circuit);
		} catch (IOException e) {
			if (e instanceof FileNotFoundException) {
				throw new CircuitException("404", e.getMessage());
			}
			try {
				throw new CircuitException(
						String.valueOf(conn.getResponseCode()),
						conn.getResponseMessage());
			} catch (IOException e1) {
				throw new CircuitException("503", e1.getMessage());
			}
		} finally {
			conn.disconnect();
		}
		if (circuit.containsFeedback(IFeedback.KEY_INPUT_FEEDBACK)) {
			IFeedback back = circuit.feedback(IFeedback.KEY_INPUT_FEEDBACK);
			try {
				Frame f = circuit.convert();
				back.doBack(f, circuit);
			} catch (CircuitException e) {
				throw e;
			}
		}

	}

	private void setConn(HttpURLConnection conn, Frame frame)
			throws IOException {
		String[] heads = frame.enumHeadName();
		for (String key : heads) {
			String v = frame.head(key);
			conn.addRequestProperty(key, v);
		}
		// querystring已在ch打开前初始化完。
		// 原则是：frame地址中的查询串作为查询串不动，如果想动也是在ch打开时调整。即在ch.open(frame.command(),
		// frame.url());代码段前
		// 侦参数作为post的提交参数。
		if ("get".equalsIgnoreCase(frame.command())) {
			IFlowContent cnt = frame.content();
			if (cnt.readableBytes() > 0) {// 如果是http侦，说明是
				byte[] bypes = null;
				bypes = cnt.readFully();
				conn.getOutputStream().write(bypes);
			}
		} else if ("post".equalsIgnoreCase(frame.command())) {

			IFlowContent cnt = frame.content();
			byte[] bypes = null;
			if (cnt.readableBytes() > 0) {// 如果是http侦，说明是
				bypes = cnt.readFully();
			} else {
				StringBuffer params = new StringBuffer();
				// 表单参数与get形式一样
				String[] ps = frame.enumParameterName();
				for (String key : ps) {
					String v = frame.parameter(key);
					params.append(key).append("=").append(v);
				}
				bypes = params.toString().getBytes();
			}
			conn.getOutputStream().write(bypes);// 输入参数
		} else {
			throw new HttpException("仅支持get,post方法");
		}

	}

	private void setCircuit(HttpURLConnection conn, Circuit circuit)
			throws IOException {
		InputStream in = conn.getInputStream();
		byte[] buf = new byte[4096];
		int read = 0;
		ByteBuf bb = Unpooled.buffer();
		while (true) {
			read = in.read(buf);
			if (read == -1) {
				break;
			}
			bb.writeBytes(buf, 0, read);
		}
		// b=bb.array();
		circuit.status(String.valueOf(conn.getResponseCode()));
		circuit.message(conn.getResponseMessage());
		circuit.content().writeBytes(bb);
		Map<String, List<String>> map = conn.getHeaderFields();
		for (int i = 0; i < map.size(); i++) {
			String v = conn.getHeaderField(i);
			String key = conn.getHeaderFieldKey(i);
			if (key == null)
				continue;
			circuit.head(key, v);
		}
	}

}