package cj.studio.ecm.graph;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
/**
 * 在电缆线新建导线时创建新的Sink，保证每条导线均拥有自己的sink实例
 * <pre>
 * －该方案的替代做法在电缆线的new导线事件中构建导线的sink为最好方案，但是在功能已经做好，只需要一些修饰时可以此方案扩展电缆功能
 * －用法：
 * 实现类在flow方法中（主电缆执行）实现功能，在newSink方法中创建其自身，这样可做到主与子的功能相同。如：
 * class A implments ISinkCreateBy{
 * 	 public void flow(Frame frame, Circuit circuit, IPlug plug)
			throws CircuitException{
			//todo something 实现功能
			plug.flow(frame,circuit);
	}
	public ISink newSink(String sinkName,IPin owner){
		return new A();
	}
 * }
 * </pre>
 * @author carocean
 *
 */
public interface ISinkCreateBy extends ISink {
	/**
	 * 电缆的子导线执行该方法
	 * <pre>
	 *
	 * </pre>
	 * @param sinkName
	 * @param owner
	 * @return
	 */
	public ISink newSink(String sinkName,IPin owner);
	/**
	 * 电缆主导线执行该方法
	 */
	 void flow(Frame frame, Circuit circuit, IPlug plug)
			throws CircuitException;
}
