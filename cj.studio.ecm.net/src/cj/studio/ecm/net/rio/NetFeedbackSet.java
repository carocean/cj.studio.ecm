package cj.studio.ecm.net.rio;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.net.nio.NetConstans;
/**
 * 
 * <pre>
 *
 * </pre>
 * @author carocean
 * @see {@link NetFeedbackSet} {@link NetFeedbackCircuit}
 */
class NetFeedbackSet implements INetFeedbackSet {
	private Map<String, NetFeedbackCircuit> map;// key=frameId

	public NetFeedbackSet() {
		map = new ConcurrentHashMap<String, NetFeedbackCircuit>();
	}

	@Override
	public void dispose() {
		map.clear();
		map = null;
	}

	@Override
	public void checkExpires() {
		Iterator<String> it = map.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			NetFeedbackCircuit fc = map.get(key);
			if (System.currentTimeMillis() - fc.recordTime >= fc.customtimeout) {
				expire(key, fc);
			}
		}

	}
	//回馈点可捕获的状态有：200成功在同步下接收，201成功异步，202异步超时。
	private void expire(String frameId, NetFeedbackCircuit fc) {
		Frame f = new Frame("expire / NET/1.1");
		f.head(NetConstans.FRAME_HEADKEY_FRAME_ID, frameId);
		f.head("record-time", String.valueOf(fc.recordTime));
		f.head("custom-timeout", String.valueOf(fc.customtimeout));
		f.head("waiting-time",
				String.valueOf(System.currentTimeMillis() - fc.recordTime));
		fc.circuit.status(NetConstans.STATUS_202);// 202表示异步超时，侦未被收到或被丢弃。
		fc.circuit.message("异步超时");
		fc.circuit
				.cause("在给定的异步超时时间内，或是网络问题侦未被收到或在异步超时时间之后收到而被丢弃。一般异步超时时间在10分钟以上，故而未被收到的原因多数是网络信号丢失。");
		try {
			fc.feedback.doBack(f, fc.circuit);
		} catch (CircuitException e) {
			throw new EcmException(String.format("对侦标识为：%s的异步超时处理失败。",frameId));
		}
	}

	@Override
	public boolean existsFeedbackCircuit(String frameId) {
		// TODO Auto-generated method stub
		return map.containsKey(frameId);
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return map.isEmpty();
	}

	@Override
	public NetFeedbackCircuit feeddbackCircuit(String frameId) {
		// TODO Auto-generated method stub
		return map.get(frameId);
	}

	@Override
	public void addFeedbackCircuit(String frameId,
			NetFeedbackCircuit feedbackCir) {
		map.put(frameId, feedbackCir);

	}

	@Override
	public void removeFeedbackCircuit(String frameId) {
		if(map.containsKey(frameId)){
			NetFeedbackCircuit fc=map.get(frameId);
			fc.dispose();
		}
		map.remove(frameId);
	}

}
