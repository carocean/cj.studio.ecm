package cj.studio.ecm.net.rio;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import com.barchart.udt.nio.SocketChannelUDT;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.ICablePin;
import cj.ultimate.IDisposable;

public class ClientDisconnectWork implements IWorker, IDisposable {
	Frame frame;
	Circuit circuit;
	SocketChannel ch;
	ICablePin netInput;
	ICablePin netOutput;
	String gname;
	private String simple;

	@Override
	public void init(Frame frame, Circuit circuit, SocketChannel ch,
			ICablePin netInput, ICablePin netOutput, String gname,
			String simple) {
		this.frame = frame;
		this.circuit = circuit;
		this.ch = ch;
		this.netInput = netInput;
		this.netOutput = netOutput;
		this.gname = gname;
		this.simple = simple;
	}

	private void initCircuit(Circuit circuit, String gname, SocketChannel ch)
			throws IOException {
		SocketAddress local = ch instanceof SocketChannelUDT
				? ((SocketChannelUDT) ch).socketUDT().getLocalSocketAddress()
				: ch.getLocalAddress();
		SocketAddress remote = ch instanceof SocketChannelUDT
				? ((SocketChannelUDT) ch).socketUDT().getRemoteSocketAddress()
				: ch.getRemoteAddress();
		circuit.attribute("transfer-protocol", "net/1.1");
		circuit.attribute("select-type", "client");
		circuit.attribute("local-address", local.toString());
		circuit.attribute("remote-address", remote.toString());
		circuit.attribute("select-simple", simple);
		circuit.attribute("select-name", gname);
		circuit.attribute("select-id", Integer.toHexString(ch.hashCode()));
	}

	@Override
	public Object call() throws Exception {
		if (netOutput == null) {
			return null;
		}
		String wireName = Integer.toHexString(ch.hashCode());
		initCircuit(circuit, gname, (SocketChannel) ch);
		try {
			netOutput.flow(wireName, frame, circuit);
		} catch (Exception e) {
			throw e;
		} finally {
			netInput.removeWire(wireName);
			netOutput.removeWire(wireName);
			dispose();
		}
		return null;
	}

	@Override
	public void dispose() {
		if (frame != null) {
			frame.dispose();
			frame = null;
		}
		if (circuit != null) {
			circuit.dispose();
			circuit = null;
		}
		ch = null;
		netInput = null;
		netOutput = null;

	}

}
