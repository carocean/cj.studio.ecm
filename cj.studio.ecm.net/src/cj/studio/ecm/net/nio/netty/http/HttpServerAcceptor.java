package cj.studio.ecm.net.nio.netty.http;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;

/**
 * 绑定物理通道，用于接收信息，并将之转换为graph的输入协议。
 * 
 * <pre>
 *
 * </pre>
 * 
 * @author carocean
 *
 */
public class HttpServerAcceptor implements ISink {

	@Override
	public void flow(Frame frame,Circuit circuit, IPlug plug) throws CircuitException {
		if ("http".equals(circuit.attribute("select-simple"))) {
			processHttp(frame, circuit, plug);
			return;
		} else if ("ws".equals(circuit.attribute("select-simple"))) {
			processWs(frame, circuit, plug);
			return;
		} else {
			throw new RuntimeException("不支持的协议." + frame.protocol());
		}
	}

	private void processWs(Frame frame, Circuit circuit, IPlug plug)
			throws CircuitException {
		plug.flow(frame, circuit);
	}

	private void processHttp(Frame frame, Circuit circuit, IPlug plug)
			throws CircuitException {

		plug.flow(frame, circuit);
		// circuit.dispose();
		return;
	}

}
