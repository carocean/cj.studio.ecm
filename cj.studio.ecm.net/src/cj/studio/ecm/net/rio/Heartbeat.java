package cj.studio.ecm.net.rio;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.ICablePin;
import cj.studio.ecm.net.nio.NetConstans;
import cj.ultimate.IDisposable;

class Heartbeat extends TimerTask implements IHeartbeat {
	SocketChannel javach;
	long lastVisitTime;
	long timeSpliter;
	Timer heartbeatTimer;
	IHeartbeatEvent e;
	boolean isServer;
	Logger logger = Logger.getLogger(Heartbeat.class);
	private IChannelWriter writer;

	public Heartbeat(IChannelWriter writer, boolean isServer) {
		this.isServer = isServer;
		this.writer = writer;
	}

	@Override
	public void visitChannel(SocketChannel ch) {
		if (javach == null) {
			javach = ch;
			lastVisitTime = System.currentTimeMillis();
			return;
		}
		lastVisitTime = System.currentTimeMillis();
		// System.out.println("服务器ackChannel");
	}

	@Override
	public void moniter(long timeSpliter) {
		this.timeSpliter = timeSpliter;
		if (isServer && timeSpliter > 0) {// 只有服务器端才发送确认侦。
			heartbeatTimer = new Timer();
			heartbeatTimer.schedule(this, 60000, timeSpliter);
		}
	}

	public void run() {
		SocketChannel ch = javach;
		if (ch == null)
			return;
		long l = lastVisitTime;
		if ((System.currentTimeMillis() - l) > timeSpliter) {
			Frame f = new Frame("heartbeat / net/1.1");
			String frameId = String.valueOf(f.hashCode());
			f.head(NetConstans.FRAME_HEADKEY_FRAME_ID,
					String.format("sb:%s", frameId));
			// byte[] b = f.toBytes();
			// ByteBuffer buf = ByteBuffer.wrap(b);
			try {
				writer.write(f);
				// while(buf.hasRemaining())
				// ch.write(buf);// 之后要做多次确认，因为网络上可能掉包也报错。不一定都是断开连接了。
				// System.out.println("心跳器发送确认侦");
				logger.debug("心跳器已发送确认侦");
			} catch (IOException e) {
				CJSystem.current().environment().logging()
						.debug(this.getClass(), "心跳检测发现网络已断开。");
				ch = null;
				if (this.e != null) {
					try {
						this.e.channelInvalid(ch);
					} catch (Exception e2) {
						CJSystem.current().environment().logging().debug(
								this.getClass(),
								"心跳检测发现网络已断开并断开连接时出错。原因: " + e2);
					}
				}
			}
		}
	}

	// 客户端收到侦并确认
	@Override
	public void ackFrame(SocketChannel ch, String frameId) {
		Frame f = new Frame("heartbeat / net/1.1");
		f.head(NetConstans.FRAME_HEADKEY_FRAME_ID,
				String.format("cb:%s", frameId));
		// byte[] b = f.toBytes();
		// ByteBuffer buf = ByteBuffer.wrap(b);
		try {
			writer.write(f);
			// while(buf.hasRemaining())
			// ch.write(buf);
			// System.out.println("客户端心跳器确认服务器发来的心跳侦");
			logger.debug("客户端心跳器确认服务器发来的心跳侦，已确认并已回发给服务器。");
		} catch (IOException e) {
			CJSystem.current().environment().logging().error(this.getClass(),
					"心跳检测到服务器发来的确认侦，但回馈确认失败");
			throw new EcmException(e);
		} finally {
			visitChannel(ch);
		}
	}

	@Override
	public void setChannel(SocketChannel ch) {
		javach = ch;
		lastVisitTime = System.currentTimeMillis();
	}

	@Override
	public void event(IHeartbeatEvent e) {
		this.e = e;

	}

	@Override
	public void dispose() {
		if (heartbeatTimer != null) {
			heartbeatTimer.cancel();
			heartbeatTimer = null;
		}
		javach = null;
	}

	@Override
	public IWorker getAckHeartbeatCBFrameWork() {
		return new AckHeartbeatCBFrameWork(this);
	}

	@Override
	public IWorker getAckHeartbeatSBFrameWork() {
		// TODO Auto-generated method stub
		return new AckHeartbeatSBFrameWork(this);
	}

	class AckHeartbeatSBFrameWork implements IWorker, IDisposable {
		Frame frame;
		SocketChannel ch;
		ICablePin netInput;
		ICablePin netOutput;
		String selectType;
		IHeartbeat heartbeat;

		@Override
		public void dispose() {
			if (frame != null) {
				frame.dispose();
				frame = null;
			}
			ch = null;
			netInput = null;
			netOutput = null;
			heartbeat = null;
		}

		@Override
		public void init(Frame f, Circuit c, SocketChannel ch,
				ICablePin netInput, ICablePin netOutput, String gname,
				String simple) {
			this.frame = f;
			this.ch = ch;
			this.netInput = netInput;
			this.netOutput = netOutput;
		}

		public AckHeartbeatSBFrameWork(IHeartbeat heartbeat) {
			this.heartbeat = heartbeat;
		}

		@Override
		public Object call() throws Exception {
			synchronized (ch) {
				String frameId = frame.parameter("frameId");
				heartbeat.ackFrame(ch, frameId);
				dispose();
			}
			return null;
		}

	}

	class AckHeartbeatCBFrameWork implements IWorker, IDisposable {
		Frame frame;
		SocketChannel ch;
		ICablePin netInput;
		ICablePin netOutput;
		IHeartbeat heartbeat;

		@Override
		public void dispose() {
			if (frame != null) {
				frame.dispose();
				frame = null;
			}
			ch = null;
			netInput = null;
			netOutput = null;
			heartbeat = null;
		}

		@Override
		public void init(Frame f, Circuit c, SocketChannel ch,
				ICablePin netInput, ICablePin netOutput, String gname,
				String simple) {
			this.frame = f;
			this.ch = ch;
			this.netInput = netInput;
			this.netOutput = netOutput;
		}

		public AckHeartbeatCBFrameWork(IHeartbeat heartbeat) {
			this.heartbeat = heartbeat;
		}

		@Override
		public Object call() throws Exception {
			heartbeat.visitChannel(ch);
			dispose();
			return null;
		}

	}
}
