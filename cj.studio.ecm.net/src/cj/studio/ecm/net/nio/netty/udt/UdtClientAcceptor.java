package cj.studio.ecm.net.nio.netty.udt;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
/**
 * 绑定物理通道，用于接收信息，并将之转换为graph的输入协议。
 * <pre>
 *
 * </pre>
 * @author carocean
 *
 */
public class UdtClientAcceptor implements ISink {


	@Override
	public void flow(Frame frame,Circuit circuit, IPlug plug) throws CircuitException {
		plug.flow(frame, circuit);
	}


}
