package cj.studio.ecm.net.rio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import cj.studio.ecm.frame.FramePacketDecoder;
import cj.ultimate.IDisposable;

public interface IChannelContext extends IDisposable{
	public ByteBuffer getReadBuf();
	public FramePacketDecoder getDecoder();
	SendQueue getSq();
	String getChannelId();
	SocketChannel getChannel();
	void register(SelectionKey key, SocketChannel ch,
			ChannelNioMoniterPool pool) throws IOException;
	void startHearbeat(IHeartbeatEvent e);
	public IChannelWriter getWriter();
	
}
