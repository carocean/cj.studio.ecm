package cj.studio.ecm.net.rio;

import java.nio.channels.SocketChannel;

public interface IHeartbeatEvent {
	void channelInvalid(SocketChannel ch) throws Exception;
}
