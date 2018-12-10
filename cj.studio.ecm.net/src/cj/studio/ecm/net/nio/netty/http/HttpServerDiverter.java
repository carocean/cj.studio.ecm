package cj.studio.ecm.net.nio.netty.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;

public class HttpServerDiverter implements ISink {
	Logger logger=LoggerFactory.getLogger(HttpHandlerBinder.class);
	@Override
	public void flow(Frame frame,Circuit circuit, IPlug plug) throws CircuitException {
		// Send the uppercase string back.
		//logger.debug(frame+" "+plug.fullName()+" "+frame.head("senderType")+" "+plug.circuit().attribute("req")+" "+plug.circuit().attribute("ctx"));
		if (plug.owner().startsWith("input_")) {
			IPin out = plug.branch("output");
			out.flow(frame, circuit);
			return;
		}
		if ("input".equals(plug.owner())) {
			IPin sp = plug.branch("feedback");
			sp.flow(frame,circuit);
			return;
		}
		
	}
}
