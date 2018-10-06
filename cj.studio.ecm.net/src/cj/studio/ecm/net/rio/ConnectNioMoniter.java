package cj.studio.ecm.net.rio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Iterator;
import java.util.Set;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.frame.FramePacketDecoder;
import cj.studio.ecm.frame.IFrameDecoderCallback;
import cj.studio.ecm.graph.ICablePin;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.net.graph.INetGraph;
import cj.studio.ecm.net.nio.NetConstans;
import cj.ultimate.util.StringUtil;

/**
 * 客户端连接监视器
 * 
 * <pre>
 * 好吧，长遍大论下nio，首先jvm的nio实现 is bultshit，太多坑，诸如cpu 100%,register拥堵selector等，还有libbarchart-udt这玩意，在netty下是100%cpu，这也是我为什么要自行做一套rio的原因。
 * 以上的坑，让我耗费了一个星期的没日没夜调试，不停的变着戏法，我操，谁要用我这套产品，不骂他两句我操，你就给我操。
 * 
 * 客户端与服务器端的nio结构不同，因为客户端物理上只有一个信道，而服务器端是n个，但是架构上又想趋于一致，所以服务器端有主线程、工作线程，那客户端也想有，但干啥用？后来我终于有了一个idea
 * 问题是：
 * 客户端也可以实现主线程和工作线程，只是如果一个物理连接却用多个工作线程来监视nio，虽然也可以（nio的select机制，虽然每个监视器都能得到selectKeys，但只有一个能读出数据，其它读数为0
 * 据此原理实现一个物理注册到多个select没问题，关键是消息的顺序！虽然收取时是按nio的顺序，但多个selector的监视线程向外输出的过程中，顺序一定是乱了[已经过实现并测试过］，因为是多线程向外输出，所以如果要求
 * 调用者不讲究顺序，那很没意思，虽然也有这种需求，但这种需求太没劲），因此我想到了连接池，这个用途大。
 * 
 * 连接池，像jdbc连接池，对于程序员见到的就是一个连接器，而却管理一组连接，使用和维护都十分方便，这很有必要。所以：
 * 在一个客户端内：
 * 〜规定有且仅有一个主线程，它用于监听连接池的每个连接的完成事件
 * 〜规定工作线程池数－1+1个主线程数＝连接池数=工作线程数
 * 〜规定一个物理连接（javachannel）只注册到一个信道监视器(用于监视一个selector的读写事件)，可以任意选择一个监视器，但一个物理连接不能注册到多个监视器上，因此能够保证每个连接的接收顺序是正确的。
 * 〜netGraph的输入与输入端子是电览线类型，其内每个导线即对应一个连接，导线名为连接标识。
 * 
 * 现象：连接事件，读写事件按电缆线输入输出，注意电缆线及其导线的用法
 * </pre>
 * 
 * @author carocean
 *
 */
public class ConnectNioMoniter implements INioMoniter {
	private ILogging log;
	private Selector selector;
	private INetEngine engine;
	private ChannelNioMoniterPool pool;
	private IMoniterResult mresult;
	private boolean isRunning;

	public ConnectNioMoniter(INetEngine engine,
			AbstractSelectableChannel javach, IMoniterResult mresult)
			throws IOException {
		this.selector = Selector.open();
		javach.register(selector, SelectionKey.OP_CONNECT);
		this.engine = engine;
		pool = engine.getChannelNioMoniterPool();
		log = CJSystem.current().environment().logging();
		this.mresult = mresult;
	}

	@Override
	public Selector getSelector() {
		return selector;
	}

	@Override
	public boolean isRunning() {
		// TODO Auto-generated method stub
		return isRunning;
	}
	@Override
	public void stop() {
		// TODO Auto-generated method stub
		isRunning=false;
	}
	@Override
	public Object call() throws Exception {
		isRunning = true;
		int usedchild = 0;
		while (isRunning&&!Thread.currentThread().isInterrupted()) {
			selector.select();
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> keyIterator = keys.iterator();
			while (keyIterator.hasNext()) {
				SelectionKey key = keyIterator.next();
				keyIterator.remove();
				if (!key.isValid()) {
					continue;
				}

				if (key.isConnectable()) {

					SocketChannel ch = (SocketChannel) key.channel();
					try {
						while (!ch.finishConnect()) {
						}

						String wireName = Integer.toHexString(ch.hashCode());
						FramePacketDecoder decoder = new FramePacketDecoder();
						ByteBuffer buf = ByteBuffer
								.allocate(NetConstans.CHANNEL_READ_BUFSIZE);
						IChannelWriter writer = new ChannelWriter(ch);
						IHeartbeat heartbeat = new Heartbeat(writer, false);
						SendQueue sq = createSendQueue(ch, wireName);
						IChannelContext ctx = new ChildChannelContext(decoder,
								buf, sq, heartbeat, writer, wireName);// 给它一个子监视窗，以供往里放worker，子监视窗的运行由内核来控制
						heartbeat.setChannel(ch);
						/*
						 * 注意：由于主、子监视器共享同一个java channel实例，所以会引发争用，导致：
						 * － 在主监视器空循环，cpu100%，实际上，在客户端的实现中主监视器与子监视器合并成一个是最好的，但为了架构统一，还是分开了
						 * 避免空循环的方法是，一个信道不同时注册到主、子上，而是：
						 * 在主上得到连接后，即将select关闭，并启用一个新的selector，由此避免了同一信道的争用问题。
						 * 
						 * 另外的解决办法是：return null，当成功连接，即退出循环，而此种方法在处理连接池时（主监视器用来建立多个连接），第一个连接之后的连接申请便不被
						 * 接受，必须在netengine中解决这个问题，此方案会造成架构的不统一，因此不必考虑。
						 * 
						 * 在所有子监视器（每个子连接）建立完成后，最好将主监视器退出，因为像动态建立多个连接的功能并实现不想支持，一次性建完了，主监视器已完成使命，但：
						 * 为了将来支持这种像jdbc池一样动态扩展连接数的功能而保留
						 */
						selector.close();// 千万不能与子争用，否则会导致前端第一次登录时悬停

						try {
							workConnect(key, ch, ctx);
							ctx.register(key, ch, pool);
							// ChildChannelContext ccc = (ChildChannelContext)
							// ctx;//放到子连接完成通知，这样改过第一个连接完成通知模式2016/7/30
							//用于通知主连接完成
							// ccc.connectResult().notifyFinished();//
							// 完成连接：连接池建立第一个连接即视为客户端连接成功(已改为等待所有连接完成）
						} catch (Exception e) {
							log.error(getClass(), e);
							try {
								key.cancel();
							} catch (Exception e2) {
							}
						}
						/*
						 * 其下是建立子连接(连接池)
						 */
						// workPoolSizeV-1的原因是客户端已连接了一个主连接，连接数＝1个主连接+（workPoolSizeV－1)个辅连接即等于工作线程池数
						int count = pool.workPoolSize() - 1;
						if (usedchild == count) {// 建立子连接完成
							mresult.notifyFinished();// 所有连接完成时通知
							return null;
						}
						
						selector = Selector.open();

						SocketChannel sc = SocketChannel.open();
						sc.configureBlocking(false);
						String host = engine.getProps().get("host");
						int port = Integer
								.valueOf(engine.getProps().get("port"));
						sc.register(selector, SelectionKey.OP_CONNECT);
						sc.connect(new InetSocketAddress(host, port));

						usedchild++;
					} catch (Exception e) {
						key.cancel();
						// if (selector != null) {
						// selector.selectNow();// removes the key from this
						// // selector
						// }
						log.error(getClass(), e);
						mresult.state(NetConstans.STATUS_691);
						mresult.message(e.getMessage());
						mresult.notifyFinished();
						throw new EcmException(e);
					}
				}
			}
		}
		isRunning = false;
		return null;
	}

	public void workConnect(SelectionKey key, SocketChannel ch,
			IChannelContext ctx) throws Exception {

		IWorker worker = null;
		worker = new ConnectWork();
		Frame f = new Frame("connect / net/1.1");
		Circuit c = new Circuit("net/1.1 200 ok");

		String simple = engine.getSimple();
		INetGraph g = engine.getNetGraph();
		worker.init(f, c, ch, (ICablePin) g.netInput(),
				(ICablePin) g.netOutput(), g.name(), simple);

		String wireName = Integer.toHexString(ch.hashCode());
		ICablePin out = (ICablePin) g.netOutput();
		ICablePin in = (ICablePin) g.netInput();
		out.newWire(wireName);
		// out.options("socket-channel", ch);// 由于客户端只有一个信道，故放入默认选项。
		out.options("cable-net", "client");
		in.newWire(wireName);
		// in.options("socket-channel", ch);
		in.options("cable-net", "client");

		in.wireOptions(wireName, "channel-context", ctx);
		out.wireOptions(wireName, "channel-context", ctx);

		worker.call();

		ctx.startHearbeat(new IHeartbeatEvent() {

			@Override
			public void channelInvalid(SocketChannel ch) throws Exception {
				ch.close();
			}
		});
	}

	private SendQueue createSendQueue(SocketChannel ch, String channelId) {
		String sendQueueSize = engine.getProps().get("sendQueueSize");
		if (StringUtil.isEmpty(sendQueueSize)) {
			sendQueueSize = NetConstans.RIO_NET_SENDQUEUESIZE;// "4096";
		}
		int sendQueueSizeInt = Integer.valueOf(sendQueueSize);
		String circuitSyncTimeout = engine.getProps().get("circuitSyncTimeout");
		if (StringUtil.isEmpty(circuitSyncTimeout)) {
			circuitSyncTimeout = "3600";
		}
		long circuitSyncTimeoutInt = Long.valueOf(circuitSyncTimeout);
		if (circuitSyncTimeoutInt < 500) {
			circuitSyncTimeoutInt = 3600;
		}
		SendQueue sq = new SendQueue(sendQueueSizeInt, circuitSyncTimeoutInt);

		// INetGraph g=engine.getNetGraph();
		// ((ICablePin)g.netInput()).wireOptions(channelId,"send-queue", sq);
		// ((ICablePin)g.netOutput()).wireOptions(channelId,"send-queue", sq);

		return sq;
	}

	class ChildChannelContext
			implements IChannelContext, IFrameDecoderCallback {
		FramePacketDecoder decoder;
		ByteBuffer buf;
		private IHeartbeat heartbeat;
		private SendQueue sq;
		private String channelId;
		private SocketChannel channel;
		private IChannelWriter writer;

		public ChildChannelContext(FramePacketDecoder decoder, ByteBuffer buf,
				SendQueue sq, IHeartbeat heartbeat, IChannelWriter writer,
				String channelId) {
			this.decoder = decoder;
			this.buf = buf;
			this.heartbeat = heartbeat;
			this.writer = writer;
			this.sq = sq;
			this.channelId = channelId;
			decoder.setDecoder(this);
		}

		@Override
		public IChannelWriter getWriter() {
			return writer;
		}

		@Override
		public void startHearbeat(IHeartbeatEvent e) {

			long d = engine.heartbeatInterval();
			if (heartbeat != null && d > 0) {
				heartbeat.event(e);
				heartbeat.moniter(d);
			}
		}

		@Override
		public void dispose() {
			if (heartbeat != null) {
				heartbeat.dispose();
			}
			heartbeat = null;
			if (writer != null) {
				writer.dispose();
			}
			this.sq.dispose();
			sq = null;
			mresult = null;
			buf.clear();
			decoder.dispose();

		}

		public IMoniterResult connectResult() {
			return mresult;
		}

		/*
		 * 没办法，得长篇大论下，nio坑太多坑太深，动不动就堵，动不动就死循环，经不段的测试和改进，总结出两种实现方案
		 * 所有的方案都是为了避免在ch.register方法与selector.select()方法不在同一线程下，register会堵塞的问题。
		 * 
		 * 第一种：采用注册任务队列机制
		 * 	在主监视器中只唤醒，不进对物理信道进行物理注册，即不这样：ch.register(child.getSelector(), SelectionKey.OP_READ, this);
		 * 	采用在子监视器中进行注册，使得ch.register与selector.select方法可在同一线程下执行，便不会堵塞
		 * 	而是向子监视器的注册队列放入注册任务，在子监视器的循环中，发现队列中有任务则执行真正的注册，这种方案的优点是：
		 * 	1.架构上比较优美，主子监视器分的特别清晰，连接的建立处理也交由了子监视器完成，但缺点是：
		 * 	2.由于真正的注册在子监视线程中完成，如果子监视器中正在处理较耗时的用户任务，则必然堵住后续的连接申请(同一子监视器可以监视多个信道)，因此在及时性断和开比较频烦的链路中，比如
		 * 		http的neuron，浏览器请求一个页面会同时建立多个资源连接，请求完就断开，下次又有一批连上，因此这种子监视器也处理连接的方案是不适合生产环境要求的。
		 * 	注意事项：在使用第一种方案时，由于唤醒与注册队列真正的执行有一段矩离，如果在监视到连接关闭时read()==0，使用了key.cancel之后紧跟着又进行了selector.selectNow()，
		 * 			则selectNow方法便把先前的唤醒时的keys给吃掉了，因此造成循环等待，而注册队列虽有任务却不能被执行，客户端表现为：建立连接时长时等待。
		 * 			因此，在使用方案一时一定要注销掉key.cancel后面的selector.selectNow()，selectNow方法跟在cancel后的作用仅仅是让cancel立即生效，实际上等到下次循环也一样生效。
		 * 第二种：采用同步机制
		 * 	在子监视器的循环中设一个空的同步块，在主监视器的注册方法中该子监视器实例作为同步锁
		 * 	在主监视器中执行物理注册，那么：
		 * 	1.主监视器执行连接方法
		 * 	2.子监视器执行读、写、断开连接
		 * 	同步机制可以及时的接收连接，是一种较好的方案。
		 */
		@Override
		public void register(SelectionKey key, SocketChannel ch,
				ChannelNioMoniterPool pool) throws IOException {
			pool.clientChannels.add(ch);
			// 将这一个信道一次性注册到所有moniter上，根据推测，nio使得各个moniter都收到事件，但一个取出数据，其它的就取不到。
			// 在信道监视器中ch.read()==0//这个条件实现了一个信道向多个监视器注册而不出现接收乱序的的原理，即：
			// 虽然一次读取事件各监视线程都有select到(都得到keys)，但一个读出数据其它皆空，其它的都只能读到zero字节,因此:
			// 收取时虽是正确顺序，但多线程往后面输出的过程中可能造成乱序，因此本实现不采用这种方案。
			// 本实现实现一种类似于连接池的功能，一个工作线程就对应一个连接，因此一个信道仍向一个信道临视器注册，这种用途比较多。
			this.channel = ch;
			INioMoniter child = pool.get(ch.hashCode());
			INioRegisterWakeup wakeup = (INioRegisterWakeup) child;
			key.attach(this);
			// 方案一开始

			// try {
			// wakeup.offerRegisterTask(key);
			// } catch (Exception e) {
			// log.error(getClass(), e);
			// return;
			// }
			// wakeup.wakeup();
			// 方案一结束
			// 方案二开始
			synchronized (child) {
				wakeup.wakeup();
				wakeup.offerRegisterTask(key);
				// ch.register(child.getSelector(), SelectionKey.OP_READ, this);
				// child.notifyAll();//
				// 必须：否则子监视器的“已退出的信道”会堵住子监视器，导致后续的新的信道无法读写，每注册完得通知子监视器的所有线程干活
				// // jvm 的 nio就是 bultshit
			}
		}

		@Override
		public SocketChannel getChannel() {
			return channel;
		}

		@Override
		public String getChannelId() {
			return channelId;
		}

		@Override
		public SendQueue getSq() {
			return sq;
		}

		public ByteBuffer getReadBuf() {
			return buf;
		}

		public FramePacketDecoder getDecoder() {
			return decoder;
		}

		@Override
		public Frame onNewFrame(Frame frame, Object... attachArgs) {
			try {
				if (ackFrame(frame, attachArgs)) {
					return frame;// 确认侦不下发。即读取线程与确认侦无关。
				}
			} catch (Exception e) {
				log.error(getClass(), e);
				return frame;
			}
			// System.out.println("found new frame...");
			SelectionKey key = (SelectionKey) attachArgs[0];
			SocketChannel ch = (SocketChannel) key.channel();
			heartbeat.visitChannel(ch);

			INetGraph g = engine.getNetGraph();
			IWorker worker = null;
			worker = new ClientReadWork();
			Circuit c = new Circuit(
					String.format("%s 200 ok", frame.protocol()));
			String simple = engine.getSimple();
			worker.init(frame, c, ch, (ICablePin) g.netInput(),
					(ICablePin) g.netOutput(), g.name(), simple);
			try {
				worker.call();
			} catch (Exception e) {
				// log.error(getClass(), e);
			}
			return frame;
		}

		// 什么是确认侦：即请求方在等待远程处理完后回发过来的侦，即是确认侦，那么该方法处理确认侦收到后如何通知等待方的问题。
		protected boolean ackFrame(Frame frame, Object... attachArgs)
				throws Exception {
			if (!frame.containsHead(NetConstans.FRAME_HEADKEY_FRAME_ID))
				return false;
			String frameId = frame.head(NetConstans.FRAME_HEADKEY_FRAME_ID);
			if (StringUtil.isEmpty(frameId))
				return false;
			if (!frameId.contains(":"))
				return false;
			if (frameId.charAt(1) != ':' && frameId.charAt(2) != ':') {// 确认类型在消息标识中最多占两个字符位。所以要么第0位要么第0,1位
				return false;
			}
			String arr[] = frameId.split(":");
			frameId = arr[1];
			String ackType = arr[0];// 确认类型

			SelectionKey key = (SelectionKey) attachArgs[0];
			INetGraph g = engine.getNetGraph();
			IChannelContext ctx = (IChannelContext) key.attachment();
			SendQueue sq = ctx.getSq();
			// s来自客户端确认
			IWorker worker = null;
			switch (ackType) {
			case "c":
			case "s":
				if (!sq.notifya(frameId, frame)) {// 如果没有成功通知，则采用回馈异步方式。
					// 为什么不交由主线程处理，原因：如果确认过程出错，会导至服务器挂掉，因此交由消息泵申请的新线程处理较为安全。
					worker = new AckFrameWork(heartbeat, frameId, sq);
					String simple = engine.getSimple();
					worker.init(frame, null, (SocketChannel) key.channel(),
							(ICablePin) g.netInput(), (ICablePin) g.netOutput(),
							g.name(), simple);
					worker.call();
				}
				break;
			case "cb":// 服务器收到的：心跳确认侦,sb是心跳发送客户端的包，发送不在此处。心跳包
				// 心跳线程交由主线程处理，因为处理时间很短
				worker = heartbeat.getAckHeartbeatCBFrameWork();
				String simple = engine.getSimple();
				worker.init(frame, null, (SocketChannel) key.channel(),
						(ICablePin) g.netInput(), (ICablePin) g.netOutput(),
						g.name(), simple);
				worker.call();// 还是有专属的确认线程池最安全。
				break;
			case "sb":
				frame.parameter("frameId", frameId);
				worker = heartbeat.getAckHeartbeatSBFrameWork();
				simple = engine.getSimple();
				worker.init(frame, null, (SocketChannel) key.channel(),
						(ICablePin) g.netInput(), (ICablePin) g.netOutput(),
						g.name(), simple);
				worker.call();// 还是有专属的确认线程池最安全。
				break;
			default:
				CJSystem.current().environment().logging().error(
						AckFrameWork.class, "侦标识格式错误,不支持此确认类型：" + ackType);
			}

			return true;
		}
	}

}
