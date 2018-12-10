package cj.studio.ecm.net.rio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Iterator;
import java.util.Set;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.frame.FramePacketDecoder;
import cj.studio.ecm.frame.IFrameDecoderCallback;
import cj.studio.ecm.graph.ICablePin;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.net.graph.INetGraph;
import cj.studio.ecm.net.nio.NetConstans;
import cj.ultimate.util.StringUtil;

public class AcceptNioMoniter implements INioMoniter {
	private ILogging log;
	private Selector selector;
	private INetEngine engine;
	private ChannelNioMoniterPool pool;
	private boolean isRunning;

	public AcceptNioMoniter(INetEngine engine, AbstractSelectableChannel javach)
			throws IOException {
		this.selector = Selector.open();
		javach.register(selector, SelectionKey.OP_ACCEPT);
		this.engine = engine;
		pool = engine.getChannelNioMoniterPool();
		log = CJSystem.current().environment().logging();
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		isRunning = false;
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	/*
	 * 
	 * 当有数据在写时，将数据写到缓冲区中，并注册写事件。
	public void write(byte[] data) throws IOException {  
	    writeBuffer.put(data);  
	    key.interestOps(SelectionKey.OP_WRITE);  
	}  
	
	注册写事件后，写操作就绪，这时将之前写入缓冲区的数据写入通道，并取消注册。
	channel.write(writeBuffer);  
	key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
	 * (non-Javadoc)
	 * @see cj.studio.ecm.net.rio.IMoniter#call()
	 */
	/**
	 * 一定要保障各个监视器在工作之前其call方法被提交，否则接收到请求向信道监视池注册却因为没有启动而不能建立起连接 rio采用起动时即初始化好监视池
	 */
	@Override
	public Object call() throws Exception {
		isRunning = true;
		// 主线程读取信息或接受连接，而后将信息或事件分发给worker线程。
		while (isRunning && !Thread.currentThread().isInterrupted()) {
			selector.select();
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> keyIterator = keys.iterator();
			while (keyIterator.hasNext()) {
				SelectionKey key = (SelectionKey) keyIterator.next();
				keyIterator.remove();
				if (!key.isValid()) {
					continue;
				}
				// 由于连接的建立又不分先后次序，所以可以向多个selector注册，以实现多路acceptor
				if (key.isAcceptable()) {
					try {
						ServerSocketChannel ssc = (ServerSocketChannel) key
								.channel();
						SocketChannel ch = ssc.accept();
						if (ch == null) {// 这种情况是：主线程大于1,被其它accept监视器接收了
							continue;
						}
						// workAccept(key, ch);
						String wireName = Integer.toHexString(ch.hashCode());
						SendQueue sq = createSendQueue(ch, wireName);
						// 在处理完accept的连接之后再将io信道注册为可读，并为之生成信道上下文放入javach的附件
						// ChildNioMoniter数量最多与工作线程数量相同，因为工作线程与监视器一比一，所以子临视器集合在engine中维护，在所有父监视器中共享
						FramePacketDecoder decoder = new FramePacketDecoder();
						ByteBuffer buf = ByteBuffer
								.allocate(NetConstans.CHANNEL_READ_BUFSIZE);
						IChannelWriter writer = new ChannelWriter(ch);
						IHeartbeat heartbeat = new Heartbeat(writer, true);
						heartbeat.setChannel(ch);
						IChannelContext ctx = new ChildChannelContext(decoder,
								buf, sq, heartbeat, writer, wireName);// 给它一个子监视窗，以供往里放worker，子监视窗的运行由内核来控制

						try {
							workAccept(key, ch, ctx);
							ctx.register(key, ch, pool);
						} catch (Exception e) {
							log.error(getClass(), e);
							try {
								key.cancel();
								// if (selector != null) {
								// selector.selectNow();// removes the key from
								// // this selector
								// }
							} catch (Exception e2) {
							}
						}

					} catch (Exception e) {// 不使工作的异常跳出主线程
						/*
						 * 是这样的，如果发现断网（物理链路断了）那么key是失效了，下次不能再轮询该无效的key，但是你不能关掉信道，否则
						 * 客户端的连接还在，而服务器关了，则会使客户端不可再用。
						 * 但在经神元中客户端发现写的io异常，会主动关掉客户端而再发起新的连接，但这样便丢包了，因此：
						 * 在net机制下，如果发现网络异常，仍只是使key无效即可。
						 * 实际上不显式取消key也可以，照常也可使用，因此此处仍需观擦
						 */
						try {
							key.cancel();
							// if (selector != null) {
							// selector.selectNow();// removes the key from
							// // this selector
							// }
						} catch (Exception e2) {
						}
						log.error(getClass(), String.format(
								"server accept error:%s", e.getMessage()));
					}
				} else {
					System.out.println("else....");
				}
			}

		}
		isRunning = false;
		return null;
	}

	public void workAccept(SelectionKey key, SocketChannel ch,
			IChannelContext ctx) throws Exception {
		IWorker worker = new AcceptWork();
		Frame f = new Frame("connect / net/1.1");
		Circuit c = new Circuit("net/1.1 200 ok");
		String simple = engine.getSimple();
		INetGraph g = engine.getNetGraph();
		worker.init(f, c, ch, (ICablePin) g.netInput(),
				(ICablePin) g.netOutput(), g.name(), simple);

		String wireName = ctx.getChannelId();
		ICablePin out = (ICablePin) g.netOutput();
		ICablePin in = (ICablePin) g.netInput();

		out.newWire(wireName);
		out.options("cable-net", "server");
		in.newWire(wireName);
		in.options("cable-net", "server");
		in.wireOptions(wireName, "channel-context", ctx);
		out.wireOptions(wireName, "channel-context", ctx);

		// 先向应用通知连接
		// 而后建立子监视器的侦听
		worker.call();

		ctx.startHearbeat(new IHeartbeatEvent() {

			@Override
			public void channelInvalid(SocketChannel ch) throws Exception {
				ch.close();// 监视器立即会监听到关闭事件
			}
		});
	}

	@Override
	public Selector getSelector() {
		return selector;
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

		// INetGraph g = engine.getNetGraph();
		// ICablePin in = ((ICablePin) g.netInput());
		// in.wireOptions(channelId, "send-queue", sq);
		// ((ICablePin) g.netOutput()).wireOptions(channelId, "send-queue", sq);

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
		public SendQueue getSq() {
			return sq;
		}

		@Override
		public void dispose() {
			if (heartbeat != null) {
				heartbeat.dispose();
			}
			heartbeat = null;
			buf.clear();
			decoder.dispose();
			sq.dispose();
			writer.dispose();

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
			this.channel = ch;
			pool.clientChannels.add(ch);
			INioMoniter child = pool.get(ch.hashCode());
			key.attach(this);
			/*
			 * 方案一开始
			 */

			// INioRegisterWakeup wakeup = (INioRegisterWakeup) child;
			// try {
			// wakeup.offerRegisterTask(key);
			// wakeup.wakeup();
			// // child.getSelector().wakeup();
			// } catch (Exception e) {
			// log.error(getClass(), e);
			// return;
			// }
			/*
			 *方案一结束 
			 */
			/*
			 * 方案二开始
			 */
			INioRegisterWakeup wakeup = (INioRegisterWakeup) child;
			synchronized (child) {
				wakeup.wakeup();
				wakeup.offerRegisterTask(key);
				// ch.configureBlocking(false);
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
			worker = new ServerReadWork();
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
			IChannelContext ctx = (IChannelContext) key.attachment();
			INetGraph g = engine.getNetGraph();
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
				worker.call();
				break;
			default:
				CJSystem.current().environment().logging().error(
						AckFrameWork.class, "侦标识格式错误,不支持此确认类型：" + ackType);
			}

			return true;
		}
	}
}