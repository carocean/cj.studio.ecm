package cj.studio.ecm.net.rio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetBoundException;
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

public class ServerChannelNioMoniter
		implements INioMoniter, INioRegisterWakeup {
	private ILogging log;
	private Selector selector;
	private INetEngine engine;
	// private Queue<SelectionKey> registerQueue;
	int i = 0;
	private boolean isRunning;

	public ServerChannelNioMoniter(INetEngine engine) throws IOException {
		this.selector = Selector.open();
		log = CJSystem.current().environment().logging();
		this.engine = engine;
		// this.registerQueue = new LinkedList<>();

	}
	
	@Override
	public boolean isRunning() {
		return isRunning;
	}
	@Override
	public void stop() {
		// TODO Auto-generated method stub
		isRunning=false;
	}
	@Override
	public void offerRegisterTask(SelectionKey key)
			throws IOException {
		// 方案一
		// if (!registerQueue.offer(key)) {
		// throw new EcmException("信道注册失败");
		// }
		// 方案二
		IChannelContext ctx = (IChannelContext) key.attachment();
		ctx.getChannel().configureBlocking(false);
		ctx.getChannel().register(selector, SelectionKey.OP_READ, ctx);
		this.notifyAll();// 必须：否则子监视器的“已退出的信道”会堵住子监视器，导致后续的新的信道无法读写，每注册完得通知子监视器的所有线程干活
		// jvm 的 nio就是 bultshit

	}

	@Override
	public void wakeup() {
		selector.wakeup().wakeup();
	}

	@Override
	public Object call() throws Exception {
		isRunning = true;
		// 主线程读取信息或接受连接，而后将信息或事件分发给worker线程。
		while (isRunning&&!Thread.currentThread().isInterrupted()) {
			// 同步仅用于注册信道时wakeup//http://php.mandelson.org/wp2/?p=311
			// http://stackoverflow.com/questions/12822298/nio-selector-how-to-properly-register-new-channel-while-selecting
			// 使用同步架构缺陷太大，不管怎么调时有堵住前面的连接，因此采用注册队列方式，使得register方法的调用与selector在同一线程内，这就完美了
			synchronized (this) {
			}

			// while (!registerQueue.isEmpty()) {
			// SelectionKey key = registerQueue.poll();
			// IChannelContext ctx = (IChannelContext) key.attachment();
			// SocketChannel ch = ctx.getChannel();
			// ch.configureBlocking(false);
			// ch.register(selector, SelectionKey.OP_READ, ctx);
			// try {
			// workAccept(key, ctx);
			// } catch (Exception e) {
			// log.error(getClass(), e);
			// try {
			// key.cancel();
			// if (selector != null) {
			// selector.selectNow();// removes the key from//selectNow要小心使用啊，这玩意会废了一次wakeup()，造成后续连接请求堵塞，因为在注册前的唤配操作被吃掉了
			// // this selector
			// }
			// } catch (Exception e2) {
			// }
			// }
			// }

			selector.select();

			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> keyIterator = keys.iterator();
			while (keyIterator.hasNext()) {
				SelectionKey key = (SelectionKey) keyIterator.next();
				if (!key.isValid()) {
					continue;
				}

				if (key.isReadable()) {
					// SocketChannel ch=(SocketChannel)key.channel();
					// ByteBuffer buf=ByteBuffer.allocate(1024);
					// if(ch.read(buf)<1){
					// key.cancel();
					// }
					try {
						workServerRead(key);
					} catch (IOException e) {// 不使工作的异常跳出主线程//如果将异常抛出而监视器的loop捕获不到不能释放它的话，则会导致后续的读写堵塞
						try {
							key.cancel();
//							 if (selector != null) {
//							 selector.selectNow();// removes the key from//selectNow要小心使用啊，这玩意会废了一次wakeup()，造成后续连接请求堵塞，因为在注册前的唤配操作被吃掉了
//							 selector.wakeup();
//							 }

						} catch (Exception e2) {
						}
						log.error(getClass(), String.format(
								"server read error:%s", e.getMessage()));
					}
				} else if (key.isWritable()) {
					// 缓冲写，rio暂不考虑支持
					try {
						key.interestOps(
								key.interestOps() & ~SelectionKey.OP_WRITE);
						workServerWrite(key);
					} catch (Exception e) {// 不使工作的异常跳出主线程
						if (e instanceof IOException) {
							try {
								key.cancel();
								// if (selector != null) {
								// selector.selectNow();// removes the key from
								// // this selector
								// }
							} catch (Exception e2) {
							}
						}
						log.error(getClass(), String.format(
								"server write error:%s", e.getMessage()));
					}
				}
				try {
					keyIterator.remove();
				} catch (Exception e) {

				}
			}
		}
		isRunning = false;
		return null;
	}

	private void workServerWrite(SelectionKey key) {
		throw new EcmException("rio不支持write事件。该事件会导致linux上cpu占满");
	}

	private void workServerDisconnect(SelectionKey key) throws Exception {
		IWorker worker = null;
		worker = new ServerDisconnectWork();
		Frame f = new Frame("disconnect / net/1.1");
		Circuit c = new Circuit("net/1.1 200 ok");
		String simple = engine.getSimple();
		INetGraph g = engine.getNetGraph();
		worker.init(f, c, (SocketChannel) key.channel(),
				(ICablePin) g.netInput(), (ICablePin) g.netOutput(), g.name(),
				simple);
		worker.call();
		IChannelContext ctx = (IChannelContext) key.attachment();
		ctx.dispose();
		key.attach(null);
		
		/*
		 * 由于服务器上其子监视器是固定的池，可以服务许多信道，因此它是永远开启的，不能像客户端那样可以把子监视器移除，服务器上的子信道永远为新旧连接服务，
		 * 一个子监视器上可以注册多个信道，达到多路复用，这怎敢remove，因此必须注释掉。
		 */
//		ChannelNioMoniterPool pool=engine.getChannelNioMoniterPool();
//		if(pool!=null){
//			pool.remove(this);
//		}
	}

	private void workServerRead(SelectionKey key) throws Exception {
		SocketChannel ch = null;
		ch = (SocketChannel) key.channel();

		IChannelContext ctx = (IChannelContext) key.attachment();
		ByteBuffer buf = ctx.getReadBuf();
		FramePacketDecoder decoder = ctx.getDecoder();
		int read = 0;
		buf.clear();
		try {
			read = ch.read(buf);
			if (read == 0) {
				return;
			}
		} catch (IOException | NotYetBoundException e) {
			read = -1;// io出错则主动设为－1以便在finally中关闭
			log.error(getClass(), String.format("server:%s", e));// 如果将异常抛出而监视器的loop捕获不到不能释放它的话，则会导致后续的读写堵塞
		} finally {
			if (read == -1) {// 远程关闭会触发该事件
				try {
					workServerDisconnect(key);
				} catch (Exception e) {
					log.error(getClass(), e);// 如果将异常抛出而监视器的loop捕获不到不能释放它的话，则会导致后续的读写堵塞
				} finally {
					// 一定要保证在遇到－1时（远端关闭）关闭信道关闭key，如果不正确的退出，则引起监视器同步方法的堵塞，导致后续的远端请求堵塞。
					try {
						key.cancel();
//						 if (selector != null) {//selectNow要小心使用啊，这玩意会废了一次wakeup()，造成后续连接请求堵塞，因为在注册前的唤配操作被吃掉了
//						 selector.selectNow();// removes the key from
//						 selector.wakeup();
//						 }
					} catch (Exception e) {
					}
				}
				return;
			}
		}

		ByteBuf bb = Unpooled.buffer();
		try {
			buf.flip();
			while (buf.hasRemaining()) {
				bb.writeBytes(buf);
			}
			buf.clear();
			decoder.write(bb, key, "s");
		} catch (Exception e) {
			log.error(getClass(), String.format("server:%s", e));
		} finally {
			bb.clear();
			buf = null;
			bb = null;
		}
	}

	@Override
	public Selector getSelector() {
		return selector;
	}

}