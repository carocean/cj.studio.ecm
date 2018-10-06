package cj.studio.ecm.net.rio;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.frame.IFeedback;
import cj.studio.ecm.graph.ISink;

public abstract class Worker implements IWorker{
	protected IFeedback rioServerFeedbackSetSource(IChannelContext ctx,  Frame frame, Circuit circuit) {
		circuit.feedbackSetSource(IFeedback.KEY_OUTPUT_FEEDBACK);
		ISink end = new RIOFeedbackEndSink(ctx);
		IFeedback b = circuit.feedback(IFeedback.KEY_OUTPUT_FEEDBACK);
		b.options("channel-context",ctx);
		b.plugSink(IFeedback.KEY_ENDSINK_FEEDBACK, end);
		b.plugSink("rioHttpChunked", new RioHttpChunkedSink())
				.option("mainFrame", frame).option("mainCircuit", circuit);
		return b;
	}
}
