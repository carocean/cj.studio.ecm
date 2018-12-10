package cj.studio.ecm.net.switch0;

import java.util.Map;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.Access;
import cj.studio.ecm.graph.AnnotationProtocolFactory;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.Graph;
import cj.studio.ecm.graph.GraphCreator;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.IProtocolFactory;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.graph.INetGraph;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.web.HttpCircuit;
import cj.studio.ecm.net.web.HttpFrame;

class SwitchGraph extends Graph implements INetGraph {
	private String name;
	public SwitchGraph(String name, Map<String, String> props) {
		this.name = name;
		options(KEY_SWITCH_GRAPH, "true");
		for(String key:props.keySet()){
			options(key,props.get(key));
		}
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return name;
	}

	@Override
	public IPin netInput() {
		// TODO Auto-generated method stub
		return in(getPort());
	}

	@Override
	public IPin netOutput() {
		// TODO Auto-generated method stub
		return out(getPort());
	}

	protected String getPort() {
		return (String) options("port");
	}

	

	@Override
	protected void build(GraphCreator creator) {
		// TODO Auto-generated method stub
		IPin input = creator.newWirePin(getPort(), Access.input);
		IPin output = creator.newWirePin(getPort(), Access.output);
		ISink diverter = creator.newSink("diverter");

		IPlug p = input.plugFirst("diverter", diverter);
		p.plugBranch(getPort(), output);
	}

	@Override
	protected GraphCreator newCreator() {
		return new GraphCreator() {
			@Override
			protected IProtocolFactory newProtocol() {
				return AnnotationProtocolFactory.factory(NetConstans.class);
			}

			@Override
			public ISink createSink(String sink) {
				if ("diverter".equals(sink)) {
					return new Diverter();
				}
				return null;
			}
		};
	}

	private void initCircuit(Circuit c, Circuit circuit) {
		c.attribute("select-type", (String) circuit.attribute("select-type"));
		c.attribute("local-address",
				(String) circuit.attribute("local-address"));
		c.attribute("remote-address",
				(String) circuit.attribute("remote-address"));
		c.attribute("select-name", (String) circuit.attribute("select-name"));
		c.attribute("switch-name", name());
		c.attribute("switch-port", getPort());
		c.attribute("select-simple",
				(String) circuit.attribute("select-simple"));
		c.attribute("select-id", (String) circuit.attribute("select-id"));
	}

	class Diverter implements ISink {
		@Override
		public void flow(Frame frame,Circuit circuit, IPlug plug) throws CircuitException {
			IPin out = plug.branch(getPort());
			// String line=String.format("%s 200 ok", frame.protocol());
			Circuit c = null;
			Frame f=null;
			if ("HTTP/1.1".equals(frame.protocol())) {
				c = new HttpCircuit(circuit.toString());
				f=new HttpFrame(frame.toString());
				f.copyFrom(frame,true);
			} else {
				c = new Circuit(circuit.toString());
				f=frame.copy();
			}
			initCircuit(c, circuit);
			ClassLoader old=Thread.currentThread().getContextClassLoader();
			try {
				out.flow(f, c);
			} catch (Exception e) {
				throw e;
			} finally {
				circuit.copyFrom(c, true);
				Thread.currentThread().setContextClassLoader(old);
			}
		}

	}
}
