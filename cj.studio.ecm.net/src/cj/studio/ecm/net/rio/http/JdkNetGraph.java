package cj.studio.ecm.net.rio.http;

import java.util.Map;

import cj.studio.ecm.graph.Access;
import cj.studio.ecm.graph.AnnotationProtocolFactory;
import cj.studio.ecm.graph.Graph;
import cj.studio.ecm.graph.GraphCreator;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IProtocolFactory;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.graph.SinkCreateBy;
import cj.studio.ecm.net.graph.INetGraph;
import cj.studio.ecm.net.nio.NetConstans;

class JdkNetGraph extends Graph implements INetGraph {
	String name;
	Map<String, String> refProps;
	public JdkNetGraph(String name, Map<String, String> props) {
		// options("$graphName",name);
		this.name = name;
		refProps = props;
	}

	@Override
	public Object options(String key) {
		if (super.containsOptions(key))
			return super.options(key);
		return refProps != null ? refProps.get(key) : null;
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return name;
	}

	@Override
	protected String defineAcceptProptocol() {
		// TODO Auto-generated method stub
		return ".*";
	}

	@Override
	public IPin netInput() {
		// TODO Auto-generated method stub
		return in("input");
	}

	@Override
	public IPin netOutput() {
		// TODO Auto-generated method stub
		return out("output");
	}

	@Override
	protected GraphCreator newCreator() {
		return new ApacheNetGraphCreator();
	}

	@Override
	protected void build(GraphCreator c) {
		c.newCablePin("input", Access.input).plugLast("translater",
				new SinkCreateBy(c));
	}
	@Override
	public void dispose() {
		super.dispose();
	}
	class ApacheNetGraphCreator extends GraphCreator {
		@Override
		protected IProtocolFactory newProtocol() {
			return AnnotationProtocolFactory.factory(NetConstans.class);
		}

		@Override
		protected ISink createSink(String sink) {
			if ("translater".equals(sink)) {
				return new Translater();
			}
			return null;
		}
	}


}
