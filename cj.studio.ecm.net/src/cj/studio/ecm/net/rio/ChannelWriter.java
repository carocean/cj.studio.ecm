package cj.studio.ecm.net.rio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import cj.studio.ecm.frame.Frame;

public class ChannelWriter implements IChannelWriter {

	private SocketChannel javachannel;

	public ChannelWriter(SocketChannel ch) {
		this.javachannel = ch;
	}

	/**
	 * 为了避免心跳线程写和数据写在同一java信道上的冲突而导致网络流上的侦不完整，从而引起侦解析错误的问题
	 */
	@Override
	public void write(Frame frame) throws IOException {
		synchronized (javachannel) {
			ByteBuffer buf = ByteBuffer.wrap(frame.toBytes());
			while (buf.hasRemaining()) {
				javachannel.write(buf);
			}
		}
	}

	@Override
	public void dispose() {
		javachannel = null;
	}

}
