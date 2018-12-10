package cj.studio.ecm.net.nio.netty.tcp;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.nio.NetConstans;

/**
 * 绑定物理通道，用于接收信息，并将之转换为graph的输入协议。
 * 
 * <pre>
 *
 * </pre>
 * 
 * @author carocean
 *
 */
public class TcpServerAcceptor implements ISink,NetConstans {


	@Override
	public void flow(Frame frame,Circuit circuit, IPlug plug) throws CircuitException {
//		if (frame.containsHead(FRAME_HEADKEY_FRAME_ID)) {
//			circuit.head(FRAME_HEADKEY_FRAME_ID, frame.head(FRAME_HEADKEY_FRAME_ID));
//		}
//		if ("pull".equalsIgnoreCase(frame.command())||frame.containsHead(FRAME_HEADKEY_FRAME_ID)) {
//			circuit.head(FRAME_HEADKEY_CIRCUIT_SYNC, frame.head(FRAME_HEADKEY_CIRCUIT_SYNC));
//		}
		plug.flow(frame, circuit);
	}


}
