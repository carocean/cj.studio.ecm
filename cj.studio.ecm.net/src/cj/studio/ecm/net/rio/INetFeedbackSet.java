package cj.studio.ecm.net.rio;

import cj.ultimate.IDisposable;


/**
 * 用于同步接收响应时等待超时后的处理：
 * 
 * <pre>
 * 1.超时后发送者回路结束
 * 2.如果发送者设置的了回路的回馈点
 * 3.net将通过异步方式通知回馈点
 * </pre>
 * 
 * @author carocean
 *
 */
public interface INetFeedbackSet extends IDisposable {
	String KEY_FEEDBACKSET="wait-feedback-map";
	boolean existsFeedbackCircuit(String frameId);

	NetFeedbackCircuit feeddbackCircuit(String frameId);

	void addFeedbackCircuit(String frameId, NetFeedbackCircuit feedbackCir);
	void removeFeedbackCircuit(String frameId);

	boolean isEmpty();

	void checkExpires();
	
}
