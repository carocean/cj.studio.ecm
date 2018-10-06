package cj.studio.ecm.net.rio;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;

public class ReadSink implements ISink {

	@Override
	public void flow(Frame frame,Circuit circuit, IPlug plug) throws CircuitException {
		//如果是服务器端则检查捎带
		plug.flow(frame, circuit);
	}

}
