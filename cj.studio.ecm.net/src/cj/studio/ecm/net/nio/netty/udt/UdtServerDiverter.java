package cj.studio.ecm.net.nio.netty.udt;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;

public class UdtServerDiverter implements ISink {


	@Override
	public void flow(Frame frame,Circuit circuit, IPlug plug) throws CircuitException {
		// Send the uppercase string back.
		if (plug.owner().startsWith("input_")) {
			IPin out = plug.branch("output");
			out.flow(frame, circuit);
			return;
		}
		if ("input".equals(plug.owner())) {
			IPin sp = plug.branch("feedback");
			if("pull".equalsIgnoreCase(frame.command())||circuit.containsHead("sync")){
				frame.parameter("sync",circuit.head("sync"));
			}
			sp.flow(frame, circuit);
			return;
		}
	}
}
