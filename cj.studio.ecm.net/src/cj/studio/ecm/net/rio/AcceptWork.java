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

public class AcceptWork extends Worker implements IWorker, IDisposable {
	Frame f;
	Circuit c;
	SocketChannel ch;
	ICablePin netInput;
	ICablePin netOutput;
	String selectType;
	String gname;
	// ReentrantLock lock;
	private String simple;

	public AcceptWork() {
	}

	@Override
	public void init(Frame frame, Circuit circuit, SocketChannel ch,
			ICablePin netInput, ICablePin netOutput, String gname,
			String simple) {
		this.f = frame;
		this.c = circuit;
		this.ch = ch;
		this.netInput = netInput;
		this.netOutput = netOutput;
		this.gname = gname;
		this.simple = simple;
		// lock = new ReentrantLock();
	}

	protected void initCircuit(Circuit circuit, String gname, SocketChannel ch,
			String selectId) throws IOException {
		SocketAddress local = ch instanceof SocketChannelUDT
				? ((SocketChannelUDT) ch).socketUDT().getLocalSocketAddress()
				: ch.getLocalAddress();
		SocketAddress remote = ch instanceof SocketChannelUDT
				? ((SocketChannelUDT) ch).socketUDT().getRemoteSocketAddress()
				: ch.getRemoteAddress();
		circuit.attribute("transfer-protocol", "net/1.1");
		circuit.attribute("select-type", "server");
		circuit.attribute("local-address", local.toString());
		circuit.attribute("remote-address", remote.toString());
		circuit.attribute("select-simple", simple);
		circuit.attribute("select-name", gname);
		circuit.attribute("select-id", selectId);
	}

	@Override
	public Object call() throws Exception {
		IFeedback fb = null;
		String wireName = Integer.toHexString(ch.hashCode());
		IChannelContext ctx = (IChannelContext) netOutput.wireOptions(wireName,
				"channel-context");
		try {

			initCircuit(c, gname, (SocketChannel) ch, wireName);

			fb = rioServerFeedbackSetSource(ctx, f, c);
			netOutput.flow(wireName, f, c);

			if (c.isPiggybacking()) {
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
					back = c.snapshot("connectOK / net/1.1");
				}
				ctx.getWriter().write(back);
				// ByteBuffer buf = ByteBuffer.wrap(back.toBytes());
				// while (buf.hasRemaining()) {
				// ch.write(buf);
				// }
				if ("disconnect".equals(c.attribute("net-action"))) {
					ch.close();
				}
			}

		} catch (Exception e) {
			CJSystem.current().environment().logging().error(this.getClass(),
					e);
			if (c.isPiggybacking()) {
				Frame error = c.snapshot();
				ctx.getWriter().write(error);
				// ByteBuffer buf = ByteBuffer.wrap(error.toBytes());
				// while (buf.hasRemaining()) {
				// ch.write(buf);
				// }

			}
			CircuitException ce = CircuitException.search(e);
			if (ce == null) {
				ce = new CircuitException(NetConstans.STATUS_603, e);
			}
			throw new EcmException(ce);
		} finally {
			// lock.unlock();
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
