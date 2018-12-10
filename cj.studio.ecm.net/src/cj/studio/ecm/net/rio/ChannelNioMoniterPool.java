package cj.studio.ecm.net.rio;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import cj.studio.ecm.EcmException;
import cj.ultimate.IDisposable;

public class ChannelNioMoniterPool implements IDisposable {

	ExecutorService worksPool;
	int workPoolSize;
	List<INioMoniter> moniters;
	List<SelectableChannel> clientChannels;
	private INetEngine engine;
	private boolean isServerNet;
//	ReentrantLock wirePinReleaseLocker;

	public ChannelNioMoniterPool(INetEngine engine, ExecutorService worksPool,
			int workPoolSize, boolean isServerNet) {
		this.engine = engine;
		this.worksPool = worksPool;
		this.workPoolSize = workPoolSize;
		this.moniters = new ArrayList<>();
		clientChannels = new ArrayList<>();
//		if(!isServerNet){
//			wirePinReleaseLocker = new ReentrantLock();
//		}
		init(isServerNet);
	}

	/**
	 * 
	 * <pre>
	 *
			 * 原因：disconnect / NET/1.1事件出现问题。
			 * 多个子监视器同时监测到断开事件，因为它是多线程的，在执行该事件时并发修改了客户端的输入输出电缆的导线数，这必然导致在事件中获取的导线数不一致。
			 * 因此：
			 * 采用了pool.getWirePinReleaseLocker()锁，即在断开时同步处理。
	 *
	 * </pre>
	 * 
	 * @return
	 */
//	public ReentrantLock getWirePinReleaseLocker() {
//		return wirePinReleaseLocker;
//	}

	@Override
	public void dispose() {
		/*
		 * 对端和本端在信道关闭时未有关闭事件，所以添加以下代码，但该代码仅能发起服务器端侦听客户端信道关闭的通知，客户端主动关闭时，客户端的关闭事件没有
		 */
		SelectableChannel[] chs = clientChannels
				.toArray(new SelectableChannel[0]);
		for (SelectableChannel m : chs) {// 一般情况下只有客户端需要调用释放
			try {
				m.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		clientChannels.clear();
		INioMoniter[] marr = moniters.toArray(new INioMoniter[0]);
		for (INioMoniter m : marr) {// 一般情况下只有客户端需要调用释放
			if (m == null) {
				continue;
			}
			try {
				m.stop();
				m.getSelector().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		worksPool = null;
		moniters.clear();
		engine = null;
//		this.wirePinReleaseLocker=null;
	}

	public int workPoolSize() {
		return workPoolSize;
	}

	/**
	 * 必须在系统启动时将监控池初始化，为之分配selector并运行它，这是因为jvm nio存在太鸡巴多的缺陷： ＊ jvm
	 * nio在服务器端接收连接后要为信道注册，而注册线程与selector.select()的线程如不在同一线程下，则register堵塞，
	 * 导致客户端都连不上 ＊
	 * 如何避免阻塞，我采用了并发锁，可参数moniter的代码。而并发锁是可以无阻塞注册了，但在io监视器中读写操作无反应。经过几天的反复的搞啊搞，
	 * 终于发现一个解决方案，这就是： ＊
	 * select()所在线程仅在register之前运行还不行，一定要使select()线程完成准备好了在那等，再注册才有效，
	 * 因此最好让netengine一启动就将信道监视器池准备好，因此： ＊
	 * 该方法根据最大数，充满它。客户端虽不受上述问题的影响，但也不敢保障，因此也一样预初始化它。而后所有的信道的注册可需从监视器中取来用即可。
	 * 
	 * @param isServerNet
	 */
	protected void init(boolean isServerNet) {
		this.isServerNet=isServerNet;
		for (int i = 0; i < workPoolSize; i++) {
			try {
				INioMoniter child = null;
				if (isServerNet) {
					child = new ServerChannelNioMoniter(engine);
				} else {
					child = new ClientChannelNioMoniter(
							engine/*,connectedEvent*/);
				}
				moniters.add(child);
				saferun(child);
			} catch (IOException e) {
				throw new EcmException(e);
			}
		}
	}

	/**
	 * 获取信道监视器，需要均衡分布
	 * 
	 * <pre>
	 * 客户端：
	 * 由于客户端的每connect一次其javachannel是同一个，即在主监视器和其子监视器中其javachannel是相同的，因此：
	 * 必须保证一个javachannel不被分配给多个监视器，而一个监视器仍可分配给多个javachannel
	 * 
	 * 服务器端：
	 * 由于服务器端每次accept的信道是不同的，它在注册时可以随机选择子监视器，只要保证： ＊
	 * 一个java信道不能注册到多个子监视器即可，这是因为：每个监视器即一个线程，如果 ＊
	 * 注册到多个，该信道接收到的消息的次序就会混乱。因此：一个监视器可以接受多个信道的注册 而一个信道只能注册到一个监视器。
	 * 
	 * </pre>
	 * 
	 * @param randoom
	 * @return
	 * @throws IOException
	 */
	public INioMoniter get(int randoom) throws IOException {
		if(workPoolSize<0){
			try {//一个线程用于监听多个信道可以而且顺序不会乱，反之，一个信道使用多个线程，会导致接收乱序。
				INioMoniter child = null;
				if (isServerNet) {
					child = new ServerChannelNioMoniter(engine);
				} else {
					child = new ClientChannelNioMoniter(
							engine/*,connectedEvent*/);
				}
				moniters.add(child);
				saferun(child);
				return child;
			} catch (IOException e) {
				throw new EcmException(e);
			}
		}else{
			return moniters.get(randoom % workPoolSize);
		}
	}

	/**
	 * 安全执行，如果没有运行过则运行它，否则什么也不做
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param child
	 */
	public void saferun(INioMoniter child) {
		if (!child.isRunning()) {
			worksPool.submit(child);
		}
	}

	public void remove(INioMoniter moniter) {
		moniter.stop();
		if (this.moniters != null) {
			this.moniters.remove(moniter);
		}
		try {
			Selector s = moniter.getSelector();
			if (s != null && s.isOpen()) {
				s.close();
			}
		} catch (IOException e) {
		}
	}

}
