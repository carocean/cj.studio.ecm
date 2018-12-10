package cj.studio.ecm.net.nio.netty.udt;

import org.apache.log4j.Logger;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.frame.IFeedback;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.ISequenceNameGenerator;
import cj.studio.ecm.net.SyncPool;
import cj.studio.ecm.net.nio.NetConstans;
import cj.ultimate.util.StringUtil;

public class UdtClientSync implements ISink, NetConstans {
	SyncPool pool;
	ISequenceNameGenerator gen;
	Logger log = Logger.getLogger(UdtClientSync.class);

	public UdtClientSync() {
		pool = new SyncPool(2000);
		gen = new MessageIdGen();
	}

	@Override
	public void flow(Frame frame,Circuit circuit, IPlug plug) throws CircuitException {
		if (plug.owner().startsWith("input$")) {
			if ("connect".equals(frame.command())) {
				throw NetConstans.newCircuitException(STATUS_613);
			}
			if ("disconnect".equals(frame.command())) {
				plug.flow(frame, circuit);
				return;
			}
			Circuit c =circuit;
			if ("pull".equalsIgnoreCase(frame.command())
					|| "true".equals(frame
							.head(NetConstans.FRAME_HEADKEY_CIRCUIT_SYNC))) {
				// 如果是同步，则为之生成msgId
				String msgId = gen.genName();
				frame.head(NetConstans.FRAME_HEADKEY_FRAME_ID, msgId);
				//
				if (pool.put(msgId)) {
					frame.removeHead(FRAME_HEADKEY_CIRCUIT_SYNC);
					plug.flow(frame,circuit);
					String syn_timeout = frame
							.head(NetConstans.FRAME_HEADKEY_CIRCUIT_SYNC_TIMEOUT);
					if (StringUtil.isEmpty(syn_timeout)) {
						syn_timeout = "3800";
					}
					Long l = Long.valueOf(syn_timeout);
					Frame back = null;
					back = (Frame) pool.take(msgId, l);
					frame.removeHead(NetConstans.FRAME_HEADKEY_FRAME_ID);
					if (back != null) {
						back.removeHead(NetConstans.FRAME_HEADKEY_FRAME_ID);
						c.contentType("frame/bin");
						c.content().writeBytes(back.toBytes());
						c.status(frame.head("status"));
						c.message(frame.head("message"));
						// c.fireCallback(back);
						if (c.containsFeedback(IFeedback.KEY_INPUT_FEEDBACK)) {
							c.feedback(IFeedback.KEY_INPUT_FEEDBACK)
									.doBack(back, c);
						}

					} else {
						c.status(NetConstans.STATUS_202);
						c.message("等待超时");
						String cause = String
								.format("原因是等待超时，超时时间：%s毫秒，默认是3800毫秒。\r\n可以在frame头中指定：cj-syn-timeout，可以设置每侦的等待时间",
										syn_timeout);
						c.content().writeBytes(cause.getBytes());
					}
				}
				return;
			}
			plug.flow(frame, c);
			return;
		}
		if (plug.owner().startsWith("input_")) {
			String msgId = frame.head(NetConstans.FRAME_HEADKEY_FRAME_ID);
			if (frame.containsHead(NetConstans.FRAME_HEADKEY_FRAME_ID)
					&& pool.contains(msgId)) {
				// 同步消息到，通知解锁线程
				pool.notifya(msgId, frame);
				frame.removeHead(NetConstans.FRAME_HEADKEY_FRAME_ID);
				return;
			}
			plug.branch("output").flow(frame, circuit);
			return;
		}
	}

	class MessageIdGen implements ISequenceNameGenerator {
		int pos;
		int max;
		int begin;

		public MessageIdGen() {
			max = Integer.MAX_VALUE;
		}

		@Override
		public String genName() {
			if (pos < max)
				pos++;
			else {
				pos = begin + 1;
			}
			return pos + "";
		}

	}
}
