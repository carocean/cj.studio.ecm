package cj.studio.ecm.net.nio.netty.http;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;

public class HttpClientAcceptor implements ISink {

	@Override
	public void flow(Frame frame,Circuit circuit, IPlug plug) throws CircuitException {
		plug.flow(frame,circuit);
	}

}
