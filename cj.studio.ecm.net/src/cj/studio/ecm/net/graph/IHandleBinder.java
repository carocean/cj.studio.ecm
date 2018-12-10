package cj.studio.ecm.net.graph;

import io.netty.channel.ChannelHandler;
import cj.studio.ecm.net.nio.netty.INettyGraph;

public interface IHandleBinder extends ChannelHandler{
	void init(INettyGraph g);
}
