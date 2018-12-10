package cj.studio.ecm.net.rio.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

import cj.studio.ecm.net.rio.BaseClientRIO;

public class TcpCjNetClient extends BaseClientRIO {
	public TcpCjNetClient() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected AbstractSelectableChannel openNetEngine(String host, int port)
			throws IOException {
		SocketChannel sc = SocketChannel.open();
		sc.configureBlocking(false);
		sc.connect(new InetSocketAddress(host, port));
		return sc;
	}

	@Override
	public String simple() {
		return "rio-tcp";
	}

}
