package cj.studio.ecm.net.rio.local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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
import cj.studio.ecm.graph.ICablePin;
import cj.studio.ecm.graph.ICableWireCreateCallback;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.IProtocolFactory;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.graph.ISinkCreateBy;
import cj.studio.ecm.graph.IWirePin;
import cj.studio.ecm.net.INetGraphInitedEvent;
import cj.studio.ecm.net.INetGraphStopedEvent;
import cj.studio.ecm.net.IServer;
import cj.studio.ecm.net.graph.INetGraph;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.web.HttpCircuit;
import cj.studio.ecm.net.web.HttpFrame;
import cj.ultimate.util.StringUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * 交换机
 * 
 * <pre>
 * 用于在neuron,netsite中交换graph的请求，以请求中的协议作为导向
 * </pre>
 * 
 * @author carocean
 *
 */
public class NswitchServerSwitch implements IServer, NetConstans {

	private DefaultEventLoopGroup serverGroup;
	private Map<String, String> props;
	private List<INetGraphInitedEvent> graphinitevents;
	private List<INetGraphStopedEvent> graphStopedEvents;
	INetGraph g;
	private NioEventLoopGroup workGroup;

	public NswitchServerSwitch() {
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
		return "n-switch";
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

		serverGroup = new DefaultEventLoopGroup(
				Integer.valueOf(bossThreadCount));
		workGroup = new NioEventLoopGroup(Integer.valueOf(workThreadCount));
		ServerBootstrap sb = new ServerBootstrap();
		sb.group(serverGroup, workGroup).channel(LocalServerChannel.class)
				.handler(new ChannelInitializer<LocalServerChannel>() {
					@Override
					public void initChannel(LocalServerChannel ch)
							throws Exception {
						// ch.pipeline()
						// .addLast(new LoggingHandler(LogLevel.INFO));
					}
				}).childHandler(new ChannelInitializer<LocalChannel>() {
					@Override
					public void initChannel(LocalChannel ch) throws Exception {

						ch.pipeline()
								.addLast(/*new LoggingHandler(LogLevel.INFO),*/
										new LocalEchoServerHandler());

					}
				});
		LocalAddress addr = new LocalAddress(port);
		try {
			sb.bind(addr).sync();
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

	public class LocalEchoServerHandler extends ChannelHandlerAdapter {

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg)
				throws Exception {
			// 传引用的形式
			Object[] arr = (Object[]) msg;
			Frame frame = (Frame) arr[0];
			Circuit circuit = (Circuit) arr[1];

			String id = ctx.channel().id().asShortText();

			initCircuit(circuit, ctx);

			ClassLoader old = Thread.currentThread().getContextClassLoader();
			Condition condition = null;
			ReentrantLock locker = null;

			try {
				circuit.feedbackRemove(IFeedback.KEY_OUTPUT_FEEDBACK);
				circuit.feedbackSetSource(IFeedback.KEY_OUTPUT_FEEDBACK);
				ISink end = new SwitchFeedbackEndSink();
				IFeedback b = circuit.feedback(IFeedback.KEY_OUTPUT_FEEDBACK);
				IPlug plug = b.plugSink(IFeedback.KEY_ENDSINK_FEEDBACK, end)
						.option("channel", ctx.channel());
				if (circuit.containAtrribute("locker-condition")) {
					condition = (Condition) circuit
							.attribute("locker-condition");
					locker = (ReentrantLock) circuit.attribute("locker");
					circuit.removeAttribute("locker-condition");
					circuit.removeAttribute("locker");
					plug.option("locker", locker);
				}

				ICablePin output = (ICablePin) g.netOutput();
				output.flow(id, frame, circuit);
			} catch (Exception e) {
				throw e;
			} finally {
				Thread.currentThread().setContextClassLoader(old);

				if ("disconnect".equals(circuit.attribute("net-action"))) {
					ctx.channel().close();
				}
				frame.dispose();
				if (circuit.isPiggybacking() || (locker!=null&&condition != null)) {
					try {
						locker.lock();
						condition.signalAll();
					} catch (Exception e) {
						throw e;
					} finally {
						if(locker!=null)
						locker.unlock();
					}
				} else {
					circuit.dispose();
				}
			}
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			ICablePin input = (ICablePin) g.netInput();
			ICablePin output = (ICablePin) g.netOutput();
			String id = ctx.channel().id().asShortText();
			input.newWire(id, new ICableWireCreateCallback() {

				@Override
				public void createdWire(String name, IWirePin wire) {
					wire.options("channel", ctx.channel());
				}
			});
			output.newWire(id);
			Frame frame = new Frame("connect / net/1.1");
			Circuit circuit = new Circuit("net/1.1 200 ok");
			initCircuit(circuit, ctx);
			try {
				output.flow(id, frame, circuit);
			} catch (Exception e) {
				// CircuitException.print(e, log);
				throw new EcmException(e);
			}
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx)
				throws Exception {
			ICablePin input = (ICablePin) g.netInput();
			ICablePin output = (ICablePin) g.netOutput();
			String id = ctx.channel().id().asShortText();
			Frame frame = new Frame("disconnect / net/1.1");
			Circuit circuit = new Circuit("net/1.1 200 ok");
			initCircuit(circuit, ctx);
			try {
				output.flow(id, frame, circuit);
				input.removeWire(id);
				output.removeWire(id);
			} catch (Exception e) {
				throw new EcmException(e);
			}

		}

		private void initCircuit(Circuit circuit, ChannelHandlerContext ctx) {
			circuit.attribute("transfer-protocol", "net/1.1");
			circuit.attribute("select-type", "server");
			circuit.attribute("local-address",
					ctx.channel().localAddress().toString());
			circuit.attribute("remote-address",
					ctx.channel().remoteAddress().toString());
			circuit.attribute("select-name", netName());
			circuit.attribute("select-simple", "n-switch");
			circuit.attribute("select-id", ctx.channel().id().asShortText());
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx)
				throws Exception {
			ctx.flush();
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx,
				Throwable cause) {
			Channel ch = ctx.channel();
			Frame frame = new Frame("piggyback / net/1.1");
			Circuit c=new Circuit("net/1.1 200 ok");
//			AttributeKey<String> key = AttributeKey
//					.valueOf(NetConstans.FRAME_HEADKEY_FRAME_ID);
//			String mid = ctx.attr(key).get();
//			if (!StringUtil.isEmpty(mid)) {
//				frame.head(NetConstans.FRAME_HEADKEY_FRAME_ID, mid);
//			}
			CircuitException cir = CircuitException.search(cause);
			if (cir != null) {
				frame.head("status", cir.getStatus());
				frame.head("message", cause.getMessage().replace("\r\n", ""));
				String error = CircuitException.print(cause, null);
				if (error != null) {
					frame.content().writeBytes(error.getBytes());
				}
				ch.writeAndFlush(new Object[]{frame,c});
				return;
			}
			frame.head("status", "503");
			frame.head("message", cause.getMessage().replace("\r\n", ""));
			ch.writeAndFlush(new Object[]{frame,c});
		}
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
			input.plugLast("input#switch", creator.newSink("input#switch"));
			output.plugLast("output#switch", creator.newSink("output#switch"));

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
				case "input#switch":
					return new InputSwitch();
				case "output#switch":
					return new OutputSwitch();
				}
				return null;
			}

		}

		class InputSwitch implements ISinkCreateBy {
			ReentrantLock locker;

			public InputSwitch() {
				locker = new ReentrantLock();
			}

			@Override
			public ISink newSink(String sinkName, IPin owner) {
				return new InputSwitch();
			}

			@Override
			public void flow(Frame frame, Circuit circuit, IPlug plug)
					throws CircuitException {
				plug.optionsPin(NetConstans.PIN_WORK_STATUS_KEY, "busy");
				Channel ch = (Channel) plug.optionsPin("channel");
				Frame f = frame.copy();
				Circuit c = null;
				if (frame instanceof HttpFrame) {
					c = new HttpCircuit(
							String.format("%s 200 ok", frame.protocol()));
				} else {
					c = new Circuit(
							String.format("%s 200 ok", frame.protocol()));
				}

				Condition loker_condition = null;
				if (frame.containsHead(FRAME_HEADKEY_CIRCUIT_SYNC)) {
					loker_condition = locker.newCondition();
					c.attribute("locker-condition", loker_condition);
					c.attribute("locker", locker);
				}
				try {
					ch.writeAndFlush(new Object[] { f, c });

					if (loker_condition != null) {
						long syncTimeout = 3600;
						if (frame.containsHead(
								FRAME_HEADKEY_CIRCUIT_SYNC_TIMEOUT)) {
							String v = frame
									.head(FRAME_HEADKEY_CIRCUIT_SYNC_TIMEOUT);
							if (StringUtil.isEmpty(v)) {
								v = "3600";
							}
							long t = Long.valueOf(v);
							if (t >= FRAME_HEADKEY_CIRCUIT_SYNC_MAX_TIMEOUT) {
								throw NetConstans.newCircuitException(
										NetConstans.STATUS_607,
										NetConstans.FRAME_HEADKEY_CIRCUIT_SYNC_MAX_TIMEOUT);
							}
							if (t > syncTimeout) {
								syncTimeout = t;
							}
						}
						try {
							locker.lock();
							loker_condition.await(syncTimeout,
									TimeUnit.MILLISECONDS);
							circuit.copyFrom(c, false);
						} catch (InterruptedException e) {
							throw new EcmException(e);
						} finally {
							c.dispose();
							locker.unlock();
						}
					}

					if (circuit
							.containsFeedback(IFeedback.KEY_INPUT_FEEDBACK)) {
						IFeedback fb = circuit
								.feedback(IFeedback.KEY_INPUT_FEEDBACK);
						Frame back = null;
						if ("frame/bin".equals(circuit.contentType())
								|| "frame/json".equals(circuit.contentType())) {
							back = new Frame(circuit.content().readFully());
						} else {
							back = circuit.snapshot("piggyback /");
						}
						fb.doBack(back, circuit);
					}
				} catch (Exception e) {
					throw e;
				} finally {
					plug.optionsPin(NetConstans.PIN_WORK_STATUS_KEY, "idle");
				}
			}

		}

		class OutputSwitch implements ISinkCreateBy {

			@Override
			public ISink newSink(String sinkName, IPin owner) {
				return new OutputSwitch();
			}

			@Override
			public void flow(Frame frame, Circuit circuit, IPlug plug)
					throws CircuitException {
				plug.flow(frame, circuit);

			}

		}

	}

	class SwitchFeedbackEndSink implements ISink {
		@Override
		public void flow(Frame frame, Circuit circuit, IPlug plug)
				throws CircuitException {
			Channel cout = (Channel) plug.option("channel");
			ReentrantLock locker = (ReentrantLock) plug.option("locker");
			Condition loker_condition = null;
			if (frame.containsHead(FRAME_HEADKEY_CIRCUIT_SYNC)) {
				loker_condition = locker.newCondition();
				circuit.attribute("locker-condition", loker_condition);
				circuit.attribute("locker", locker);
			}

			cout.writeAndFlush(new Object[] { frame, circuit });

			if (loker_condition != null) {
				long syncTimeout = 3600;
				if (frame.containsHead(FRAME_HEADKEY_CIRCUIT_SYNC_TIMEOUT)) {
					String v = frame.head(FRAME_HEADKEY_CIRCUIT_SYNC_TIMEOUT);
					if (StringUtil.isEmpty(v)) {
						v = "3600";
					}
					long t = Long.valueOf(v);
					if (t >= FRAME_HEADKEY_CIRCUIT_SYNC_MAX_TIMEOUT) {
						throw NetConstans.newCircuitException(
								NetConstans.STATUS_607,
								NetConstans.FRAME_HEADKEY_CIRCUIT_SYNC_MAX_TIMEOUT);
					}
					if (t > syncTimeout) {
						syncTimeout = t;
					}
				}
				try {
					locker.lock();
					loker_condition.await(syncTimeout, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					throw new EcmException(e);
				} finally {
					locker.unlock();
				}
			}
		}
	}
}
