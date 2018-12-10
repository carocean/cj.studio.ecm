package cj.studio.ecm.net.rio;

import java.nio.channels.SocketChannel;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.ICablePin;
import cj.studio.ecm.net.nio.NetConstans;
import cj.ultimate.IDisposable;

public class AckFrameWork implements IWorker, IDisposable {
	Frame frame;
	Circuit c;
	SocketChannel ch;
	ICablePin netInput;
	ICablePin netOutput;
	String selectType;
	String gname;
	IHeartbeat heartbeat;
	private String frameId;
	SendQueue sq;

	public AckFrameWork(IHeartbeat heartbeat, String frameId, SendQueue sq) {
		this.heartbeat = heartbeat;
		this.frameId = frameId;
		this.sq = sq;
	}

	@Override
	public void dispose() {
		if (frame != null) {
			frame.dispose();
			frame = null;
		}
		if (c != null) {
			c.dispose();
			c = null;
		}
		ch = null;
		netInput = null;
		netOutput = null;
		heartbeat = null;
	}

	@Override
	public void init(Frame f, Circuit c, SocketChannel ch, ICablePin netInput,
			ICablePin netOutput, String gname,String simple) {
		this.frame = f;
		this.c = c;
		this.ch = ch;
		this.netInput = netInput;
		this.netOutput = netOutput;
		this.gname = gname;
	}

	@Override
	public Object call() throws Exception {
		heartbeat.visitChannel(ch);

		String chId = Integer.toHexString(ch.hashCode());
		if (netInput.containsWire(chId)) {
			INetFeedbackSet set = (INetFeedbackSet) netInput.wireOptions(chId,
					INetFeedbackSet.KEY_FEEDBACKSET);
			if (set != null && set.existsFeedbackCircuit(frameId)) {
				try {
					NetFeedbackCircuit fc = set.feeddbackCircuit(frameId);
					fc.circuit.status(NetConstans.STATUS_201);
					fc.circuit.message("异步回馈模式。");
					fc.circuit
							.cause("原因是：同步超时，回馈给发送者设置了回馈点。\r\n注意：异步模式与同步模式的线程不同。同步在发送者的线程下，而异步在net工作线程下。");
					// 异步也可能会超时，异步超时异常是202，故回馈点可捕获的状态有：200成功在同步下接收，201成功异步，202异步超时。见：netConstants
					fc.feedback.doBack(frame, fc.circuit);
				} catch (Exception e) {
					throw e;
				} finally {
					set.removeFeedbackCircuit(frameId);
					if (set.isEmpty()) {
						netInput.removeWireOptions(chId, "wait-feedback-map");
					} else {// 检查当前信道中过期的回馈
						set.checkExpires();
					}
					dispose();
				}
			}
		}
		return null;

	}

}
