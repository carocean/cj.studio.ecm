package cj.studio.ecm.net.rio;

import java.io.IOException;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.INetGraphInitedEvent;
import cj.studio.ecm.net.INetGraphStopedEvent;
import cj.studio.ecm.net.IServer;
import cj.studio.ecm.net.graph.INetGraph;
import cj.ultimate.util.StringUtil;

public abstract class BaseServerRIO implements IServer {
	private INetEngine engine;
	private Map<String, String> props;
	private List<INetGraphInitedEvent> graphinitevents;
	private List<INetGraphStopedEvent> graphStopedEvents;

	public BaseServerRIO() {
		props = new HashMap<String, String>();
		graphinitevents = new ArrayList<INetGraphInitedEvent>(1);
		graphStopedEvents = new ArrayList<INetGraphStopedEvent>(1);
		INetGraphNamed named = new NetGraphNamed();
		engine = new NetEngine(named, props);
	}
	@Override
	public void removeProperty(String propName) {
		props.remove(propName);
	}
	public String netName() {
		CjService s = BaseServerRIO.this.getClass()
				.getAnnotation(CjService.class);
		if (s == null) {
			String name = BaseServerRIO.this.getProperty("serverName");
			if (StringUtil.isEmpty(name))// 如果为空则以端口号作为名字
				name = BaseServerRIO.this.simple() + "-"
						+ BaseServerRIO.this.getProperty("port");
			return name;
		}

		return s.name();
	}

	protected abstract AbstractSelectableChannel openNetEngine(String inetHost,int port) throws IOException;

	protected final INetEngine engine() {
		return engine;
	}


	@Override
	public String getINetHost() {
		// TODO Auto-generated method stub
		return props.get("inetHost");
	}

	@Override
	public void start(String inetHost, String port) {
		if("listening".equals(props.get("status"))){
			throw new EcmException("服务器已运行");
		}
		props.put("port", port);
		if (!StringUtil.isEmpty(inetHost)) {
			props.put("inetHost", inetHost);
		}
		props.put("protocol", simple());
		AbstractSelectableChannel ch=null;
		try {
			ch=openNetEngine(inetHost,Integer.valueOf(port));
		} catch (Exception e) {
			throw new EcmException(String.format("端口绑定失败. port：%s, 原因：%s", port,
					e.getMessage()));
		}
		engine.setNetProperties(props);
		INetGraph ng = engine.getNetGraph();
		ng.options("port", port);
		ng.options("protocol", simple());
		
		try {
			
			engine.moniter(this,ch,props,null);
			props.put("status", "listening");
			for (INetGraphInitedEvent event : graphinitevents) {
				event.graphInited(this);
			}
		} catch (Exception e) {
			throw new EcmException(e);
		} finally {

		}
	}

	/**
	 * 默认线程数为8
	 */
	@Override
	public final void start(String port) {
		start(null, port);
	}

	@Override
	public void stop() {
		engine.stopMoniter();
		for (INetGraphStopedEvent e : this.graphStopedEvents) {
			e.stop(this);
		}
		props.clear();
		props.put("status", "stoped");
		this.graphinitevents.clear();
		this.graphStopedEvents.clear();
	}

	@Override
	public String getPort() {
		return props.get("port");
	}

	@Override
	public void setProperty(String propName, String value) {
		props.put(propName, value);
	}

	@Override
	public String getProperty(String name) {
		if("bossThreadCount".equals(name)){
			return String.valueOf(engine.bossThreadCount());
		}
		if("workThreadCount".equals(name)){
			return engine.workThreadCount();
		}
		return props.get(name);
	}

	@Override
	public String[] enumProp() {
		String[] at=props.keySet().toArray(new String[0]);
//		String[] ret=new String[at.length+4];
//		ret[0]="bossThreadCount";
//		ret[1]="workThreadCount";
//		System.arraycopy(at, 0, ret, 4, at.length);
		return at;
	}

	@Override
	public final INetGraph buildNetGraph() {
		INetGraph ng = engine.getNetGraph();
		if (ng.isInit()) {
			return ng;
		}

		ng.initGraph();
		return ng;
	}

	@Override
	public void eventNetGraphInited(INetGraphInitedEvent event) {
		graphinitevents.add(event);
	}

	@Override
	public void eventNetGraphStoped(INetGraphStopedEvent event) {
		this.graphStopedEvents.add(event);
	}

	class NetGraphNamed implements INetGraphNamed {
		@Override
		public String getName() {
			return netName();
		}
	}
}
