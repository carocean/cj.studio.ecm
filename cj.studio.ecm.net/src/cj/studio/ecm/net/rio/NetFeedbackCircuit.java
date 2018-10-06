package cj.studio.ecm.net.rio;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.IFeedback;
import cj.ultimate.IDisposable;

/**
 * 
 * <pre>
 *
 * </pre>
 * 
 * @author carocean
 * @see {@link NetFeedbackSet} {@link NetFeedbackCircuit}
 */
public class NetFeedbackCircuit implements IDisposable {
	IFeedback feedback;
	Circuit circuit;
	long customtimeout;// 开发者定义的该条回路中的异步超时时间
	long recordTime;// 登记时间。

	@Override
	public void dispose() {
		feedback = null;
		circuit = null;
	}

	/**
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param feedback
	 * @param circuit
	 * @param timeout
	 *            异步等待超时时间
	 */
	public NetFeedbackCircuit(IFeedback feedback, Circuit circuit, long timeout) {
		this.feedback = feedback;
		this.circuit = circuit;
		this.customtimeout = timeout < 3600?3600:timeout;
		recordTime = System.currentTimeMillis();
	}

	public Circuit getCircuit() {
		return circuit;
	}

	public void setCircuit(Circuit circuit) {
		this.circuit = circuit;
	}

	public IFeedback getFeedback() {
		return feedback;
	}

	public void setFeedback(IFeedback feedback) {
		this.feedback = feedback;
	}

	public long getCustomTimeout() {
		return customtimeout;
	}

	public long getRecordTime() {
		return recordTime;
	}
}
