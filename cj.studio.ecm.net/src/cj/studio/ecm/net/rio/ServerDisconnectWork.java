package cj.studio.ecm.net.rio;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import com.barchart.udt.nio.SocketChannelUDT;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.ICablePin;
import cj.ultimate.IDisposable;

public class ServerDisconnectWork implements IWorker, IDisposable {
	SocketChannel ch;
	ICablePin netInput;
	ICablePin netOutput;
	String gname;
	Frame f;
	Circuit c;
	private String simple;

	@Override
	public void init(Frame f, Circuit c, SocketChannel ch, ICablePin netInput,
			ICablePin netOutput, String gname,String simple) {
		this.simple=simple;
		this.f = f;
		this.c=c;
		this.ch = ch;
		this.netInput = netInput;
		this.netOutput = netOutput;
		this.gname = gname;
	}

	private void initCircuit(Circuit circuit, String gname, SocketChannel ch)
			throws IOException {
		SocketAddress local = ch instanceof SocketChannelUDT ? ((SocketChannelUDT) ch)
				.socketUDT().getLocalSocketAddress() : ch.getLocalAddress();
		SocketAddress remote = ch instanceof SocketChannelUDT ? ((SocketChannelUDT) ch)
				.socketUDT().getRemoteSocketAddress() : ch.getRemoteAddress();
		circuit.attribute("transfer-protocol", "net/1.1");
		circuit.attribute("select-type", "server");
		circuit.attribute("local-address", local.toString());
		circuit.attribute("remote-address", remote.toString());
		circuit.attribute("select-simple", simple);
		circuit.attribute("select-name", gname);
		circuit.attribute("select-id", Integer.toHexString(ch.hashCode()));
	}

	@Override
	public Object call() throws Exception {
		if(netOutput==null){
			return null;
		}
		String wireName = Integer.toHexString(ch.hashCode());
		initCircuit(c, gname, (SocketChannel) ch);
		
		netOutput.flow(wireName, f, c);

		netInput.removeWire(wireName);
		netOutput.removeWire(wireName);
		dispose();
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
