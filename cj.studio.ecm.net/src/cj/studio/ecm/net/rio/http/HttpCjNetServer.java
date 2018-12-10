package cj.studio.ecm.net.rio.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.Access;
import cj.studio.ecm.graph.AnnotationProtocolFactory;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.Graph;
import cj.studio.ecm.graph.GraphCreator;
import cj.studio.ecm.graph.ICablePin;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.IProtocolFactory;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.graph.ISinkCreateBy;
import cj.studio.ecm.net.INetGraphInitedEvent;
import cj.studio.ecm.net.INetGraphStopedEvent;
import cj.studio.ecm.net.IServer;
import cj.studio.ecm.net.graph.INetGraph;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.nio.netty.http.SenderFrameHelper;
import cj.ultimate.util.StringUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * http实现
 * 
 * <pre>
 * </pre>
 * 
 * @author carocean
 *
 */
public class HttpCjNetServer implements IServer, NetConstans {

	private NioEventLoopGroup serverGroup;
	private Map<String, String> props;
	private List<INetGraphInitedEvent> graphinitevents;
	private List<INetGraphStopedEvent> graphStopedEvents;
	INetGraph g;
	private NioEventLoopGroup workGroup;

	public HttpCjNetServer() {
		props = new HashMap<String, String>();
		graphinitevents = new ArrayList<INetGraphInitedEvent>(1);
		graphStopedEvents = new ArrayList<INetGraphStopedEvent>(1);
	}

	@Override
	public String netName() {
		CjService s = this.getClass().getAnnotation(CjService.class);
		if (s == null) {
			String name = this.getProperty("serverName");
			if (StringUtil.isEmpty(name))// 如果为空则以端口号作为名字
				name = this.simple() + "-" + this.getProperty("port");
			return name;
		}

		return s.name();
	}

	@Override
	public String simple() {
		return "rio-http";
	}

	@Override
	public void start(String port) {
		start("localhost", port);
	}

	@Override
	public void start(String inetHost, String port) {
		if ("listening".equals(props.get("status"))) {
			throw new EcmException("服务器已运行");
		}
		props.put("port", port);
		if (!StringUtil.isEmpty(inetHost)) {
			props.put("inetHost", inetHost);
		} else {
			props.put("inetHost", "localhost");
		}
		props.put("protocol", simple());
		String bossThreadCount = props.get("bossThreadCount");
		if (StringUtil.isEmpty(bossThreadCount)) {
			bossThreadCount = "1";
			props.put("bossThreadCount", bossThreadCount);
		}
		String workThreadCount = props.get("workThreadCount");
		if (StringUtil.isEmpty(workThreadCount)
				|| "0".equals(workThreadCount)) {
			int cpuCount = Runtime.getRuntime().availableProcessors();
			workThreadCount = String.valueOf(cpuCount);
			props.put("workThreadCount", workThreadCount);
		}

		serverGroup = new NioEventLoopGroup(
				Integer.valueOf(bossThreadCount));
		int workthreadcount=Integer.valueOf(workThreadCount);
		if(workthreadcount<0){
			//netty在NioEventLoopGroup的基类的构造中强制在启动时构造了子监视数为线程池大小，且不可改变，因此本句既便采用了newCachedThreadPool仍然突破不了线程数限制。
			//连从NioEventLoopGroup派生也被限死了，因为其中一个类又弄成了内部类。
			int cpuCount = Runtime.getRuntime().availableProcessors();
			workThreadCount = String.valueOf(cpuCount);
			props.put("workThreadCount", workThreadCount);
			workGroup = new NioEventLoopGroup(cpuCount,Executors.newCachedThreadPool());
		}else{
			workGroup = new NioEventLoopGroup(workthreadcount);
		}
		
		
		ServerBootstrap sb = new ServerBootstrap();
		String SO_KEEPALIVE = props.get("child.SO_KEEPALIVE");
		if (StringUtil.isEmpty(SO_KEEPALIVE))
			SO_KEEPALIVE = "true";
		sb.group(serverGroup, workGroup).channel(NioServerSocketChannel.class)
				.handler(new ChannelInitializer<NioServerSocketChannel>() {
					@Override
					public void initChannel(NioServerSocketChannel ch)
							throws Exception {
						// ch.pipeline()
						// .addLast(new LoggingHandler(LogLevel.INFO));
					}
				}).childHandler(new HttpChannelInit(this))
				.childOption(ChannelOption.SO_KEEPALIVE,
						Boolean.valueOf(SO_KEEPALIVE));

		Channel ch;
		try {
			if (StringUtil.isEmpty(getINetHost())||"localhost".equals(getINetHost())) {
				ch = sb.bind(Integer.valueOf(getPort())).sync().channel();
			} else {
				ch = sb.bind(getINetHost(), Integer.valueOf(getPort())).sync()
						.channel();
			}
			ch.closeFuture();
		} catch (InterruptedException e) {
			throw new EcmException(e);
		}
		props.put("status", "listening");
		for (INetGraphInitedEvent event : graphinitevents) {
			event.graphInited(this);
		}
	}

	@Override
	public void stop() {
		if ("stoped".equals(this.props.get("status"))) {
			return;
		}
		this.serverGroup.shutdownGracefully();
		this.workGroup.shutdownGracefully();
		// 放在inactive中释放它
		// INetGraph cg = buildNetGraph();
		// cg.dispose();
		this.props.put("status", "stoped");
	}

	@Override
	public void removeProperty(String propName) {
		props.remove(propName);
	}

	@Override
	public String getINetHost() {
		return props.get("inetHost");
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
		return props.get(name);
	}

	@Override
	public String[] enumProp() {
		String[] at = props.keySet().toArray(new String[0]);
		return at;
	}

	@Override
	public INetGraph buildNetGraph() {
		if (g == null) {
			g = new SwitchGraph();
			g.initGraph();
		}
		return g;
	}

	@Override
	public void eventNetGraphInited(INetGraphInitedEvent event) {
		graphinitevents.add(event);
	}

	@Override
	public void eventNetGraphStoped(INetGraphStopedEvent event) {
		this.graphStopedEvents.add(event);
	}

	public class SwitchGraph extends Graph implements INetGraph {

		@Override
		public IPin netInput() {
			return in("input");
		}

		@Override
		public IPin netOutput() {
			return out("output");
		}

		@Override
		protected void build(GraphCreator creator) {
			ICablePin input = creator.newCablePin("input", Access.input);
			ICablePin output = creator.newCablePin("output", Access.output);
			input.plugLast("input#http", creator.newSink("input#http"));
			output.plugLast("output#http", creator.newSink("output#http"));

		}

		@Override
		protected GraphCreator newCreator() {
			return new SwitchGraphCreator();
		}

		class SwitchGraphCreator extends GraphCreator {
			@Override
			protected IProtocolFactory newProtocol() {
				return AnnotationProtocolFactory.factory(NetConstans.class);
			}

			@Override
			protected ISink createSink(String sink) {
				switch (sink) {
				case "input#http":
					return new InputHttp();
				case "output#http":
					return new OutputHttp();
				}
				return null;
			}

		}

		class InputHttp extends SenderFrameHelper implements ISinkCreateBy {

			public InputHttp() {
			}

			@Override
			public ISink newSink(String sinkName, IPin owner) {
				return new InputHttp();
			}

			@Override
			public void flow(Frame frame, Circuit circuit, IPlug plug)
					throws CircuitException {
				plug.optionsPin(NetConstans.PIN_WORK_STATUS_KEY, "busy");

				try {
					Channel ch=(Channel)plug.option("channel");
					sendWsMsg(ch, frame);
				} catch (Exception e) {
					throw e;
				} finally {
					plug.optionsPin(NetConstans.PIN_WORK_STATUS_KEY, "idle");
				}
			}

		}

		class OutputHttp implements ISinkCreateBy {

			@Override
			public ISink newSink(String sinkName, IPin owner) {
				return new OutputHttp();
			}

			@Override
			public void flow(Frame frame, Circuit circuit, IPlug plug)
					throws CircuitException {
				plug.flow(frame, circuit);

			}

		}

	}

}
