package cj.studio.ecm.frame;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.net.nio.NetConstans;
import cj.ultimate.IDisposable;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;

/**
 * 从流中解析侦
 * 
 * <pre>
 *
 * </pre>
 * 
 * @author carocean
 *
 */
public class FramePacketDecoder implements IFrameDecoder, IDisposable {
	IFrameDecoderCallback e;
	ByteBuf tmp;
	Frame dirty;// 当前解析的侦数据：可能还未收到完整的侦。
	byte type;// －1表示未作务，0为正在接收头，1为正在接收参数，2为正在接收内容
	int contentLen;// 表示发现的内容长度。
	int found = 0;// 发现的连接分隔符数。\r\n\r\n
	long frameMaxLen;// 侦的最大大小，缓冲tmp如果超过此大小视为溢出。

	public FramePacketDecoder() {
		tmp = UnpooledByteBufAllocator.DEFAULT.buffer();
		dirty = new Frame();
		frameMaxLen = NetConstans.FRAME_MAX_LENGTH;
	}

	@Override
	public  void write(ByteBuf d, Object... attachArgs) {
		int len = d.readableBytes();
		if (len <= 0)
			return;
		try {
			if (type == 0) {
				d = parseHead(d);
			}
			if (type == 1) {
				d = parseParam(d);
			}
			if (type == 2) {
				d = parseContent(d, attachArgs);
			}
			if (d.readableBytes() > 0) {// 如果还有剩余，则递归
				write(d, attachArgs);
			}
		} catch (Exception e) {
			reset();
//			CJSystem.current().environment().logging().error(getClass(),e);
			CJSystem.current().environment().logging().error(getClass(),
					String.format("侦解析时出错，已重置，错误原因：%s", e.getMessage()));
		}
	}

	private void reset() {
		this.contentLen = 0;
		this.dirty = new Frame();
		this.type = 0;
		this.tmp.clear();
		this.found = 0;
	}

	private ByteBuf parseContent(ByteBuf d, Object... attachArgs) {
		if (contentLen == 0) {
			tmp.clear();
			type = 0;
			contentLen = 0;
			if (this.e != null) {
				e.onNewFrame(dirty, attachArgs);
			}
			dirty = new Frame();
			return d;
		}
		int len = d.readableBytes();
		for (int i = 0; i < len; i++) {
			byte b = d.readByte();
			tmp.writeByte(b);
			checkMaxLen();
			if (tmp.readableBytes() == contentLen) {
				dirty.content.writeBytes(tmp);
				tmp.clear();
				type = 0;
				contentLen = 0;
				if (this.e != null) {
					e.onNewFrame(dirty, attachArgs);
				}
				dirty = new Frame();
				break;
			}

		}
		return d;
	}

	private ByteBuf parseParam(ByteBuf d) {
		int len = d.readableBytes();
		for (int i = 0; i < len; i++) {
			byte b = d.readByte();
			if (b == '\r') {
				found++;
				continue;
			}
			if (b == '\n') {
				found++;
			}
			if (found == 2) {// 完成一个头了
				if (tmp.readableBytes() <= 0) {// 整个头部结束:为空说明本次是连续的分隔符，即凑够了4个分隔符了。
					type = 2;
					found = 0;
					break;
				}
				byte[] s = new byte[tmp.readableBytes()];
				tmp.readBytes(s);
				String h = new String(s);
				String key = "";
				String value = "";
				if (h.contains("=")) {
					int pos = h.indexOf("=");
					key = h.substring(0, pos).trim();
					value = h.substring(pos + 1, h.length());
				}
				dirty.parameter(key, value);
				tmp.clear();
				found = 0;
				continue;
			}
			tmp.writeByte(b);
			checkMaxLen();
		}
		return d;
	}

	private void checkMaxLen() {
		if (tmp.readableBytes() >= frameMaxLen) {
			tmp.clear();
			type = 0;
			contentLen = 0;
			throw new EcmException("侦超过最大大小" + frameMaxLen);
		}
	}

	private ByteBuf parseHead(ByteBuf d) {
		int len = d.readableBytes();
		for (int i = 0; i < len; i++) {
			byte b = d.readByte();
			if (b == '\r') {
				found++;
				continue;
			}
			if (b == '\n') {
				found++;
			}
			if (found == 2) {// 完成一个头了
				if (tmp.readableBytes() <= 0) {// 整个头部结束:为空说明本次是连续的分隔符，即凑够了4个分隔符了。
					type = 1;
					found = 0;
					break;
				}
				byte[] s = new byte[tmp.readableBytes()];
				tmp.readBytes(s);
				String h = new String(s);
				String key = "";
				String value = "";
				if (h.contains("=")) {
					int pos = h.indexOf("=");
					key = h.substring(0, pos).trim();
					value = h.substring(pos + 1, h.length());
				}
				dirty.head(key, value);
				if ("Content-Length".equals(key)) {
					if (StringUtil.isEmpty(value)) {
						throw new EcmException("侦内容长度未指定:" + dirty);
					}
					contentLen = Integer.valueOf(value);
				}
				tmp.clear();
				found = 0;
				continue;
			}
			tmp.writeByte(b);
			checkMaxLen();
		}
		return d;
	}

	@Override
	public void setDecoder(IFrameDecoderCallback e) {
		this.e = e;
	}

	@Override
	public void dispose() {
	}

	public static void main(String... strings) {
		Frame f = new Frame("get /test?swsid=xxxx&uid=rrrrr nl/1.0");
		// f.parameter("f-key1", "v1");
		// f.parameter("f-key2", "v2");
		// f.head("f-head1", "h1");
		// f.head("f-head2", "h2");
		// f.content.writeBytes("吊毛".getBytes());
		Frame f2 = new Frame("get /test2 nl/1.0");
		f2.parameter("f2-key1", "v1");
		f2.parameter("f2-key2", "v2");
		// f2.head("f2-head1", "h1");
		// f2.head("f2-head2", "h2");
		// f2.content.writeBytes("吃饭了吗".getBytes());
		Frame f3 = new Frame("get /test3 nl/1.0");
		// f3.parameter("f3-key1", "v1");
		// f3.parameter("f3-key2", "v2");
		// f3.head("f3-head1", "h1");
		// f3.head("f3-head2", "h2");
		// f3.content.writeBytes("喝水，那天喝王老吉".getBytes());

		FramePacketDecoder fd = new FramePacketDecoder();
		fd.setDecoder(new IFrameDecoderCallback() {

			@Override
			public Frame onNewFrame(Frame frame, Object... attachArgs) {
				System.out.println(frame);
				System.out.println(new String(frame.content.readFully()));
				System.out.println("------");
				return frame;
			}
		});

		ByteBuf b = f.toByteBuf();
		// fd.write(b);

		ByteBuf b2 = f2.toByteBuf();
		// fd.write(b2);
		ByteBuf b3 = f3.toByteBuf();
		// fd.write(b3);
		ByteBuf b4 = Unpooled.buffer();
		b4.writeBytes(f.toBytes());
		b4.writeBytes(f2.toBytes());
		b4.writeBytes(f3.toBytes());
		// fd.write(b4);

		b4.resetReaderIndex();
		byte[] raws = new byte[b4.readableBytes()];
		b4.readBytes(raws);

		for (int i = 0; i < raws.length; i++) {
			ByteBuf bb = Unpooled.buffer();
			bb.writeByte(raws[i]);
			fd.write(bb);
		}

	}
}
