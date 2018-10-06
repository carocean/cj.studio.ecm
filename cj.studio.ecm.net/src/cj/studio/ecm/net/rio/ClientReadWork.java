package cj.studio.ecm.net.rio;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import com.barchart.udt.nio.SocketChannelUDT;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.frame.IFeedback;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.ICablePin;
import cj.studio.ecm.net.nio.NetConstans;
import cj.ultimate.IDisposable;
import cj.ultimate.util.StringUtil;

public class ClientReadWork extends Worker implements IWorker, IDisposable, NetConstans {
	Frame f;
	Circuit c;
	SocketChannel ch;
	ICablePin netInput;
	ICablePin netOutput;
	String selectType;
	String gname;
//	ReentrantLock lock;
	private String simple;
	public ClientReadWork() {
	}

	@Override
	public void init(Frame f, Circuit c, SocketChannel ch, ICablePin netInput,
			ICablePin netOutput, String gname,String simple) {
		this.f = f;
		this.c = c;
		this.ch = ch;
		this.netInput = netInput;
		this.netOutput = netOutput;
		this.gname = gname;
//		lock = new ReentrantLock();
		this.simple=simple;
	}

	private void initCircuit(Circuit circuit, String gname, SocketChannel ch,String wirename)
			throws IOException {
		SocketAddress local = ch instanceof SocketChannelUDT ? ((SocketChannelUDT) ch)
				.socketUDT().getLocalSocketAddress() : ch.getLocalAddress();
		SocketAddress remote = ch instanceof SocketChannelUDT ? ((SocketChannelUDT) ch)
				.socketUDT().getRemoteSocketAddress() : ch.getRemoteAddress();
		circuit.attribute("transfer-protocol", "net/1.1");
		circuit.attribute("select-type", "client");
		circuit.attribute("local-address", local.toString());
		circuit.attribute("remote-address", remote.toString());
		circuit.attribute("select-simple", simple);
		circuit.attribute("select-name", gname);
		circuit.attribute("select-id", wirename);
	}

	@Override
	public Object call() throws Exception {

//		lock.lock();//按通道ch加锁

		String frameId = f.head(NetConstans.FRAME_HEADKEY_FRAME_ID);
		IFeedback fb = null;
		String wirename=Integer.toHexString(ch.hashCode());
		IChannelContext ctx=(IChannelContext) netOutput.wireOptions(wirename,"channel-context");
		try {
			if (f.containsHead(NetConstans.FRAME_HEADKEY_FRAME_ID)) {// 是服务器发来的请求侦或发来的确认侦
				// 用于通知下游的net对该侦采用同步模式发送。如果开发者在链路中去除了该属性，则源头的调用者将等待超时。因为后续没有同步返回结果。
				f.head(FRAME_HEADKEY_CIRCUIT_SYNC, "true");
				// 侦的编号对开发者不可见。
				f.removeHead(FRAME_HEADKEY_FRAME_ID);
				c.piggybacking(true);
			}
			
			initCircuit(c, gname, ch,wirename);
			
			
			rioServerFeedbackSetSource(ctx, f, c);

			netOutput.flow(wirename, f, c);

			if (!c.isPiggybacking()) {
				return null;
			}
			Frame back = null;
			boolean isunwrap = false;
			if (c.containsContentType()) {
				String v = c.contentType();
				if ("frame/bin".equals(v) || "frame/json".equals(v)) {
					isunwrap = true;
				}
			}
			if (isunwrap) {
				back = new Frame(c.content().readFully());
			} else {
				back = c.snapshot("piggyback /");
			}
			String fid = String.format("c:%s", frameId);
			back.head(NetConstans.FRAME_HEADKEY_FRAME_ID, fid);
			ctx.getWriter().write(back);
//			ByteBuffer buf = ByteBuffer.wrap(back.toBytes());
//			while (buf.hasRemaining()) {
//				ch.write(buf);
//			}
			if ("disconnect".equals(c.attribute("net-action"))) {
				ch.close();
			}
		} catch (Exception e) {
			CJSystem.current().environment().logging()
					.error(this.getClass(), e);
			e.printStackTrace();
			if (c.isPiggybacking()) {
				c.message(c.message().replace("\r", "<br>")
						.replace("\n", "<br>"));
				Frame error = c.snapshot();
				String fid = String.format("s:%s", frameId);
				error.head(NetConstans.FRAME_HEADKEY_FRAME_ID, fid);
				if ("true"
						.equals(f
								.head(NetConstans.FRAME_HEADKEY_RIO_DESPLAYEXCEPTIONCAUSE))) {
					String cause = c.cause();
					if (!StringUtil.isEmpty(cause)) {
						error.content().writeBytes(cause.getBytes());
					}
				}
				ctx.getWriter().write(error);
//				ByteBuffer buf = ByteBuffer.wrap(error.toBytes());
//				while (buf.hasRemaining()) {
//					ch.write(buf);
//				}
			}
			CircuitException ce = CircuitException.search(e);
			if (ce == null) {
				ce = new CircuitException(NetConstans.STATUS_603, e);
			}
			throw new EcmException(ce);
		} finally {
//			lock.unlock();
			if (fb != null) {
				fb.dispose();
			}
			dispose();
		}
		return null;
	}

	@Override
	public void dispose() {
		if (f != null) {
			f.dispose();
			f = null;
		}
		if (c != null) {
			c.dispose();
			c = null;
		}
		ch = null;
		netInput = null;
		netOutput = null;
	}
}