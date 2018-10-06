package cj.studio.ecm.net.rio;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface INioRegisterWakeup {
	void wakeup();

	void offerRegisterTask(SelectionKey key) throws IOException;
}
