package cj.studio.ecm.net.nio.netty;

import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.net.graph.INetGraph;

public interface INettyGraph extends INetGraph{
	/**
	 * 新建输入端子。用于接受netty的输入
	 * <pre>
	 * 由于每次连接均需要新的netty channel pipeline，因此，每个pipeline均要为之分配一个输入端子实例，
	 * </pre>
	 * @return
	 */
	IPin newAcceptorPin();
}
