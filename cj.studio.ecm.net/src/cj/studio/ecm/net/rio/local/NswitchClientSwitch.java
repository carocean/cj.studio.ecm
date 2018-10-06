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
import cj.studio.ecm.graph.ICablePinEvent;
import cj.studio.ecm.graph.ICableWireCreateCallback;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.IProtocolFactory;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.graph.ISinkCreateBy;
import cj.studio.ecm.graph.IWirePin;
import cj.studio.ecm.net.IClient;
import cj.studio.ecm.net.IConnectCallback;
import cj.studio.ecm.net.IRioClientCloseEvent;
import cj.studio.ecm.net.IRioClientCloseEventable;
import cj.studio.ecm.net.graph.INetGraph;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.web.HttpCircuit;
import cj.studio.ecm.net.web.HttpFrame;
import cj.ultimate.IDisposable;
import cj.ultimate.util.StringUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.nio.NioEventLoopGroup;

public class NswitchClientSwitch implements IClient,IRioClientCloseEventable {
	private Map<String, String> props;
	private SwitchGraph g;
	private NioEventLoopGroup clientGroup;
	List<IRioClientCloseEvent> events;
	public NswitchClientSwitch() {
		props = new HashMap<String, String>();
		events=new ArrayList<>();
		props.put("status", "inited");
	}
	@Override
	public void onEvent(IRioClientCloseEvent e) {
		events.add(e);
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
	public String getProperty(String name) {
		return props.get(name);
	}

	@Override
	public String[] enumProp() {
		String[] at = props.keySet().toArray(new String[0]);
		return at;
	}

	@Override
	public void close() {
		// 通知switch服务器断开连接，事件
		if ("stoped".equals(status())) {
			return;
		}
		for(IRioClientCloseEvent e:events){
			e.onClosing(this);
		}
		clientGroup.shutdownGracefully();
		// INetGraph cg = buildNetGraph();
		// cg.dispose();
		// props.clear();
		props.put("status", "stoped");
	}

	@Override
	public Object connect(String ip, String port) throws InterruptedException {
		return connect(ip, port, null);
	}

	@Override
	public Object connect(String ip, String port, IConnectCallback callback)
			throws InterruptedException {
		if ("listening".equals(props.get("status"))) {
			// throw new EcmException("连接已运行");
		}
		if (!props.containsKey("host")) {
			props.put("host", ip);
		}
		if (!props.containsKey("port")) {
			props.put("port", port);
		}

		LocalAddress addr = new LocalAddress(port);

		String bossThreadCount = "1";
		props.put("bossThreadCount", bossThreadCount);
		String workThreadCount = props.get("workThreadCount");
		if (StringUtil.isEmpty(workThreadCount)
				|| "0".equals(workThreadCount)) {
			workThreadCount = "1";
			props.put("workThreadCount", workThreadCount);
		}
		int workThreadCountV = Integer.valueOf(workThreadCount);

		if (callback != null) {
			callback.buildGraph(this, buildNetGraph());
		}
		ICablePin input=(ICablePin)buildNetGraph().netInput();
		input.onEvent(new ICablePinEvent() {
			
			@Override
			public void onNewWired(ICablePin owner, String name, IWirePin wire) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onDestoryWired(ICablePin owner, String name, IWirePin wire) {
				if(owner.wireCount()<1)
				NswitchClientSwitch.this.close();
			}
			
			@Override
			public void onDestoringWired(ICablePin owner, String name, IWirePin wire) {
				// TODO Auto-generated method stub
				
			}
		});
		clientGroup = new NioEventLoopGroup(workThreadCountV);

		for (int i = 0; i < workThreadCountV; i++) {
			Bootstrap cb = new Bootstrap();
			cb.group(clientGroup).channel(LocalChannel.class)
					.handler(new ChannelInitializer<LocalChannel>() {
						@Override
						public void initChannel(LocalChannel ch)
								throws Exception {
							// System.out.println(ch);
							ch.pipeline().addLast(
//									new LoggingHandler(LogLevel.INFO),
									new LocalEchoClientHandler());
						}
					});
			ChannelFuture f = cb.connect(addr).sync();
			try {
				f.channel();
				f.await(NetConstans.waitClientConnectTimeout,
						TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {

			}
		}
		props.put("status", "listening");
		// for (INetGraphInitedEvent event : graphinitedEvents) {
		// event.graphInited(this);
		// }
		return null;
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
	public String simple() {
		return "n-switch";
	}

	@Override
	public String netName() {
		CjService s = this.getClass().getAnnotation(CjService.class);
		if (s == null) {
			String name = this.getProperty("clientName");
			if (StringUtil.isEmpty(name))// 如果为空则以端口号作为名字
				name = this.simple() + "$" + this.getHost() + "#" + getPort();
			return name;
		}

		return s.name();
	}

	public class LocalEchoClientHandler
			extends SimpleChannelInboundHandler<Object> {

		@Override
		public void messageReceived(ChannelHandlerContext ctx, Object msg)
				throws Exception {
			// Print as received
			Object[] arr = (Object[]) msg;
			Frame frame = (Frame) arr[0];
			Circuit circuit = (Circuit) arr[1];

			String id = ctx.channel().id().asShortText();

			initCircuit(circuit, ctx);

			ClassLoader old = Thread.currentThread().getContextClassLoader();
			
			Condition condition=null;
			ReentrantLock locker=null;
			
			try {
				circuit.feedbackRemove(IFeedback.KEY_OUTPUT_FEEDBACK);
				circuit.feedbackSetSource(IFeedback.KEY_OUTPUT_FEEDBACK);
				ISink end = new SwitchFeedbackEndSink();
				IFeedback b = circuit.feedback(IFeedback.KEY_OUTPUT_FEEDBACK);
				IPlug plug=b.plugSink(IFeedback.KEY_ENDSINK_FEEDBACK, end)
						.option("channel", ctx.channel());
				if(circuit.containAtrribute("locker-condition")){
					condition=(Condition)circuit.attribute("locker-condition");
					locker=(ReentrantLock)circuit.attribute("locker");
					circuit.removeAttribute("locker-condition");
					circuit.removeAttribute("locker");
					plug.option("locker",locker);
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
				if (circuit.isPiggybacking()||(condition!=null&&locker!=null)) {
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
			circuit.attribute("transfer-protocol", "NET/1.1");
			circuit.head("select-type", "client");
			circuit.head("local-address",
					ctx.channel().localAddress().toString());
			circuit.head("remote-address",
					ctx.channel().remoteAddress().toString());
			circuit.head("select-name", netName());
			circuit.head("select-simple", "n-switch");
			circuit.head("select-id", ctx.channel().id().asShortText());
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
				throws Exception {
			super.exceptionCaught(ctx, cause);
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

	class InputSwitch implements ISinkCreateBy, NetConstans, IDisposable {
		ReentrantLock locker;

		public InputSwitch() {
			locker = new ReentrantLock();
		}

		@Override
		public void dispose() {
			locker = null;
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
				c = new Circuit(String.format("%s 200 ok", frame.protocol()));
			}
			Condition loker_condition = null;
			if (frame.containsHead(FRAME_HEADKEY_CIRCUIT_SYNC)) {
				loker_condition = locker.newCondition();
				c.attribute("locker-condition", loker_condition);
				c.attribute("locker", locker);
			}
			try{
			ch.writeAndFlush(new Object[] { f, c });

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
					circuit.copyFrom(c, false);
				} catch (InterruptedException e) {
					throw new EcmException(e);
				} finally {
					c.dispose();
					locker.unlock();
				}
			}
			if (circuit.containsFeedback(IFeedback.KEY_INPUT_FEEDBACK)) {
				IFeedback fb = circuit.feedback(IFeedback.KEY_INPUT_FEEDBACK);
				Frame back = null;
				if ("frame/bin".equals(circuit.contentType())
						|| "frame/json".equals(circuit.contentType())) {
					back = new Frame(circuit.content().readFully());
				} else {
					back = circuit.snapshot("piggyback /");
				}
				fb.doBack(back, circuit);
			}
			}catch(Exception e){
				throw e;
			}finally{
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

	class SwitchFeedbackEndSink implements ISink,NetConstans {
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
