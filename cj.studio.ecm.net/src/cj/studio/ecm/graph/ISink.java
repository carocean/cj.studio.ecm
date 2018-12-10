package cj.studio.ecm.graph;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;

/**
 * 槽，流过程中的功能处理单元
 * 一个sink可以有多个插头plug
 * @author carocean
 *
 */
public interface ISink {


	public  void flow(Frame frame, Circuit circuit,IPlug plug) throws CircuitException;
}
