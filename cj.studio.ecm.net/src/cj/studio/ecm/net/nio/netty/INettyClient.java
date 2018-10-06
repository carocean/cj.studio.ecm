package cj.studio.ecm.net.nio.netty;

import cj.studio.ecm.net.IClient;
import cj.studio.ecm.net.INetGraphInitedEvent;

public interface INettyClient extends IClient{
	int connectMaxCount();
	int connectCount();
	int idleCount();
	int busyCount();
	void eventNetGraphInited(INetGraphInitedEvent event);
}
