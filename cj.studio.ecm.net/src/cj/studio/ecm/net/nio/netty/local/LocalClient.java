package cj.studio.ecm.net.nio.netty.local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.frame.IFeedback;
import cj.studio.ecm.graph.Access;
import cj.studio.ecm.graph.AnnotationProtocolFactory;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.Graph;
import cj.studio.ecm.graph.GraphCreator;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.IProtocolFactory;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.IClient;
import cj.studio.ecm.net.IConnectCallback;
import cj.studio.ecm.net.INetGraphInitedEvent;
import cj.studio.ecm.net.ISequenceNameGenerator;
import cj.studio.ecm.net.SyncPool;
import cj.studio.ecm.net.graph.INetGraph;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.nio.netty.INettyGraph;
import cj.studio.ecm.net.nio.netty.udt.UdtClientSync;
import cj.ultimate.util.StringUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.nio.NioEventLoopGroup;

public class LocalClient implements IClient {
	private Map<String, String> props;
	protected List<INetGraphInitedEvent> graphinitedEvents;
	private NetGraph g;
	NioEventLoopGroup group;

	public LocalClient() {
		props = new HashMap<String, String>();
		graphinitedEvents = new ArrayList<>();
		props.put("status", "inited");
	}

	@Override
	public String status() {
		return props.get("status");
	}

	@Override
	public void setProperty(String key, String value) {
		props.put(key, value);
	}

	@Override
	public String getPort() {
		return props.get("port");
	}

	@Override
	public String getHost() {
		return props.get("host");
	}

	@Override
	public void close() {
		props.clear();
		props.put("status", "stoped");
		group.shutdownGracefully();
	}

	@Override
	public String getProperty(String name) {

		return props.get(name);
	}

	@Override
	public String[] enumProp() {

		return props.keySet().toArray(new String[0]);
	}

	protected void initProperties(Bootstrap b, Map<String, String> propMap) {
		String SO_BACKLOG = propMap.get("SO_BACKLOG");
		if (StringUtil.isEmpty(SO_BACKLOG))
			SO_BACKLOG = "1024";
		b.option(ChannelOption.SO_BACKLOG, Integer.valueOf(SO_BACKLOG));
	}
	@Override
	public Object connect(String ip, String port)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return connect(ip, port, null);
	}
	@Override
	public Object connect(String ip, String port, IConnectCallback callback) throws InterruptedException {
		if (!props.containsKey("host")) {
			props.put("host", ip);
		}
		if (!props.containsKey("port")) {
			props.put("port", port);
		}
		LocalAddress addr = new LocalAddress(port);
		Bootstrap b = new Bootstrap();
		initProperties(b, props);
		int bossThreadCount = 0;
		if (props.containsKey("bossThreadCount")) {
			bossThreadCount = Integer.valueOf(props.get("bossThreadCount"));
		} else {
			props.put("bossThreadCount", String.valueOf(bossThreadCount));
		}
		group = new NioEventLoopGroup(bossThreadCount);
		b.group(group).channel(LocalChannel.class).handler(new ChannelInitializer<LocalChannel>() {
			@Override
			protected void initChannel(LocalChannel ch) throws Exception {
				INetGraph g = buildNetGraph();
				if (!g.isInit()) {
					g.initGraph();
				}
				g.options("channel", ch);
				ConnectorHandler handler = new ConnectorHandler(g.netOutput());
				ch.pipeline().addLast(handler);
			}
		});
		if(callback!=null){
			callback.buildGraph(this,buildNetGraph());
		}
		ChannelFuture f = b.connect(addr).sync();
		try {
			f.channel();
			f.await(NetConstans.waitClientConnectTimeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			
		}
		props.put("status", "connected");
		for (INetGraphInitedEvent event : graphinitedEvents) {
			event.graphInited(this);
		}
		return f;
	}

	@Override
	public INetGraph buildNetGraph() {
		if (g == null) {
			g = new NetGraph();
		}
		if (!g.isInit()) {
			g.initGraph();
		}
		return g;
	}

	@Override
	public String simple() {
		return "local";
	}

	@Override
	public String netName() {
		CjService s = LocalClient.this.getClass().getAnnotation(CjService.class);
		if (s == null) {
			String name = LocalClient.this.getProperty("clientName");
			if (StringUtil.isEmpty(name))// 如果为空则以端口号作为名字
				name = LocalClient.this.simple() + "$" + LocalClient.this.getProperty("host") + "#"
						+ LocalClient.this.getProperty("port");
			return name;
		}

		return s.name();
	}

	class ConnectorHandler extends ChannelHandlerAdapter {
		IPin pin;

		public ConnectorHandler(IPin netOutput) {
			pin = netOutput;
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			try {
				Frame frame = (Frame) msg;
				Circuit circuit = new Circuit("net/1.1 200 ok");
				initCircuit(circuit, ctx);
				circuit.attribute("ctx", ctx);
				pin.flow(frame, circuit);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			Frame frame = new Frame("connect / net/1.1");
			Circuit circuit = new Circuit("net/1.1 200 ok");
			initCircuit(circuit, ctx);
			circuit.attribute("ctx", ctx);
			try {
				pin.flow(frame, circuit);
				frame.dispose();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private void initCircuit(Circuit circuit, ChannelHandlerContext ctx) {
			circuit.head("select-type", "client");
			circuit.head("local-address", ctx.channel().localAddress().toString());
			circuit.head("remote-address", ctx.channel().remoteAddress().toString());
			circuit.head("select-name", buildNetGraph().name());
			circuit.head("select-simple", "local");
			circuit.head("select-id", ctx.channel().id().asShortText());
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			Frame frame = new Frame("disconnect / net/1.1");
			Circuit circuit = new Circuit("net/1.1 200 ok");
			initCircuit(circuit, ctx);
			circuit.attribute("ctx", ctx);
			try {
				pin.flow(frame, circuit);
				frame.dispose();
			} catch (Exception e) {
				throw new EcmException(e);
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			super.exceptionCaught(ctx, cause);
		}

	}

	class NetGraph extends Graph implements INettyGraph {
		@Override
		protected GraphCreator newCreator() {

			return new NetGraphCreator();
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
		public IPin newAcceptorPin() {

			return null;
		}

		@Override
		protected void build(GraphCreator c) {
			IPin input = c.newWirePin("input", Access.input);
			IPin output = c.newWirePin("output", Access.output);
			input.plugLast("netInputSink", c.newSink("netInputSink"));
			output.plugLast("netOutputSink", c.newSink("netOutputSink"));
		}

	}

	class MessageIdGen implements ISequenceNameGenerator {
		int pos;
		int max;
		int begin;

		public MessageIdGen() {
			max = Integer.MAX_VALUE;
		}

		@Override
		public String genName() {
			if (pos < max)
				pos++;
			else {
				pos = begin + 1;
			}
			return pos + "";
		}

	}

	class NetGraphCreator extends GraphCreator implements NetConstans {
		SyncPool pool;
		ISequenceNameGenerator gen;
		Logger log = Logger.getLogger(UdtClientSync.class);

		public NetGraphCreator() {
			pool = new SyncPool(2000);
			gen = new MessageIdGen();
		}

		@Override
		protected IProtocolFactory newProtocol() {
			return AnnotationProtocolFactory.factory(NetConstans.class);
		}

		@Override
		protected ISink createSink(String sink) {
			if ("netInputSink".equals(sink)) {
				return new NetInputSink(gen, pool);
			}
			if ("netOutputSink".equals(sink)) {
				return new NetOutputSink(gen, pool);
			}
			return null;
		}

	}

	class NetOutputSink implements ISink, NetConstans {
		ISequenceNameGenerator gen;
		SyncPool pool;

		public NetOutputSink(ISequenceNameGenerator gen, SyncPool pool) {
			this.gen = gen;
			this.pool = pool;
		}

		@Override
		public void flow(Frame frame, Circuit circuit, IPlug plug) throws CircuitException {
			if ("NET/1.1".equals(frame.protocol()) && !"piggyback".equals(frame.command())) {
				plug.flow(frame, circuit);
				return;
			}
			String msgId = frame.head(NetConstans.FRAME_HEADKEY_FRAME_ID);
			if (frame.containsHead(NetConstans.FRAME_HEADKEY_FRAME_ID) && pool.contains(msgId)) {
				// 同步消息到，通知解锁线程
				pool.notifya(msgId, frame);
				frame.removeHead(NetConstans.FRAME_HEADKEY_FRAME_ID);
				return;
			}
			plug.flow(frame, circuit);
		}
	}

	class NetInputSink implements ISink, NetConstans {
		ISequenceNameGenerator gen;
		SyncPool pool;

		public NetInputSink(ISequenceNameGenerator gen, SyncPool pool) {
			this.gen = gen;
			this.pool = pool;
		}

		@Override
		public void flow(Frame frame, Circuit circuit, IPlug plug) throws CircuitException {
			Channel ch = (Channel) plug.optionsGraph("channel");
			if ("true".equals(frame.head(NetConstans.FRAME_HEADKEY_CIRCUIT_SYNC))) {
				waitFlow(frame, circuit, ch);
				return;
			}
			ch.writeAndFlush(frame);
		}

		private void waitFlow(Frame frame, Circuit circuit, Channel ch) throws CircuitException {
			String msgId = gen.genName();
			frame.head(FRAME_HEADKEY_FRAME_ID, msgId);
			//
			if (pool.put(msgId)) {
				frame.removeHead(FRAME_HEADKEY_CIRCUIT_SYNC);
				ch.writeAndFlush(frame);
				String syn_timeout = frame.head(NetConstans.FRAME_HEADKEY_CIRCUIT_SYNC_TIMEOUT);
				if (StringUtil.isEmpty(syn_timeout)) {
					syn_timeout = "3600";
				}
				Long l = Long.valueOf(syn_timeout);
				Frame back = null;
				back = (Frame) pool.take(msgId, l);
				frame.removeHead(FRAME_HEADKEY_FRAME_ID);
				if (back != null) {
					back.removeHead(FRAME_HEADKEY_FRAME_ID);
					circuit.contentType("frame/bin");
					circuit.content().writeBytes(back.toBytes());
					circuit.status(frame.head("status"));
					circuit.message(frame.head("message"));
					// c.fireCallback(back);
					if (circuit.containsFeedback(IFeedback.KEY_INPUT_FEEDBACK)) {
						circuit.feedback(IFeedback.KEY_INPUT_FEEDBACK).doBack(back, circuit);
					}

				} else {
					circuit.status(STATUS_202);
					circuit.message("等待超时");
					String cause = String.format(
							"原因是等待超时，超时时间：%s毫秒，默认是3600毫秒。\r\n可以在frame头中指定：cj-sync-timeout，可以设置每侦的等待时间", syn_timeout);
					circuit.content().writeBytes(cause.getBytes());
				}
			}

		}
	}
}
