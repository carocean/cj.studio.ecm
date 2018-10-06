package cj.studio.ecm.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceSite;
import cj.studio.ecm.net.graph.INetGraph;
import cj.ultimate.util.StringUtil;

public abstract class Graph implements IGraph {
	private GraphCreator creator;
	private byte status;
	private IGraphServiceSite gsite;
	private Map<String, Object> options;
	@CjServiceSite()
	private IServiceSite chipSite;

	public Graph() {
		gsite = createSite();
		options = new HashMap<String, Object>(4);
		CjService ser = this.getClass().getAnnotation(CjService.class);
		if (ser != null) {
			options.put("$graphName", ser.name());
		} else {
			options.put("$graphName", this.getClass().getName());
		}
	}

	public String processId() {
		String key = "processid-in-netsite";
		return options.containsKey(key) ? (String) options.get(key) : name();
	}

	public boolean containsOptions(String key) {
		return options.containsKey(key);
	}

	@Override
	public final String acceptProtocol() {
		String protocol = defineAcceptProptocol();
		if (StringUtil.isEmpty(protocol)) {
			IProtocolFactory f = protocolFactory();
			if (f != null) {
				return f.getProtocol();
			}
			return ".*";
		}
		if (protocol.contains(".*") && protocol.length() != 2) {
			throw new EcmException("协议如果定义为.*，则不能有其它声明。");
		}
		if (!protocol.contains(".*")) {
			if (protocolFactory() != null) {
				if (protocol.endsWith("|")) {
					protocol = String.format("%s%s", protocol,
							protocolFactory().getProtocol());
				} else {
					protocol = String.format("%s|%s", protocol,
							protocolFactory().getProtocol());
				}
			}
		}
		return protocol;

	}

	protected String defineAcceptProptocol() {
		return "";
	}

	@Override
	public void parent(IServiceProvider parent) {
		gsite.parent(parent);
	}

	/**
	 * 为图提供服务
	 * 
	 * <pre>
	 * 图中服务的搜索是先搜索站点中的父服务提供器，再搜索系统站点
	 * </pre>
	 * 
	 * @return
	 */
	protected IGraphServiceSite createSite() {
		return new GraphServiceSite();
	}

	@Override
	public String name() {
		// if (!isInit()) {
		// throw new EcmException("graph未初始化.");
		// }
		String name = (String) options.get("$graphName");
		return name;
	}

	@Override
	public final GraphCreator creator() {
		return creator;
	}

	@Override
	public final IProtocolFactory protocolFactory() {
		if (creator == null)
			return null;
		return creator.protocolFactory();
	}

	public void setChipSite(IServiceSite site) {
		this.gsite.chipSite(site);
	}

	public void options(String key, Object value) {
		if (key.startsWith("$")) {
			throw new EcmException("key 前缀$为系统所用，开发者不能指定为$");
		}
		options.put(key, value);
	}

	public Object options(String key) {
		return options.get(key);
	}

	@Override
	public final void initGraph() {
		initGraph(null);
	}

	public final void initGraph(Map<String, Object> options) {
		if (options != null) {
			Iterator<Entry<String, Object>> it = options.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, Object> kv = it.next();
				this.options.put(kv.getKey(), kv.getValue());
			}
		}
		GraphCreator creator = newCreator();
		if (creator == null)
			throw new EcmException("构建器为空");
		gsite.chipSite(chipSite);
		creator.site = gsite;
		initGraphImpl(creator);
	}

	@Override
	public boolean isInit() {
		return status == 1 ? true : false;
	}

	private void initGraphImpl(GraphCreator creator) {
		if (creator.site == null) {
			creator.site = gsite;
		}
		this.creator = creator;
		creator.init(options);
		creator.factory = creator.newProtocol();

		creator.onBuildBefore();
		build(creator);
		creator.onBuildAfter();
		// in = creator.in;
		// inNames = in.keySet().toArray(new String[0]);
		// out = creator.out();
		// outNames = out.keySet().toArray(new String[0]);
		status = 1;
	}

	@Override
	public String[] enumInputPin() {
		// if (inNames.length != in.size()) {
		// inNames = in.keySet().toArray(new String[0]);
		// }
		return creator.in.keySet().toArray(new String[0]);
	}

	@Override
	public String[] enumOutputPin() {
		// if (outNames.length != out.size()) {
		// outNames = out.keySet().toArray(new String[0]);
		// }
		return creator.out.keySet().toArray(new String[0]);
	}

	/**
	 * 
	 * 图的构建器，如果派生类返回null将使用默认的构建器。默认构建器仅提供一个主端子
	 * 
	 * <pre>
	 * 如需会话层和路由层，需从该类NetLayerGraphCreator的派生实例
	 * </pre>
	 * 
	 * @return
	 */
	protected GraphCreator newCreator() {
		return new DefaultGraphCreator();
	}

	/**
	 * 构建图
	 * 
	 * <pre>
	 * 如果构建中需要对已有sink进行分流，
	 * 只需在sink前或者流程中适当的节点位置添加sink，
	 * 并在此sink中实现分流逻辑即可，每个sink内实现的分流逻辑无需改变
	 * 
	 * 服务提供：
	 * 1.入站netsite时对当前graph的事件通知服务：
	 *   site().addService("$.graph.handler",IGraphHandler);
	 *   handler的demandPin事件用于请求当前graph给出在in,out中不存在的端子，也就是netsite管理员要求图开发者提供的端子，用于动态按需创建端子。
	 *   
	 * 2.NetLayer层包括会话管理和cookie管理。会话层需要派生NetLayerGraphCreator
	 * 	1）如需要会话：调用GraphCreatorHelper.attachSessionNetLayer方法。
	 *  2)如需要cookie管理：调用GraphCreatorHelper.attachCookieContainerNetLayer方法
	 *  
	 * 3.获取netsite的控制台
	 * 	site().getService("$.cmdline(console)>");
	 * 
	 * 4.调用netsite的chipgraph与net的连接指令，用于使当前的图中的端子与net建立连接。例：
	 * 	site().getService("$.cmdline(gc)>is -n website -o input");
	 *  site().getService("$.cmdline(cc)>connect localhost:9090 -t udt");
	 *  
	 * 5.会话侦听：
	 * 	NetLayerGraphCreator c=(NetLayerGraphCreator)creator;
	 * 		c.getSessionManager().addEvent(event);
	 * 
	 * <b>注意：</b>
	 * graph的开发者务必用creator创建端子和sink，
	 * 这是为了适应graph的使用者捕获pin和sink以根据需要扩展。
	 * 比如，第三方想实现sink的服务容器
	 * </pre>
	 * 
	 * @param creator
	 */
	protected abstract void build(GraphCreator creator);

	public IComponent component(String name) {
		return creator.components.get(name);
	}

	public String[] enumComponent() {
		return creator.components.keySet().toArray(new String[0]);
	}

	public boolean containsComponent(String name) {
		return creator.components.containsKey(name);
	}

	public boolean containsInput(String name) {
		return creator.in.containsKey(name);
	}

	public boolean containsOutput(String name) {
		return creator.out.containsKey(name);
	}

	public IPin in(String pinName) {
		// if (in == null)
		// return null;
		return creator.in.get(pinName);
	}

	@Override
	public IPin out(String pinName) {
		// if (out == null)
		// return null;
		return creator.out.get(pinName);
	}

	protected void dispose(boolean isdeep) {
		if (isdeep) {
			if (creator != null)
				creator.dispose();
			// if (in != null) {
			// for (String key : in.keySet()) {
			// Pin p = in.get(key);
			// p.dispose();
			// }
			// in.clear();
			// }
			// in = null;
			// if (out != null) {
			// for (String key : out.keySet()) {
			// Pin p = out.get(key);
			// p.dispose();
			// }
			// out.clear();
			// }
			// out = null;
			// this.outNames = null;
			// this.inNames = null;
			options.clear();
			chipSite=null;
			status = 0;
		}
	}

	@Override
	public void dispose() {
		dispose(true);
		gsite = null;
	}

	/**
	 */
	@Override
	public void print(StringBuffer sb) {
		print(sb, " ");
	}

	@Override
	public void print(StringBuffer sb, String indent) {
		// sb.append(this.getClass().getSimpleName() + indent);
		// for (String s : in.keySet()) {
		// in.get(s).print(sb, indent);
		// }
		// for (String s : out.keySet()) {
		// out.get(s).print(sb, indent);
		// }
	}

	@Override
	public IServiceSite site() {
		return gsite;
	}

	class DefaultGraphCreator extends GraphCreator {

		@Override
		public ISink createSink(String sink) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	class GraphServiceSite implements IGraphServiceSite {
		private IServiceSite chipsite;
		private IServiceProvider parent;

		@Override
		public void chipSite(IServiceSite chipSite) {
			chipsite = chipSite;
		}

		@Override
		public void parent(IServiceProvider parent) {
			this.parent = parent;
		}

		@Override
		public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
			ServiceCollection<T> col1 = null;
			boolean col1Empty = true;
			boolean col2Empty = true;
			if (parent != null) {
				col1 = parent.getServices(serviceClazz);
				if (col1 == null)
					col1Empty = true;
				else
					col1Empty = col1.isEmpty();
			}
			ServiceCollection<T> col2 = null;
			if (chipsite != null) {
				col2 = chipsite.getServices(serviceClazz);
				if (col2 == null)
					col2Empty = true;
				else
					col2Empty = col2.isEmpty();
			}
			if (col1Empty && col2Empty) {
				return new ServiceCollection<T>();
			}
			if (col1Empty && !col2Empty) {
				return col2;
			}
			if (col2Empty && !col1Empty) {
				return col1;
			}
			List<T> list1 = col1.asList();
			List<T> list2 = col2.asList();
			List<T> list = new ArrayList<T>();
			list.addAll(list1);
			list.addAll(list2);
			ServiceCollection<T> ret = new ServiceCollection<T>(list);
			return ret;
		}

		@Override
		public Object getService(String serviceId) {
			if ("$.graph.options".equals(serviceId)) {
				return options;
			}
			if ("$.graph.socketTable".equals(serviceId)) {
				return creator.socketTable;
			}
			if ("$.graph.protocolFactory".equals(serviceId)) {
				return protocolFactory();
			}
			if ("$.graph.classloader".equals(serviceId)) {
				return this.getClass().getClassLoader();
			}
			if (serviceId.startsWith("$.graph.atom")) {
				String name = serviceId.replace("$.graph.atom.", "");
				Object s = creator.newAtom(name);
				if (s != null)
					return s;
			}
			if (serviceId.startsWith("$.graph.component")) {
				String name = serviceId.replace("$.graph.component.", "");
				return creator.components.get(name);
			}
			if (serviceId.startsWith("$.graph.demand.sink")) {
				return creator.newSink(serviceId.replace(
						"$.graph.demand.sink.", ""));
			}
			if (serviceId.startsWith("$.graph.lookup.innerPin")) {
				String key = serviceId.replace("$.graph.lookup.innerPin.", "");
				if (StringUtil.isEmpty(key))
					return null;
				return creator.inner.get(key);
			}
			if (parent != null) {
				if (serviceId.startsWith("$.cmdline(")) {
					String newsid = parseGc(serviceId);
					return parent.getService(newsid);
				}
				Object service = null;
				service = parent.getService(serviceId);
				if (service != null) {
					return service;
				}
			}
			if (chipsite == null)
				return null;
			return chipsite.getService(serviceId);
		}

		// $.cmdline(gc)>plug -p xxx -o input -n website
		// Object
		// obj=plug.site().getService("$.cmdline(gc)>plug -o input -p input -n website");
		private String parseGc(String serviceId) {
			Pattern p = Pattern.compile("^\\$\\.cmdline\\((.+)\\)\\>(.*)$");
			Matcher m = p.matcher(serviceId);
			String linestr = "";
			if (!m.find()) {
				throw new EcmException(String.format(
						"请求的命令格式错误%s,格式：$.cmdline()>", serviceId));
			}
			switch (m.group(1)) {
			case "gc":
				linestr = m.group(2);
				String line = String.format("$.cmdline(%s)>%s -pid %s -g %s",
						m.group(1), linestr, processId(), name());
				return line;
			case "cc":
				return serviceId;
			default:
				return serviceId;
			}
		}

		@Override
		public void addService(Class<?> clazz, Object service) {
			// TODO Auto-generated method stub
			chipsite.addService(clazz, service);
		}

		@Override
		public void removeService(Class<?> clazz) {
			// TODO Auto-generated method stub
			chipsite.removeService(clazz);
		}

		@Override
		public void addService(String serviceName, Object service) {
			// TODO Auto-generated method stub
			chipsite.addService(serviceName, service);
		}

		@Override
		public void removeService(String serviceName) {
			// TODO Auto-generated method stub
			chipsite.removeService(serviceName);
		}

		@Override
		public String getProperty(String key) {
			if ("$.graph.name".equals(key)) {
				return name();
			}
			if ("$.graph.processId".equals(key)) {
				return processId();
			}
			if ("$.graph.protocol".equals(key)) {
				return protocolFactory().getProtocol();
			}
			if ("$.graph.isNet".equals(key)) {
				return Graph.this instanceof INetGraph ? "true" : "false";
			}
			return chipsite.getProperty(key);
		}

		@Override
		public String[] enumProperty() {
			return chipsite.enumProperty();
		}

	}
}
