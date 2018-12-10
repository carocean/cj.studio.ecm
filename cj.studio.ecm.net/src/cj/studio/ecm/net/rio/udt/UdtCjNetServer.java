package cj.studio.ecm.net.rio.udt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

import com.barchart.udt.nio.ServerSocketChannelUDT;

import cj.studio.ecm.net.rio.BaseServerRIO;
import cj.ultimate.util.StringUtil;

public class UdtCjNetServer extends BaseServerRIO {
	// @Override
	// protected INetEngine openNetEngine(int port) throws IOException {
	//
	// ServerSocketChannel ssc =
	// SelectorProviderUDT.DATAGRAM.openServerSocketChannel();
	// ssc.socket().bind(new InetSocketAddress(port));
	// ssc.configureBlocking(false);
	// // ssc.bind(new InetSocketAddress(port));
	//
	// // Selector selector = SelectorProviderUDT.from(TypeUDT.STREAM)
	// // .openSelector();
	// Selector selector
	// =SelectorProviderUDT.from(TypeUDT.DATAGRAM).openSelector();
	// ssc.register(selector, SelectionKey.OP_ACCEPT);
	// return NetEngine.init(ssc, selector, netName());
	// }
	@Override
	protected AbstractSelectableChannel openNetEngine(String inetHost, int port) throws IOException {
		// 流模式
		ServerSocketChannel ssc = ServerSocketChannelUDT.open();
		ssc.configureBlocking(false);
		if (StringUtil.isEmpty(inetHost)) {
			ssc.bind(new InetSocketAddress(port));
		} else {
			ssc.bind(new InetSocketAddress(inetHost,port));
		}
//		Selector selector = Selector.open();
//		ssc.register(selector, SelectionKey.OP_ACCEPT);
		return ssc;
	}

	@Override
	public String simple() {
		// TODO Auto-generated method stub
		return "rio-udt";
	}


}
