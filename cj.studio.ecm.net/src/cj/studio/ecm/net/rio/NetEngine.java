package cj.studio.ecm.net.rio;

import java.io.IOException;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.graph.Access;
import cj.studio.ecm.graph.AnnotationProtocolFactory;
import cj.studio.ecm.graph.Graph;
import cj.studio.ecm.graph.GraphCreator;
import cj.studio.ecm.graph.ICablePin;
import cj.studio.ecm.graph.ICablePinEvent;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IProtocolFactory;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.graph.IWirePin;
import cj.studio.ecm.graph.SinkCreateBy;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.net.IClient;
import cj.studio.ecm.net.IConnectCallback;
import cj.studio.ecm.net.IServer;
import cj.studio.ecm.net.graph.INetGraph;
import cj.studio.ecm.net.nio.NetConstans;
import cj.ultimate.util.StringUtil;

public class NetEngine implements INetEngine {
	private ExecutorService pumpPool;
	/*
	 * 服务器端等待客户端发送确认侦，产生等待超时的原因，与工作线程池数有关，即它是服务器负荷过大导致，原因如下：
	 * 1.假设用于读取的工作线程t1在等待确认侦，超时时间是3600毫秒 2.客户端已发来确认侦，因此启动了新的读取工作线程t2
	 * 3.t2将确认侦放入服务器的发送队列q，并通知t1确认侦已达 4.此时t1从q中尝试取出确认侦。
	 * 5.以上流程中，如果在第2步，在申请t2时，工作线程池满了，则处于线程池外面等待
	 * 6.如果此时t1中的等待超时退出了t1，则获取不到确认侦而t1就返回了 7.t1退出后，假设此时申请到了工作线程t2，t2将执行步走3之后的动作
	 * 8.而此时t1早已退出，故收不到确认侦，因为t1早已报超时了
	 * 
	 * 解决办法： 1.尽量多的设定工作线程池数，尽量长的超时间（超时时间超长越占服务器资源）
	 * 2.建立重传机制,即，如果超时则要求重传，这种方式浪费网络流量，因为此后的确认侦还是收到了。
	 * 3.无限等待，如果确认侦总是发不来，这种情况几个等待任务就把线程池用完了，故不可取。
	 * 4.建立确认侦收取池（持久化到内存或硬盘），在超时后如需要重传时（如果有重传机制）则先尝试在确认侦池中查找，如果没有再重传
	 * 这种实现可支持将超时尽量设得很短。 但也有些问题，因为重传不能总是占着线程t1，否则新的线程无法申请到，
	 * 因此可用主线程收到确认侦先放入确认侦收取池，而后再进行2-8的步走，这样确认侦可以不用启工作线程，而由主线程通知即可。
	 * 5.如果不解决，则在压力高峰，服务器端在此种情况下的超时无法避免
	 * 
	 * 结论：可采用方案4,即：确认侦不申请新的读取工作线程，而是由主线程通知t1,因此确认侦要有识别它为确认侦的机制，即：
	 * 有侦id且侦id含有确认格式：服务器为（c:id),客户端为(s:id)，由于以上原因，因此也应将心跳侦(b:id)交由主线程维持（
	 * 避免启用工作线程） 因此，确认侦将不下发到工作线程，即不送达readwork。
	 * 
	 * 因此上，使用结论中的方案，可以基本避免超时，且不占用工作线程池数，提升了服务器资源的利用率。
	 */
	private ExecutorService worksPool;
	private INetGraph g;

	private Map<String, String> props;
	private INetGraphNamed named;
	ILogging logger;
	private ChannelNioMoniterPool channelNioMoniterPool;
	private Object owner;

	public NetEngine(INetGraphNamed named, Map<String, String> props) {
		this.named = named;
		this.props = props;
		this.g = this.createNetGraph();
		logger = CJSystem.current().environment().logging();
	}

	@Override
	public Map<String, String> getProps() {
		return props;
	}

	@Override
	public void setNetProperties(Map<String, String> props) {
		this.props = props;
	}

	protected INetGraph createNetGraph() {
		return new RIONetGraph();
	}

	public final INetGraph getNetGraph() {
		return g;
	}

	@Override
	public void stopMoniter() {
		// 客户端关闭时无事件问题，目前设了hannelNioMoniterPool.dispose方法后还是不行，客户端捕捉不到关闭事件,造成关闭后再次连接而堵塞。
		if (channelNioMoniterPool != null)
			channelNioMoniterPool.dispose();
		if (pumpPool != null)
			this.pumpPool.shutdownNow();
		if (worksPool != null)
			this.worksPool.shutdownNow();

		this.channelNioMoniterPool = null;
		if (g != null) {
			this.g.dispose();
		}
		owner = null;
	}

	@Override
	public String bossThreadCount() {
		return props.get("bossThreadCount");
	}

	@Override
	public String workThreadCount() {
		return props.get("workThreadCount");
	}

	@Override
	public Object owner() {
		return owner;
	}

	/**
	 * 如果指定的线程数小于3,则修改为8
	 * 
	 * @throws IOException
	 */
	@Override
	public void moniter(Object moniter, AbstractSelectableChannel javach,
			Map<String, String> props, IConnectCallback callback)
			throws IOException {
		if (moniter instanceof IServer) {
			buildServerMoniter(moniter, javach, props, callback);
		} else {
			buildClientMoniter(moniter, javach, props, callback);
			// pool.remove(this);//该方法带上总会少一个disconnect事件，可能有共享子监视器的情况，一个把监视器关了，后一个就不能发出事件了，一个监视器上可注册多个信道，按说在客户端是想让它一对一的，看来还是有共享，不过没关系，监视池在导线束为空时把客户端关闭就行了，每个导线移除时暂不关监视器（除非某天测到多个物理信道有共享监视器这种情况）
			ICablePin in = (ICablePin) g.netInput();
			in.onEvent(new ICablePinEvent() {

				@Override
				public void onNewWired(ICablePin owner, String name,
						IWirePin wire) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onDestoryWired(ICablePin owner, String name,
						IWirePin wire) {
					if (in.wireCount() < 1) {
						IClient client = (IClient) moniter;
						client.close();
					}
				}

				@Override
				public void onDestoringWired(ICablePin owner, String name,
						IWirePin wire) {
					// TODO Auto-generated method stub

				}
			});
		}
		owner = moniter;
	}

	private void buildClientMoniter(Object moniter,
			AbstractSelectableChannel javach, Map<String, String> props2,
			IConnectCallback callback) throws IOException {

		props.put("bossThreadCount", "1");// 主线程只有一个
		String workThreadCount = props.get("workThreadCount");
		if (StringUtil.isEmpty(workThreadCount)
				|| "0".equals(workThreadCount)) {
			// int cpuCount = Runtime.getRuntime().availableProcessors();
			workThreadCount = "1"/*String.valueOf(cpuCount)*/;
			props.put("workThreadCount", workThreadCount);
		}
		int workPoolSizeV = Integer.valueOf(workThreadCount);

		HandlerThreadFactory factory = new HandlerThreadFactory();
		pumpPool = Executors.newSingleThreadExecutor(factory);
		worksPool = Executors.newFixedThreadPool(workPoolSizeV, factory);

		if (!g.isInit()) {
			g.initGraph();
		}
		if (callback != null) {
			callback.buildGraph(moniter, g);
		}

		// 本实现实现连接池的功能，一个线程池就等同于一个连接池
		channelNioMoniterPool = new ChannelNioMoniterPool(this, worksPool,
				workPoolSizeV, false);
		ConnnectMoniterResult mresult = new ConnnectMoniterResult();
		INioMoniter m = createConnectNioMoniter(javach/*主连接*/, mresult);
		pumpPool.submit(m);// 线程池不会立即全部提交到，而且也无必要使用主线程池

		try {// 仅用于等待主连接完成，只要主连接完成了，便视为客户端建立成功，子连接完不完成不关我吊事((已改为等待所有连接完成）
			mresult.waitFinished(NetConstans.waitClientConnectTimeout);
			if (mresult.state() != -1 && mresult.state() != 200) {
				throw new EcmException(String.format("%s %s", mresult.state(),
						mresult.message()));
			}
		} catch (Exception e) {
			throw new EcmException(e);
		}
	}

	@Override
	public long heartbeatInterval() {
		String nheartbeatInterval = props.get("heartbeatInterval");
		if (StringUtil.isEmpty(nheartbeatInterval)) {
			nheartbeatInterval = "-1";// 默认不开启
			props.put("heartbeatInterval", nheartbeatInterval);
		}
		return Long.valueOf(nheartbeatInterval);
	}

	private void buildServerMoniter(Object moniter,
			AbstractSelectableChannel javach, Map<String, String> props2,
			IConnectCallback callback) throws IOException {
		String bossThreadCount = props.get("bossThreadCount");
		if (StringUtil.isEmpty(bossThreadCount)
				|| "0".equals(bossThreadCount)) {
			bossThreadCount = "1";
			props.put("bossThreadCount", bossThreadCount);
		}
		int bossThreadsV = Integer.valueOf(bossThreadCount);

		String workThreadCount = props.get("workThreadCount");
		if (StringUtil.isEmpty(workThreadCount)
				|| "0".equals(workThreadCount)) {
			int cpuCount = Runtime.getRuntime().availableProcessors();
			workThreadCount = String.valueOf(cpuCount);
			props.put("workThreadCount", workThreadCount);
		}
		int workPoolSizeV = Integer.valueOf(workThreadCount);
		if(workPoolSizeV<0){
			props.put("workThreadCount", "-1");
			workPoolSizeV=-1;
		}
		HandlerThreadFactory factory = new HandlerThreadFactory();
		pumpPool = Executors.newFixedThreadPool(bossThreadsV, factory);
		if(workPoolSizeV<0){
			worksPool = Executors.newCachedThreadPool(factory);
		}else{
			worksPool = Executors.newFixedThreadPool(workPoolSizeV, factory);
		}

		if (!g.isInit()) {
			g.initGraph();
		}
		if (callback != null) {
			callback.buildGraph(moniter, g);
		}

		channelNioMoniterPool = new ChannelNioMoniterPool(this, worksPool,
				workPoolSizeV, true);

		for (int i = 0; i < bossThreadsV; i++) {
			INioMoniter m = createAcceptNioMoniter(javach);
			pumpPool.submit(m);
		}

	}

	protected INioMoniter createConnectNioMoniter(
			AbstractSelectableChannel javach, ConnnectMoniterResult mresult)
			throws IOException {
		// 客户端连接主监视器与子监视器的javachannel是同一实例，虽然如此，为了追求架构统一性，仍然：
		// 支持主线程为n个，每个主线程为调用一次connect方法产生，其子线程与主线程对应其子监视窗也是1个（多个的话接收顺序肯定乱序，因此系统固化为1对1)
		INioMoniter m = new ConnectNioMoniter(this, javach, mresult);
		return m;
	}

	protected INioMoniter createAcceptNioMoniter(
			AbstractSelectableChannel javach) throws IOException {
		// 由于连接的接受不分先后，可以支持多个acceptor线程来分配
		INioMoniter m = new AcceptNioMoniter(this, javach);
		return m;
	}

	@Override
	public String getSimple() {
		return props.get("protocol");
	}

	@Override
	public ChannelNioMoniterPool getChannelNioMoniterPool() {
		return channelNioMoniterPool;
	}

	class RIONetGraph extends Graph implements INetGraph {

		@Override
		public Object options(String key) {
			if (super.containsOptions(key))
				return super.options(key);
			return props != null ? props.get(key) : null;
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
			// TODO Auto-generated method stub
			return new NetCreator();
		}

		@Override
		protected void dispose(boolean isdeep) {
			super.dispose(isdeep);
		}

		@Override
		public String name() {
			return named.getName();
		}

		@Override
		protected void build(GraphCreator creator) {
			ICablePin in = creator.newCablePin("input", Access.input);
			in.plugLast("sync-check", new SinkCreateBy(creator));
			ICablePin out = creator.newCablePin("output", Access.output);
			out.plugLast("read-sink", new SinkCreateBy(creator));

		}

	}

	class NetCreator extends GraphCreator {
		@Override
		protected IProtocolFactory newProtocol() {
			// TODO Auto-generated method stub
			return AnnotationProtocolFactory.factory(NetConstans.class);
		}

		@Override
		protected ISink createSink(String sink) {
			switch (sink) {
			case "sync-check":
				return new SyncCheckSink();
			case "read-sink":
				return new ReadSink();
			}
			return null;
		}

	}

}
