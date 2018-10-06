package cj.studio.ecm.net.rio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.frame.FramePacketDecoder;
import cj.studio.ecm.graph.ICablePin;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.net.graph.INetGraph;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ClientChannelNioMoniter
		implements INioMoniter, INioRegisterWakeup {
	private ILogging log;
	private Selector selector;
	private INetEngine engine;
	// private Queue<SelectionKey> registerQueue;//方案一
	boolean isRunning;
	// private AtomicBoolean connectedEvent;

	public ClientChannelNioMoniter(INetEngine engine
	/*AtomicBoolean connectedEvent*/) throws IOException {
		// this.connectedEvent = connectedEvent;
		this.selector = Selector.open();
		log = CJSystem.current().environment().logging();
		this.engine = engine;
		// registerQueue = new LinkedList<>();
	}

	@Override
	public void offerRegisterTask(SelectionKey key) throws IOException {
		// registerQueue.offer(key);//方案一

		// 方案二
		IChannelContext ctx = (IChannelContext) key.attachment();
		ctx.getChannel().configureBlocking(false);
		ctx.getChannel().register(selector, SelectionKey.OP_READ, ctx);
		this.notifyAll();// 必须：否则子监视器的“已退出的信道”会堵住子监视器，导致后续的新的信道无法读写，每注册完得通知子监视器的所有线程干活
		// jvm 的 nio就是 bultshit
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		isRunning = false;
	}

	@Override
	public void wakeup() {
		selector.wakeup();
	}

	@Override
	public boolean isRunning() {
		// TODO Auto-generated method stub
		return isRunning;
	}

	@Override
	public Object call() throws Exception {
		isRunning = true;
		while (isRunning && !Thread.currentThread().isInterrupted()) {
			// 同步仅用于注册信道时wakeup
			// http://php.mandelson.org/wp2/?p=311
			// http://blog.csdn.net/wangxi969696/article/details/7352978
			synchronized (this) {
			}
			// 方案一
			// while (!this.registerQueue.isEmpty()) {
			// SelectionKey key = registerQueue.poll();
			// IChannelContext ctx = (IChannelContext) key.attachment();
			// SocketChannel ch = ctx.getChannel();
			// ch.configureBlocking(false);
			// ch.register(selector, SelectionKey.OP_READ, key.attachment());
			// try {
			// workConnect(key, ctx);
			// if (!connectedEvent.getAndSet(true)) {
			// ChildChannelContext ccc = (ChildChannelContext) ctx;
			// ccc.connectResult().notifyFinished();// 完成连接：连接池建立第一个连接即视为客户端连接成功
			// }
			// } catch (Exception e) {
			// log.error(getClass(), e);
			// try {
			// key.cancel();
			// if (selector != null)
			// {//selectNow要小心使用啊，这玩意会废了一次wakeup()，造成后续连接请求堵塞，因为在注册前的唤配操作被吃掉了
			// selector.selectNow();// removes the key from
			// // this selector
			// }
			// } catch (Exception e2) {
			// }
			// }
			//
			// }

			selector.select();
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> keyIterator = keys.iterator();
			while (keyIterator.hasNext()) {
				SelectionKey key = keyIterator.next();
				if (!key.isValid()) {
					continue;
				}
				if (key.isReadable()) {
					try {
						workClientRead(key);
					} catch (IOException e) {// 不使工作的异常跳出主线程
						try {
							key.cancel();
							// if (selector != null)
							// {//selectNow要小心使用啊，这玩意会废了一次wakeup()，造成后续连接请求堵塞，因为在注册前的唤配操作被吃掉了
							// selector.selectNow();// removes the key from
							// // this selector
							// selector.wakeup();
							// }
						} catch (Exception e2) {
						}
						log.error(getClass(), String.format(
								"client read error:%s", e.getMessage()));
					}
				} else if (key.isWritable()) {
					key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
					try {
						throw new EcmException(
								"rio不支持write事件。该事件会导致linux上cpu占满");
					} catch (Exception e) {// 不使工作的异常跳出主线程
						try {
							key.cancel();
							// if (selector != null)
							// {//selectNow要小心使用啊，这玩意会废了一次wakeup()
							// selector.selectNow();// removes the key from
							// // this selector
							// }
						} catch (Exception e2) {
						}
						log.error(getClass(), String.format(
								"client write error:%s", e.getMessage()));
					}
				}
			}
			try {
				keyIterator.remove();
			} catch (Exception e) {

			}
		}
		isRunning = false;
		return null;
	}

	@Override
	public Selector getSelector() {
		// TODO Auto-generated method stub
		return selector;
	}

	public void workClientDisconnect(SelectionKey key) throws Exception {
		IWorker worker = null;
//		ChannelNioMoniterPool pool = engine.getChannelNioMoniterPool();
		/*
		 * 原因：disconnect / NET/1.1事件出现问题。
		 * 多个子监视器同时监测到断开事件，因为它是多线程的，在执行该事件时并发修改了客户端的输入输出电缆的导线数，这必然导致在事件中获取的导线数不一致。
		 * 因此：
		 * 采用了pool.getWirePinReleaseLocker()锁，即在断开时同步处理。
		 */
//		ReentrantLock lock=pool.getWirePinReleaseLocker();
//		try {
//			lock.lock();

			worker = new ClientDisconnectWork();
			Frame f = new Frame("disconnect / net/1.1");
			Circuit c = new Circuit("net/1.1 200 ok");
			String simple = engine.getSimple();
			INetGraph g = engine.getNetGraph();
			worker.init(f, c, (SocketChannel) key.channel(),
					(ICablePin) g.netInput(), (ICablePin) g.netOutput(),
					g.name(), simple);
			key.attach(null);

			worker.call();
		// } catch (Exception e) {
		// throw e;
		// } finally {
		// lock.unlock();
		// }

	}

	public void workClientRead(SelectionKey key) throws Exception {
		/*
		 * 注意： 1.每个信道分配一个读缓冲和一个侦解码器，这个可作为key的附加数据上?需测试当key被移除后，
		 * 下次进入的同一信道的key是否有同样的附加数据，否则要自键同信道的附加机制。
		 * 或者使用信道：ch.register(engine.selector(),
		 * SelectionKey.OP_READ,附加数据);//可在此附加数据，如为信道附加缓冲区。 使用时可直接在key的附加数据中取出
		 * 2.检查所有channel.write方法，该方法调用前必须是以下写法： buf.flip();
		 * while(buf.hasRemaining()){ channel.write(buf); }
		 * Channel.write()是在while循环中调用的。因为无法保证write()方法一次能向FileChannel写入多少字节，
		 * 因此需要重复调用write()方法，直到Buffer中已经没有尚未写入通道的字节。 可参数channel.write方法的api说明。
		 * 3.从信道中读缓冲，读之后对buf进行flip再从buf读取，以保障数据不会因错误的读取位置而失读。
		 * 
		 */
		// Object obj=key.attachment();
		// System.out.println(obj);
		SocketChannel ch = (SocketChannel) key.channel();
		IChannelContext chdata = (IChannelContext) key.attachment();
		ByteBuffer buf = chdata.getReadBuf();
		FramePacketDecoder decoder = chdata.getDecoder();
		buf.clear();
		int read = 0;
		try {
			read = ch.read(buf);
			if (read == 0) {// 此条件实现了一个信道向多个监视器注册而不出现接收乱序的的原理，即：虽然一次读取事件各监视线程都有select到，但只有一个能读到数据，其它的都只能读到zero字节,因此不会乱序
				return;
			}
		} catch (IOException e) {
			read = -1;// io出错则主动设为－1以便在finally中关闭
			log.error(getClass(), e);// 如果将异常抛出而监视器的loop捕获不到不能释放它的话，则会导致后续的读写堵塞
		} finally {
			if (read == -1) {
				try {
					workClientDisconnect(key);
				} catch (Exception e) {
					log.error(getClass(), e);// 如果将异常抛出而监视器的loop捕获不到不能释放它的话，则会导致后续的读写堵塞
				} finally {
					key.cancel();
					// if (selector != null) {//selectNow要小心使用啊，这玩意会废了一次wakeup()
					// selector.selectNow();// removes the key from
					// // this selector
					// selector.wakeup();
					// }
				}
			}
		}
		//
		ByteBuf bb = Unpooled.buffer();
		try {
			buf.flip();
			while (buf.hasRemaining()) {
				bb.writeBytes(buf);
			}
			buf.clear();
			decoder.write(bb, key, "c");
		} catch (Exception e) {
			CJSystem.current().environment().logging()
					.error(String.format("client:%s", e));
		} finally {
			bb.clear();
			buf = null;
			bb = null;
		}
	}
}
