package cj.studio.ecm.net.rio.udt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

import com.barchart.udt.nio.SocketChannelUDT;

import cj.studio.ecm.net.rio.BaseClientRIO;

public class UdtCjNetClient extends BaseClientRIO {
	/*根本原因：在DATAGRAM模式下，其select方法不会堵塞。可能是在linux系统上有此问题，netty,mina使用此功能均会导致cpu占满。
	 * 
	 * 而且只会导致客户端无限循环，服务器select会堵塞。
	 * 
	 * 因此确定产barchart udt 在DATAGRAM模式下的bug,故而只能采用流模式，需要开发粘包处理。
	 * */
//	@Override
//	protected INetEngine openNetEngine(String host, int port)
//			throws IOException {
//		// udt dagram模式下，read＝0时
//		// key会导致不停轮询，只有取消key才行
//		// 这很重要，否则陷入死循环，但撤销后后续的数据不再读取了。
//		// 死循环原因：由于ch.read返回0时，本应该在此时selector.select()方法堵塞以等下一批数据
//		// 但是它却并不堵塞，而是续继执行读方法，而读方法反回0,如此cpu就上升到100%
//		// java nio不是这样，这是udt在datagram数据报模式（流模式符合java nio api要求）时才有该问题。
//		// udt datagram数据报模式下一是选择器空闲时不拥堵，二是read的返回值不符合jdk nio的定义，
//		// udt 数据报模式下，关闭连接并不返回－1，它的负值是未知，如果没有数据总是返回0
//		SocketChannel sc = SelectorProviderUDT.DATAGRAM.openSocketChannel();
//		// sc.socket().connect(new InetSocketAddress(host, port));
//		sc.configureBlocking(false);
//		sc.socket().connect(new InetSocketAddress(host, port));
//
//		/*Selector selector = SelectorProviderUDT.STREAM.openSelector();*/
//		Selector selector = SelectorProviderUDT.from(TypeUDT.DATAGRAM)
//				.openSelector();
//		sc.register(selector, SelectionKey.OP_CONNECT);
//		return NetEngine.init(sc, selector, netName());
//	}

	@Override
	protected AbstractSelectableChannel openNetEngine(String host, int port)
			throws IOException {
		SocketChannel sc = SocketChannelUDT.open();// 流模式，该模式下的udt符合jdk nio api的定义。
		sc.configureBlocking(false);
		sc.connect(new InetSocketAddress(host, port));
//		Selector selector = Selector.open();
//		sc.register(selector, SelectionKey.OP_CONNECT);
		return sc;
	}

	@Override
	public String simple() {
		return "rio-udt";
	}



}
