package cj.studio.ecm.net.web;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;

public interface IWebsocketFilter extends ISink {
	int sort();
	@Override
	public void flow(Frame frame, Circuit circuit,IPlug plug) throws CircuitException;
}
