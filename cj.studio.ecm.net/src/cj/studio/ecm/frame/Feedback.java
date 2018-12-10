package cj.studio.ecm.frame;

import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.ICreateCallbackSink;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.graph.IWirePin;
import cj.studio.ecm.graph.WirePin;

public class Feedback implements IFeedback {
	private IWirePin p;
	public Feedback() {
		 p=new WirePin("feedback");
	}
	@Override
	public Object options(String name){
		return p.options(name);
	}
	@Override
	public void options(String name ,Object v){
		p.options(name,v);
	}
	@Override
	public void doBack(Frame frame,Circuit circuit) throws CircuitException {
		p.flow(frame, circuit);
	}
	@Override
	public IPlug plugSink(String name, ISink sink) {
		if(sink instanceof ICreateCallbackSink	){
			((ICreateCallbackSink)sink).setOwnerPin(p);
		}
		return p.plugFirst(name, sink);
	}

	@Override
	public boolean containsSink(String name) {
		return p.contains(name);
	}

	@Override
	public void removeSink(String name) {
		p.unPlug(name);
	}

	@Override
	public int count() {
		return p.enumSinkName().length;
	}
	@Override
	public void dispose() {
		if(p==null)return;
		p.dispose();
	}
}
