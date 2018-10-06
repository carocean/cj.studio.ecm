package cj.studio.ecm.graph;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.ultimate.IDisposable;

/**
 * 用于sink的回调式创建
 * 
 * <pre>
 * 见：电缆线。
 * </pre>
 * 
 * @author carocean
 *
 */
public class SinkCreateBy implements ISinkCreateBy, ISink, IDisposable {
	private IGraphCreator creator;
	private ISink sink;
	
	public SinkCreateBy(GraphCreator c) {
		creator = c;
	}
	/**
	 * 新建一个sink实例。
	 * <pre>
	 * call by pin or cablePin
	 * </pre>
	 * @param sinkName
	 * @return
	 */
	@Override
	public ISink newSink(String sinkName,IPin owner) {
		sink = creator.newSink(sinkName);
		if (sink == null) {
			throw new EcmException(String.format("name of sink :%s is not be created. ",
					sinkName));
		}
		if(sink instanceof ICreateCallbackSink){
			((ICreateCallbackSink)sink).setOwnerPin(owner);
		}
		return sink;
	}

	@Override
	public void flow(Frame frame,Circuit circuit, IPlug plug) throws CircuitException {
		sink.flow(frame,circuit, plug);
	}

	@Override
	public void dispose() {
		creator = null;
		sink = null;
	}
}
