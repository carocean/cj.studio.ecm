package cj.studio.ecm.net.rio;

import java.io.IOException;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.IClient;
import cj.studio.ecm.net.IConnectCallback;
import cj.studio.ecm.net.IRioClientCloseEvent;
import cj.studio.ecm.net.IRioClientCloseEventable;
import cj.studio.ecm.net.graph.INetGraph;
import cj.ultimate.util.StringUtil;

public abstract class BaseClientRIO implements IClient,IRioClientCloseEventable {
	private INetEngine engine;
	private Map<String, String> props;
	List<IRioClientCloseEvent> events;
	public BaseClientRIO() {
		props = new HashMap<String, String>();
		events=new ArrayList<>();
		props.put("status", "inited");
		INetGraphNamed named = new NetGraphNamed();
		engine = new NetEngine(named, props);
	}
	@Override
	public void onEvent(IRioClientCloseEvent e) {
		// TODO Auto-generated method stub
		events.add(e);
	}
	public String netName() {
		CjService s = BaseClientRIO.this.getClass()
				.getAnnotation(CjService.class);
		if (s == null) {
			String name = BaseClientRIO.this.getProperty("clientName");
			if (StringUtil.isEmpty(name))// 如果为空则以端口号作为名字
				name = BaseClientRIO.this.simple() + "$"
						+ BaseClientRIO.this.getProperty("host") + "#"
						+ BaseClientRIO.this.getProperty("port");
			return name;
		}

		return s.name();
	}

	protected abstract AbstractSelectableChannel openNetEngine(String host,
			int port) throws IOException;

	protected final INetEngine engine() {
		return engine;
	}

	@Override
	public String status() {
		// TODO Auto-generated method stub
		return props.get("status");
	}

	@Override
	public void setProperty(String key, String value) {
		props.put(key, value);
	}

	@Override
	public String getPort() {
		// TODO Auto-generated method stub
		return props.get("port");
	}

	@Override
	public String getHost() {
		// TODO Auto-generated method stub
		return props.get("host");
	}

	@Override
	public void close() {
		for(IRioClientCloseEvent e:events){
			e.onClosing(this);
		}
		engine.stopMoniter();
		props.clear();
		props.put("status", "stoped");
	}

	@Override
	public String getProperty(String name) {
		if ("bossThreadCount".equals(name)) {
			return engine.bossThreadCount();
		}
		if ("workThreadCount".equals(name)) {
			return engine.workThreadCount();
		}
		return props.get(name);
	}

	@Override
	public String[] enumProp() {
		String[] at = props.keySet().toArray(new String[0]);
//		String[] ret = new String[at.length + 4];
//		ret[0]="bossThreadCount";
//		ret[1]="workThreadCount";
//		System.arraycopy(at, 0, ret, 4, at.length);
		return at;
	}
	@Override
	public Object connect(String ip, String port) throws InterruptedException {
		return connect(ip, port, null);
	}
	/**
	 * 
	 * <pre>
	 * rio连接只能调用一次。
	 * 
	 * 它的主线程固定为1，调用者不可改变，其工作线程默认为1,也可以自定义。对于每次读事件的处理作业如果太耗时可以使用多个工作线程。
	 * 
	 * 在客户端中，发送和接收顺序是保证的。
	 * </pre>
	 */
	@Override
	public Object connect(String ip, String port, IConnectCallback callback)
			throws InterruptedException {
		if("listening".equals(props.get("status"))){
			throw new EcmException("连接已运行");
		}
		props.put("host", ip);
		props.put("port", port);
		props.put("protocol", simple());
		AbstractSelectableChannel ch = null;
		try {
			ch = openNetEngine(ip, Integer.valueOf(port));
		} catch (Exception e) {
			throw new EcmException(String.format("连接远程失败. %s%s, 原因：%s", ip,
					port, e.getMessage()));
		}
		engine.setNetProperties(props);
		INetGraph ng = engine.getNetGraph();
		ng.options("host", ip);
		ng.options("port", port);
		ng.options("protocol", simple());

		try {
			engine.moniter(this, ch, props, callback);
			props.put("status", "listening");
		} catch (Exception e) {
			throw new EcmException(e);
		} finally {

		}
		return ch;
	}

	@Override
	public INetGraph buildNetGraph() {
		INetGraph ng = engine.getNetGraph();
		// if (ng.isInit()) {
		// return ng;
		// }
		//
		// ng.initGraph();
		return ng;
	}

	@Override
	public String simple() {
		// TODO Auto-generated method stub
		return "rio-udt";
	}

	class NetGraphNamed implements INetGraphNamed {
		@Override
		public String getName() {
			return netName();
		}
	}
}
