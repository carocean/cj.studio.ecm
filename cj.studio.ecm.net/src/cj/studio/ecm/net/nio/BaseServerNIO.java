package cj.studio.ecm.net.nio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.annotation.CjMethod;
import cj.studio.ecm.annotation.CjMethodArg;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.graph.Access;
import cj.studio.ecm.graph.Graph;
import cj.studio.ecm.graph.GraphCreator;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.INetGraphInitedEvent;
import cj.studio.ecm.net.INetGraphStopedEvent;
import cj.studio.ecm.net.IServer;
import cj.studio.ecm.net.graph.INetGraph;
import cj.studio.ecm.net.nio.netty.INettyGraph;
import cj.studio.ecm.net.util.IdGenerator;
import cj.ultimate.util.StringUtil;

public abstract class BaseServerNIO implements IServer {
	private INetGraph graph;
	private IServiceSite site;
	private List<INetGraphInitedEvent> graphInitedevents;
	protected Map<String, String> props;
	private List<INetGraphStopedEvent> graphStopedEvents;

	public BaseServerNIO() {
		props = new HashMap<String, String>();
		props.put("status", "init");
		this.graphInitedevents = new ArrayList<INetGraphInitedEvent>();
		graphStopedEvents = new ArrayList<INetGraphStopedEvent>(1);
	}
	@Override
	public void removeProperty(String propName) {
		// TODO Auto-generated method stub
		props.remove(propName);
	}
	@Override
	public String[] enumProp() {
		// TODO Auto-generated method stub
		return props.keySet().toArray(new String[0]);
	}

	@CjMethod()
	public void setSite(
			@CjMethodArg(ref = "cj.studio.ecm.IServiceSite") IServiceSite site) {
		this.site = site;
	}

	@Override
	public void eventNetGraphInited(INetGraphInitedEvent event) {
		this.graphInitedevents.add(event);
	}

	@Override
	public void eventNetGraphStoped(INetGraphStopedEvent event) {
		this.graphStopedEvents.add(event);
	}

	@Override
	public final INetGraph buildNetGraph() {
		if (graph == null) {
			graph = createNetGraph();
		}
		if (graph == null)
			throw new RuntimeException("netGraph没有创建");
		return graph;
	}

	/**
	 * 创建netGraph
	 * 
	 * <pre>
	 * 该方法返回一个默认的图，留到派生类以替换
	 * </pre>
	 * 
	 * @return
	 */
	protected INetGraph createNetGraph() {
		return new NetGraph();
	}

	/**
	 * 创起家netGraph构建器，比如自定义了netGraph的相关sink
	 * 
	 * <pre>
	 * 默认情况使用netgraph的默认构建器，不过派生类可替换
	 * </pre>
	 * 
	 * @return
	 */
	protected GraphCreator createNetGraphCreator() {
		return null;
	}

	@Override
	public void start(String inetHost, String port) {
		if (StringUtil.isEmpty(port))
			throw new RuntimeException("错误：没有指定端口");
		props.put("port", port);
		if (!StringUtil.isEmpty(inetHost)) {
			props.put("inetHost", inetHost);
		}
		// 如果指定的creator则规换掉默认的图的creator
		// GraphCreator creator = createNetGraphCreator();
		// if (creator != null)
		// getNetGraph().setCreator(creator);
		if (buildNetGraph() != null) {
			((Graph) graph).setChipSite(site);
			String[] keys = props.keySet().toArray(new String[0]);
			for (String key : keys) {
				graph.options(key, props.get(key));
			}
			if (!graph.isInit()) {
				graph.initGraph();
			}
			((Graph) graph).options("netType", this.getClass().getSimpleName());
			((Graph) graph).options("isNet", true);
		}
		startServer();
		props.put("status", "listening");
		for (INetGraphInitedEvent event : graphInitedevents) {
			event.graphInited(this);
		}
	}

	@Override
	public final void start(String port) {
		start(null,port);
	}

	public void setProperty(String propName, String value) {
		props.put(propName, value);
	}

	public String getProperty(String name) {
		return props.get(name);
	}

	public String getPort() {
		return props.get("port");
	}
	@Override
	public String getINetHost() {
		return props.get("inetHost");
	}
	/**
	 * 在派生类中实现启动服务器
	 * 
	 */
	protected abstract void startServer();

	@Override
	public final void stop() {
		// graph.dispose();
		site = null;
		stopServer();
		for (INetGraphStopedEvent e : this.graphStopedEvents) {
			e.stop(this);
		}
		props.put("status", "stoped");
		graphInitedevents.clear();
		graphStopedEvents.clear();
	}

	protected abstract void stopServer();

	@Override
	public String netName() {
		CjService s = BaseServerNIO.this.getClass()
				.getAnnotation(CjService.class);
		if (s == null) {
			String name = BaseServerNIO.this.getProperty("serverName");
			if (StringUtil.isEmpty(name))// 如果为空则以端口号作为名字
				name = BaseServerNIO.this.simple() + "-"
						+ BaseServerNIO.this.getProperty("port");
			return name;
		}

		return s.name();
	}

	class NetGraph extends Graph implements INettyGraph {
		ISink diverter;
		ISink sender;

		@Override
		public String name() {
			return netName();
		}

		@Override
		public IPin netInput() {
			return in("input");
		}

		@Override
		public IPin netOutput() {
			return out("output");
		}

		@Override
		protected GraphCreator newCreator() {
			GraphCreator gc = createNetGraphCreator();
			this.setChipSite(site);
			return gc;
		}

		@Override
		public IPin newAcceptorPin() {
			String id = IdGenerator.newInstance().asShortText();
			while (creator().containsInputPin("input_" + id)) {
				id = IdGenerator.newInstance().asShortText();
			}
			IPin inputPin = creator().newWirePin("input_" + id, Access.input);
			inputPin.plugLast("acceptor", creator().newSink("acceptor"));
			IPlug dplug = inputPin.plugLast("diverter", diverter);
			dplug.plugBranch("output", netOutput());
			return inputPin;
		}

		@Override
		protected void build(GraphCreator creator) {
			sender = creator.newSink("sender");
			diverter = creator.newSink("diverter");

			creator.newWirePin("output", Access.output);
			IPin input = creator.newWirePin("input", Access.input);
			IPin senderIn = creator.newWirePin("senderIn", Access.input);
			IPin feedback = creator.newWirePin("feedback");

			senderIn.plugLast("sender", sender);
			feedback.plugLast("sender", sender);
			input.plugLast("diverter", diverter).plugBranch("feedback",
					feedback);
		}

	}
}
