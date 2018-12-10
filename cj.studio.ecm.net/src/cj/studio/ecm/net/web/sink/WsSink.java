package cj.studio.ecm.net.web.sink;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.frame.IFeedback;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.web.SiteConfig;

public class WsSink implements ISink {

	// 在此解析侦并替换
	@Override
	public void flow(Frame frame,Circuit circuit, IPlug plug) throws CircuitException {
		SiteConfig wc = (SiteConfig) plug
				.optionsGraph(SiteConfig.WEBCONFIG_KEY);
		circuit.head("Content-Type", wc.responseFrameType());
//		IPin pin = plug.branch("output");
//		if (circuit.containsFeedback(IFeedback.KEY_OUTPUT_FEEDBACK)) {
//			if (!circuit.feedback(IFeedback.KEY_OUTPUT_FEEDBACK).containsSink("end")) {
//				circuit.feedback(IFeedback.KEY_OUTPUT_FEEDBACK).plugSink("end", new ISink() {
//
//					@Override
//					public void flow(Frame frame,Circuit circuit, IPlug plug)
//							throws CircuitException {
//						pin.flow(frame, circuit);
//
//					}
//				});
//			}
//		}
		// circuit.attribute("_output__WsSink", pin);
		plug.branch("filters").flow(frame, circuit);
	}

}
