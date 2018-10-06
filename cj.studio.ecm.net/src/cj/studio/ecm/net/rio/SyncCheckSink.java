package cj.studio.ecm.net.rio;

import java.io.IOException;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.frame.IFeedback;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.ICreateCallbackSink;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.net.nio.NetConstans;
import cj.ultimate.util.StringUtil;

class SyncCheckSink implements ICreateCallbackSink {

	private IPin owner;

	@Override
	public void setOwnerPin(IPin pin) {
		this.owner = pin;
	}

	@Override
	public void flow(Frame frame, Circuit circuit, IPlug plug)
			throws CircuitException {
		Circuit c = circuit;

		if (!"true"
				.equals(frame.head(NetConstans.FRAME_HEADKEY_CIRCUIT_SYNC))) {
			sendFrame(frame, circuit, plug);
			return;
		}
		IChannelContext ctx = null;
		ctx = (IChannelContext) owner.options("channel-context");
		SendQueue sq = ctx.getSq();

		String fid = sq.genFrameId();
		frame.head(NetConstans.FRAME_HEADKEY_FRAME_ID, fid);
		if (sq.put(fid)) {
			sendFrame(frame, circuit, plug);// 变更参数：date:2016/2/19
			Frame back = null;
			long syncTimeout = sq.getCircuitDefaultSyncTime();
			if (frame.containsHead(
					NetConstans.FRAME_HEADKEY_CIRCUIT_SYNC_TIMEOUT)) {
				String v = frame
						.head(NetConstans.FRAME_HEADKEY_CIRCUIT_SYNC_TIMEOUT);
				if (StringUtil.isEmpty(v)) {
					v = "3600";
				}
				long t = Long.valueOf(v);
				if (t >= NetConstans.FRAME_HEADKEY_CIRCUIT_SYNC_MAX_TIMEOUT) {
					throw NetConstans.newCircuitException(
							NetConstans.STATUS_607,
							NetConstans.FRAME_HEADKEY_CIRCUIT_SYNC_MAX_TIMEOUT);
				}
				if (t > syncTimeout) {
					syncTimeout = t;
				}
			}
			plug.optionsPin(NetConstans.PIN_WORK_STATUS_KEY, "busy");
			back = (Frame) sq.take(fid, syncTimeout);
			plug.optionsPin(NetConstans.PIN_WORK_STATUS_KEY, "idle");
			frame.removeHead(NetConstans.FRAME_HEADKEY_FRAME_ID);
			if (back != null) {
				back.removeHead(NetConstans.FRAME_HEADKEY_FRAME_ID);
				if (c.containsFeedback(IFeedback.KEY_INPUT_FEEDBACK)) {// 如果使用了回馈点，则返回的捎带侦以回馈点方式输送，否则写进回路
					c.feedback(IFeedback.KEY_INPUT_FEEDBACK).doBack(back, c);
				} else {
					c.contentType("frame/bin");
					c.content().writeBytes(back.toBytes());
					c.status(back.head("status"));
					c.message(back.head("message"));
				}
			} else {
				// System.out.println(String.format("%s %s \r\n%s",
				// c.status(), c.message(), cause));
				// 超时了，检查回路，并用异步方式通知。这样做到侦永不丢失。
				if (c.containsFeedback(IFeedback.KEY_INPUT_FEEDBACK)) {
					// 异步等待时间
					long asyncTimeout = 3600;
					if (frame.containsHead(
							NetConstans.FRAME_HEADKEY_CIRCUIT_ASYNC_TIMEOUT)) {
						String v = frame.head(
								NetConstans.FRAME_HEADKEY_CIRCUIT_ASYNC_TIMEOUT);
						if (StringUtil.isEmpty(v)) {
							v = "3600";
						}
						long t = Long.valueOf(v);
						if (t >= NetConstans.FRAME_HEADKEY_CIRCUIT_ASYNC_MAX_TIMEOUT) {
							throw NetConstans.newCircuitException(
									NetConstans.STATUS_608,
									NetConstans.FRAME_HEADKEY_CIRCUIT_ASYNC_MAX_TIMEOUT);
						}
						if (t > asyncTimeout) {
							asyncTimeout = t;
						}
					}
					// 发现回馈点，回馈点等待反馈
					waitFeedback(c.feedback(IFeedback.KEY_INPUT_FEEDBACK),
							asyncTimeout, fid, c, plug);
				} /*else {*/
				// 不论是同步还是异常对于主回路c，此时均超时了，因此必须告诉主回路已超时。因此将else注释掉了，先前没注释掉时，主回路超时不超时都是返回200，这不对
				c.status(NetConstans.STATUS_202);
				c.message("同步等待超时");
				String cause = String.format(
						"原因是等待超时，超时时间：%s毫秒。如一定需要接收到反回侦，则可使用回馈点：IFeedback.KEY_INPUT_FEEDBACK",
						syncTimeout);
				c.content().writeBytes(cause.getBytes());
				/*}*/
			}
		}
		return;

	}

	private void waitFeedback(IFeedback feedback, long asynTimeout, String fid,
			Circuit c, IPlug plug) {
		// 将回馈回路放到当前导线上，等侦到了通过id执行回馈。
		INetFeedbackSet set = null;
		if (plug.option(INetFeedbackSet.KEY_FEEDBACKSET) == null) {
			set = new NetFeedbackSet();
			plug.optionsPin(INetFeedbackSet.KEY_FEEDBACKSET, set);
		} else {
			set = (INetFeedbackSet) plug
					.optionsPin(INetFeedbackSet.KEY_FEEDBACKSET);
		}
		NetFeedbackCircuit nc = new NetFeedbackCircuit(feedback, c,
				asynTimeout);
		set.addFeedbackCircuit(fid, nc);
	}

	private void sendFrame(Frame frame, Circuit c, IPlug plug)
			throws CircuitException {
		// byte[] b = frame.toBytes();
		// ByteBuffer d = ByteBuffer.wrap(b);
		IChannelContext ctx = null;
		ctx = (IChannelContext) owner.options("channel-context");
		IChannelWriter writer = ctx.getWriter();
		try {
			writer.write(frame);

			// while (d.hasRemaining()) {
			// ch.write(d);//在客户端主动断开后，此处写便有异常了，而此时服务器端的远程断开事件线程还没有对电缆线中的导线进行释放，因此服务器端仍在不停的向已关闭的客户端写，直到关闭事件之后。
			// }
			// 解决方案：该类由netengine中的graph构建，可传入output,input，当此处得到信道关闭后，可将输入输出中的导线移除，但要注意：
			// 移除后可能net/1.1中的disconnect事件便不被触发了，因为有可能在前面。所以要有全面的测试方可动此处代码。
			// 目前net中的disconnect事件是通过ch.read为－1时得到的并触发的，而此处是写，因此如果一个信道很长时间不读，忽然关闭了netty
			// nio的ch.read方法不是立马返回－1，中间有停带时间，因此：
			// 此处最好也像netengine.workServerDisconnect方法一样在此处是重调一遍ServerDisconnectWork,该worker可经由netGraph传入
			// 最好测一下ch的read,write读时和写时对远程关闭的原生情况，再对症下药。
		} catch (IOException e) {
			// if(e instanceof ClosedChannelException||"Broken
			// pipe".equals(e.getMessage())){
			// plug.option("socket-channel",null);
			// }
			throw new CircuitException(NetConstans.STATUS_609, e);
		}
	}

}