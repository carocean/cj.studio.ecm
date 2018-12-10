package cj.studio.ecm.net.rio.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

import cj.studio.ecm.net.rio.BaseServerRIO;
import cj.ultimate.util.StringUtil;

public class TcpCjNetServer extends BaseServerRIO {
	@Override
	protected AbstractSelectableChannel openNetEngine(String inetHost, int port) throws IOException {
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		if (StringUtil.isEmpty(inetHost)) {
			ssc.bind(new InetSocketAddress(port));
		}else{
			ssc.bind(new InetSocketAddress(inetHost,port));
		}
		return ssc;
	}

	@Override
	public String simple() {
		// TODO Auto-generated method stub
		return "rio-tcp";
	}


}
