package cj.studio.ecm.net.nio;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ChannelFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.annotation.CjMethod;
import cj.studio.ecm.annotation.CjMethodArg;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.graph.Access;
import cj.studio.ecm.graph.Graph;
import cj.studio.ecm.graph.GraphCreator;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.IConnectCallback;
import cj.studio.ecm.net.INetGraphInitedEvent;
import cj.studio.ecm.net.graph.IHandleBinder;
import cj.studio.ecm.net.graph.INetGraph;
import cj.studio.ecm.net.nio.netty.IChannelInitializer;
import cj.studio.ecm.net.nio.netty.INettyClient;
import cj.studio.ecm.net.nio.netty.INettyGraph;
import cj.studio.ecm.net.util.IdGenerator;
import cj.ultimate.util.StringUtil;

public abstract class BaseClientNIO<C extends Channel> implements INettyClient {
	protected Map<String, String> props;
	EventLoopGroup group;
	Bootstrap b;
	private INetGraph graph;
	private IServiceSite site;
	private IChannelPool pool;
	protected final List<INetGraphInitedEvent> graphinitedEvents;
	protected Logger logger = LoggerFactory.getLogger(this.getClass());

	public BaseClientNIO() {
		props = new HashMap<String, String>();
		props.put("status", "init");
		graphinitedEvents = new ArrayList<INetGraphInitedEvent>();
	}

	protected IChannelPool pool() {
		return pool;
	}

	@Override
	public void eventNetGraphInited(INetGraphInitedEvent event) {
		graphinitedEvents.add(event);
	}

	@CjMethod()
	public void setSite(
			@CjMethodArg(ref = "cj.studio.ecm.IServiceSite") IServiceSite site) {
		this.site = site;
	}

	public void setProperty(String key, String value) {
		props.put(key, value);
	}

	@Override
	public String[] enumProp() {
		// TODO Auto-generated method stub
		return props.keySet().toArray(new String[0]);
	}

	@Override
	public String getProperty(String name) {
		// TODO Auto-generated method stub
		return props.get(name);
	}

	public String getPort() {
		return props.get("port");
	}

	public String getHost() {
		return props.get("host");
	}

	@Override
	public int connectMaxCount() {
		return pool.capacity();
	}

	@Override
	public final void close() {

		// graph.dispose();
		site = null;
		if (group != null)
			group.shutdownGracefully();
		// props.clear();//关闭不清除属性，以支持netsite这些上层容器刷新连接的功能
		props.put("status", "closed");
		graphinitedEvents.clear();
		pool.dispose();

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
	public String status() {
		// TODO Auto-generated method stub
		return props.get("status");
	}

	@Override
	public int busyCount() {
		return pool.busyCount();
	}

	@Override
	public int idleCount() {
		return pool.idleCount();
	}

	@Override
	public int connectCount() {
		return pool.busyCount() + pool.idleCount();
	}

	@Override
	// 选连接一次，以检测是否能连接到远程，如果ok，则说明client启动了，之后再调用connect将无效。
	// 调用该方法后，将使用cluster来管理和申请新信道
	public final Channel connect(String host, String port)
			throws InterruptedException {
		return connect(host, port, null);
	}
	@Override
	public Channel connect(String host, String port, IConnectCallback callback)
			throws InterruptedException {
		if ("connected".equals(props.get("status"))
				&& host.equals(props.get("host")) && port.equals("port")) {
			throw new RuntimeException(String.format(
					"该客户端已连接到%s:%s,不能再连接到其它终端。", host, port));
		}
		// CjService service = this.getClass().getAnnotation(CjService.class);
		// String serviceName = service.name();
		// if (StringUtil.isEmpty(serviceName)) {
		// throw new RuntimeException("服务名为空，必须声明为服务，或用注解方式：@CjService()  ");
		// }
		if (StringUtil.isEmpty(port))
			throw new RuntimeException("错误：没有指定端口");
		if (StringUtil.isEmpty(host))
			throw new RuntimeException("错误：没有指定host");
		if (!props.containsKey("host"))
			props.put("host", host);
		if (!props.containsKey("port"))
			props.put("port", port);
		Channel ch = null;
		if (!"connected".equals(props.get("status"))) {
			pool = newPool(props);
			ch = init(pool,callback);
		} else {
			ch = connectServer(b, getChannelClass());
		}
		return ch;
	}
	protected NioEventLoopGroup createGroup(int threadCount) {
		return new NioEventLoopGroup(threadCount); // (1)
	}

	protected void initProperties(Bootstrap b, Map<String, String> propMap) {
		String SO_BACKLOG = propMap.get("SO_BACKLOG");
		if (StringUtil.isEmpty(SO_BACKLOG))
			SO_BACKLOG = "1024";
		b.option(ChannelOption.SO_BACKLOG, Integer.valueOf(SO_BACKLOG));
	}

	// 第一次连接后晌应初始化.
	@SuppressWarnings("unchecked")
	protected Channel init(IChannelPool pool,IConnectCallback callback) throws NumberFormatException,
			InterruptedException {

		// 如果指定的creator则规换掉默认的图的creator
//		GraphCreator creator = createNetGraphCreator();
//		if (creator != null)
//			getNetGraph().setCreator(creator);
		if (buildNetGraph() != null) {
			((NetGraph) graph).setChipSite(site);
			graph.initGraph();
			((NetGraph) graph).options("netType", this.getClass()
					.getSimpleName());
			((NetGraph) graph).options("isNet",true);
		}
		this.group = createGroup(pool.capacity() + 1);//加1是用1个线程检测信道池的空闲超时。
		b = new Bootstrap();
		initProperties(b, props);
		b.group(group);
		ChannelFactory<? extends Channel> factory = getChannelFactory();
		if (factory != null) {
			b.channelFactory(getChannelFactory());
		}
		Class<? extends Channel> chclass = getChannelClass();
		if (chclass != null)
			b.channel(chclass);
		IChannelInitializer<C> ci = createChannel();
		b.handler(new DefaultChannelInitializer<C>(ci));
		if (pool.idleCheck() > 0) {
			group.scheduleWithFixedDelay(pool, pool.idleCheck(),
					pool.idleCheck(), TimeUnit.MILLISECONDS);
		}
		if(callback!=null){
			callback.buildGraph(this,buildNetGraph());
		}
		Channel ch = connectServer(b, chclass);
		props.put("status", "connected");
		for (INetGraphInitedEvent event : graphinitedEvents) {
			event.graphInited(this);
		}
		return ch;
	}

	protected IChannelPool newPool(Map<String, String> props) {
		int capcity = 1;
		long idleTimeout = -1;// 空闲休眠时间，空闲在池中超出此时间则关闭连接。默认－1永远休眠，表示空闲连接不被释放
		long idleCheck = -1;
		long activeTimeout = -1;
		String pidle = props.get("idle");
		if (!StringUtil.isEmpty(pidle)) {
			idleTimeout = new Long(pidle);
		}
		String pidleCheck = props.get("idleCheck");
		if (!StringUtil.isEmpty(pidleCheck)) {
			idleCheck = new Long(pidleCheck);
		}
		String pactiveTimeout = props.get("activeTimeout");
		if (!StringUtil.isEmpty(pactiveTimeout)) {
			activeTimeout = new Long(pactiveTimeout);
		}
		String works = props.get("work");
		if (!StringUtil.isEmpty(works)) {
			capcity = new Integer(works);
		}
		IChannelPool pool = new ChannelPool(this, capcity, idleTimeout,
				idleCheck, activeTimeout);

		return pool;
	}

	protected abstract Channel connectServer(Bootstrap b,
			Class<? extends Channel> chClazz) throws NumberFormatException,
			InterruptedException;

	protected ChannelFactory<? extends Channel> getChannelFactory() {
		return null;
	}

	protected abstract Class<? extends Channel> getChannelClass();

	/**
	 * 创建自定义channel，派生类可覆盖
	 * 
	 * @return
	 */
	protected abstract IChannelInitializer<C> createChannel();

	class DefaultChannelInitializer<T extends Channel> extends
			ChannelInitializer<T> {
		IChannelInitializer<T> initializer;

		public DefaultChannelInitializer(IChannelInitializer<T> initializer) {
			this.initializer = initializer;
		}

		@Override
		protected void initChannel(T ch) throws Exception {
			try {
				long idleCheckin = -1;
				String ici = props.get("idleCheckin");
				if (!StringUtil.isEmpty(ici))
					idleCheckin = Long.valueOf(ici);
				IHandleBinder hb = initializer.initChannel(ch, idleCheckin);
				if (hb == null)
					return;
				hb.init((INettyGraph)buildNetGraph());
			} catch (Exception e) {
				logger.error(e.getMessage());
				throw new RuntimeException(e);
			}
		}
	}
	@Override
	public String netName() {
		CjService s = BaseClientNIO.this.getClass().getAnnotation(
				CjService.class);
		if (s == null) {
			String name = BaseClientNIO.this.getProperty("clientName");
			if (StringUtil.isEmpty(name))// 如果为空则以端口号作为名字
				name = BaseClientNIO.this.simple() + "$"
						+ BaseClientNIO.this.getProperty("host") + "#"
						+ BaseClientNIO.this.getProperty("port");
			return name;
		}

		return s.name();
	}
	class NetGraph extends Graph implements INettyGraph {
		int index;
		ISink sender;
		ISink sync;
		@Override
		public String name() {
			return netName();
		}
		@Override
		public void dispose() {
			super.dispose();
		}
		// 每调用一次产生一个输入端子且为之分配一个信道,如果信道池中已无空闲端子且到上限则堵塞，直到有可用为止。
		@Override
		public IPin netInput() {
			String id=IdGenerator.newInstance().asShortText();
			while(creator().containsInputPin("input$"+id)){
				id=IdGenerator.newInstance().asShortText();
			}
			IPin clientPin = creator().newWirePin("input$"+id, Access.input);
			clientPin.plugLast("sync", sync);
			ISink sender = creator().newSink("sender");
			IPlug sendPlug = clientPin.plugLast("sender", sender);
			try {
				Circuit c = new Circuit("setChannel/1.0 200 ok");
				c.attribute("channel-pool",pool);
				sender.flow(null,c, sendPlug);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return clientPin;
		}
		@Override
		protected GraphCreator newCreator() {
			GraphCreator gc=createNetGraphCreator();
			this.setChipSite(site);
			return gc;
		}
		@Override
		public IPin netOutput() {
			return out("output");
		}

		@Override
		public IPin newAcceptorPin() {
			IPin inputPin = creator().newWirePin("input_" + (index++));
			inputPin.plugLast("acceptor", creator().newSink("acceptor"));
			IPlug synPlug = inputPin.plugLast("syn", sync);
			IPin out = out("output");
			synPlug.plugBranch("output", out);
			return inputPin;
		}

		@Override
		protected void build(GraphCreator creator) {
			sync = creator.newSink("sync");
			creator().newWirePin("output", Access.output);
		}

	}
}
