package cj.studio.ecm.net.nio;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.List;

import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.frame.TcpFrame;

/**
 * <pre>
 * 
 *  格式：
 *  消息头：
 *   1.消息占位符，高位0表示，一个0表示有一个字节的长度说明。
 *   2.协议类型，共支持128个
 *   3.消息数据长度
 *   4.协议类型
 * 消息数据
 * @author cj
 * 
 */
//信道一旦建立，如果解析流时长度不对报错，后续全错。一个信道一旦建立是不可能被别人盗用往里塞数据的，否则连接的握手协议不成万金油了。
public class DelimiterFrameCodec2 extends ByteToMessageCodec2<Frame> {
	private long readBodyLen;
	private int readheaderlen;
	private long bodylen;
	private int headerlen;
	private byte protoType;
	private int task = 0;// 0表示状态2字节，1表示读取长度字节，2表示开始读侦头，3表示开始读侦体
	private byte flag1;
	private byte flag2;
	private TcpFrame frame;
	private ByteBuf headData;

	// 已修改了netty的ByteToMessageDecoder.callDecode类，使之在有输出时便执行后后续的handler
	// private ByteBuf discardReadBytes;//
	// 每次任务读取后将in缓冲中的数全部读完放到这，不然out输出的侦不会流到下一个handler，所以要将缓冲中还有的读到这里，其后再从这里读出，然后再从in中读
	/*
	 * 如果想把侦输出到下一个handler，则必须使得本次读的bytebuf读完，不然会被循环读取（如下源码）
	 */
	// 输出的侦为什么有时候不向后续的handler流动，便是因为这个循环，一定要将缓冲中的数据处理完才跳出此decode
	@Override
	protected synchronized void decode(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) throws Exception {
		switch (task) {
		case 0:
			if (in.readableBytes() <= 2)
				return;
			flag1 = in.readByte();
			flag2 = in.readByte();
			task += 1;
			break;
		case 1:
			readLength(ctx, in, out);
			headData = ctx.alloc().buffer(headerlen); /*
													 * Unpooled.directBuffer(
													 * headerlen);
													 */
			task += 1;
			break;
		case 2:
			if (headerlen > 0) {
				readHeaderData(ctx, in, out);
			}
			break;
		case 3:
			if (bodylen > 0) {
				readBodyData(in, out);
			}
			break;
		}
	}

	private void readLength(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) {
		protoType = (byte) (flag1 >>> 3);

		switch (flag1 & 7) {
		case 7:
			headerlen = 0;
			break;
		case 3:
			if (in.readableBytes() <= 1)
				return;
			byte b1 = in.readByte();
			headerlen = b1 & 0xFF;
			break;
		case 1:
			if (in.readableBytes() <= 2)
				return;
			b1 = in.readByte();
			byte b2 = in.readByte();
			headerlen = ((b1 & 0xFF) << 8) | (b2 & 0xFF);
			break;
		case 0:
			if (in.readableBytes() <= 3)
				return;
			b1 = in.readByte();
			b2 = in.readByte();
			byte b3 = in.readByte();
			headerlen = ((b1 & 0xFF) << 16) | ((b2 & 0xFF) << 8) | (b3 & 0xFF);
			break;
		default:
			throw new RuntimeException("无效侦头数据长度描述标志:" + flag1);
		}

		switch (flag2) {
		case 0x7F:
			if (in.readableBytes() <= 1)
				return;
			byte b1 = in.readByte();
			bodylen = (b1 & 0xFF);
			break;
		case 0x3F:
			if (in.readableBytes() <= 2)
				return;
			b1 = in.readByte();
			byte b2 = in.readByte();
			bodylen = ((b2 & 0xFF) << 8) | (b1 & 0xFF);
			break;
		case 0x1F:
			if (in.readableBytes() <= 3)
				return;
			b1 = in.readByte();
			b2 = in.readByte();
			byte b3 = in.readByte();
			bodylen = ((b3 & 0xFF) << 16) | ((b2 & 0xFF) << 8) | (b1 & 0xFF);
			break;
		case 0x0F:
			if (in.readableBytes() <= 4)
				return;
			b1 = in.readByte();
			b2 = in.readByte();
			b3 = in.readByte();
			byte b4 = in.readByte();
			bodylen = ((b4 & 0xFF) << 24) | ((b3 & 0xFF) << 16)
					| ((b2 & 0xFF) << 8) | (b1 & 0xFF);
			break;
		case 0x07:
			if (in.readableBytes() <= 5)
				return;
			b1 = in.readByte();
			b2 = in.readByte();
			b3 = in.readByte();
			b4 = in.readByte();
			byte b5 = in.readByte();
			bodylen = ((b5 & 0xFF) << 32) | ((b4 & 0xFF) << 24)
					| ((b3 & 0xFF) << 16) | ((b2 & 0xFF) << 8) | (b1 & 0xFF);
			break;
		case 0x03:
			if (in.readableBytes() <= 6)
				return;
			b1 = in.readByte();
			b2 = in.readByte();
			b3 = in.readByte();
			b4 = in.readByte();
			b5 = in.readByte();
			byte b6 = in.readByte();
			bodylen = ((b6 & 0xFF) << 40) | ((b5 & 0xFF) << 32)
					| ((b4 & 0xFF) << 24) | ((b3 & 0xFF) << 16)
					| ((b2 & 0xFF) << 8) | (b1 & 0xFF);
			break;
		case 0x01:
			if (in.readableBytes() <= 7)
				return;
			b1 = in.readByte();
			b2 = in.readByte();
			b3 = in.readByte();
			b4 = in.readByte();
			b5 = in.readByte();
			b6 = in.readByte();
			byte b7 = in.readByte();
			bodylen = ((b7 & 0xFF) << 48) | ((b6 & 0xFF) << 40)
					| ((b5 & 0xFF) << 32) | ((b4 & 0xFF) << 24)
					| ((b3 & 0xFF) << 16) | ((b2 & 0xFF) << 8) | (b1 & 0xFF);
			break;
		case 0:
			if (in.readableBytes() <= 8)
				return;
			b1 = in.readByte();
			b2 = in.readByte();
			b3 = in.readByte();
			b4 = in.readByte();
			b5 = in.readByte();
			b6 = in.readByte();
			b7 = in.readByte();
			byte b8 = in.readByte();
			bodylen = ((b8 & 0xFF) << 56) | ((b7 & 0xFF) << 48)
					| ((b6 & 0xFF) << 40) | ((b5 & 0xFF) << 32)
					| ((b4 & 0xFF) << 24) | ((b3 & 0xFF) << 16)
					| ((b2 & 0xFF) << 8) | (b1 & 0xFF);
			break;
		default:
			throw new RuntimeException("无效侦体数据长度描述标志:" + flag2);
		}
		if (bodylen <= 0)
			throw new RuntimeException("侦为负或0");
		frame = new TcpFrame();
		frame.setProtocolType(this.protoType);
		frame.setLength(bodylen);
		if (headerlen == 0) {// 如果头为空则直接推出侦，如果不为空则在头数据读取完毕后再推出侦
			out.add(frame);
		}
	}

	private void readHeaderData(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) {
		int canread = in.readableBytes();
		long remaining = headerlen - readheaderlen;
		// if (remaining > bufferSize && readheaderlen < headerlen
		// && canread < bufferSize) {
		// return;
		// }
		int len = (int) remaining < canread ? (int) remaining : canread;// 防止到消息结尾时多读，如果此处多读或者少读，均会造成后续的消息解析不出来。因此要准确计算它
		byte[] buf = new byte[len];
		in.readBytes(buf);
		headData.writeBytes(buf);
		readheaderlen += len;
		if (readheaderlen == headerlen) {
			byte[] b = new byte[headerlen];
			headData.readBytes(b);
			frame.writeHeader(b);
			out.add(frame);
			readheaderlen = 0;
			headData.release();
			task = 3;
		}

	}

	private void readBodyData(ByteBuf in, List<Object> out) {
		int canread = in.readableBytes();
		long remaining = bodylen - readBodyLen;
		// if (remaining > bufferSize && readBodyLen < bodylen
		// && canread < bufferSize) {
		// return;
		// }
		// int len = canread > bufferSize ? bufferSize
		// : ((int) (framelen - readedFrameLen) < canread ? (int) (framelen -
		// readedFrameLen)
		// : canread);
		int len = (int) remaining < canread ? (int) remaining : canread;// 防止到消息结尾时多读，如果此处多读或者少读，均会造成后续的消息解析不出来。因此要准确计算它
		byte[] buf = new byte[len];
		in.readBytes(buf);
		frame.write(buf, 0, len);
		readBodyLen += len;
		if (readBodyLen == bodylen) {
			readBodyLen = 0;
			readheaderlen = 0;
			bodylen = 0;
			headerlen = 0;
			task = 0;
		}
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, Frame netframe,
			ChannelPromise promise, boolean preferDirect) throws Exception {
		if(!(netframe instanceof TcpFrame))throw new RuntimeException("侦不是netframe");
		TcpFrame in=(TcpFrame)netframe;
		long bodylen = in.getLength();
		byte[] headerData = in.readHeader();
		int headerlen = headerData.length;
		if (headerlen > 16777215)
			throw new RuntimeException("侦头数据溢出。最多16777215字节，约16M大小");

		byte flag1 = (byte) (in.getProtocolType() << 3);// 高5位表示协议类型，低3位表示侦的头数据长度表示该长度的字节数，因此侦头最多表示16M数据
		flag1 |= 7;
		byte flag2 = -1;// 表示侦体的长度所占字节数

		int headerLenBytecnt = 0;
		int bodyLenbytecnt = 0;

		if (headerlen > 0 && headerlen <= 0xFF) {
			flag1 &= 0xFB;
			headerLenBytecnt = 1;
		} else if (headerlen > 0xFF && headerlen <= 0xFFFF) {
			flag1 &= 0xF9;
			headerLenBytecnt = 2;
		} else if (headerlen > 0xFFFF && headerlen <= 0xFFFFFFL) {
			flag1 &= 0xF8;
			headerLenBytecnt = 3;
		}

		if (bodylen > 0 && bodylen <= 0xFF) {
			flag2 = (byte) 0x7F;
			bodyLenbytecnt = 1;
		} else if (bodylen > 0xFF && bodylen <= 0xFFFF) {
			flag2 = (byte) 0x3F;
			bodyLenbytecnt = 2;
		} else if (bodylen > 0xFFFF && bodylen <= 0xFFFFFFL) {
			flag2 = (byte) 0x1F;
			bodyLenbytecnt = 3;
		} else if (bodylen > 0xFFFFFFL && bodylen <= 0xFFFFFFFFL) {
			flag2 = (byte) 0x0F;
			bodyLenbytecnt = 4;
		} else if (bodylen > 0xFFFFFFFFL && bodylen <= 0xFFFFFFFFFFL) {
			flag2 = (byte) 0x07;
			bodyLenbytecnt = 5;
		} else if (bodylen > 0xFFFFFFFFFFL && bodylen <= 0xFFFFFFFFFFFFL) {
			flag2 = (byte) 0x03;
			bodyLenbytecnt = 6;
		} else if (bodylen > 0xFFFFFFFFFFFFL && bodylen <= 0xFFFFFFFFFFFFFFL) {
			flag2 = (byte) 0x01;
			bodyLenbytecnt = 7;
		} else if (bodylen > 0xFFFFFFFFFFFFFFL
				&& bodylen <= 0xFFFFFFFFFFFFFFFFL) {
			flag2 = 0;
			bodyLenbytecnt = 8;
		} else {
			throw new RuntimeException();
		}
		byte[] frameheader = new byte[2 + headerLenBytecnt + bodyLenbytecnt];
		frameheader[0] = flag1;
		frameheader[1] = flag2;
		for (int i = 0; i < headerLenBytecnt; i++) {
			frameheader[2 + i] = (byte) (headerlen >> (i * 8));
		}
		for (int i = 0; i < bodyLenbytecnt; i++) {
			frameheader[headerLenBytecnt + 2 + i] = (byte) (bodylen >> (i * 8));
		}

		ByteBuf out = null;
		if (preferDirect) {
			out = ctx.alloc().ioBuffer();
		} else {
			out = ctx.alloc().heapBuffer();
		}
		out.writeBytes(frameheader);
		out.writeBytes(headerData);

		int bufferSize = 2048;// 缓冲不小于1K

		if (out.readableBytes() > bufferSize) {
			ctx.write(out, promise);
			if (preferDirect) {
				out = ctx.alloc().ioBuffer();
			} else {
				out = ctx.alloc().heapBuffer();
			}
		}

		byte[] buf = new byte[bufferSize];
		int readsize = 0;
		boolean isWrited = false;
		while (readsize <= bodylen) {
			int l = in.read(buf, 0, bufferSize);
			if (l == -1) {
				break;
			}
			out.writeBytes(buf, 0, l);
			if (out.readableBytes() > bufferSize * 2) {
				ctx.write(out, promise);
				if (preferDirect) {
					out = ctx.alloc().ioBuffer();
				} else {
					out = ctx.alloc().heapBuffer();
				}
				isWrited = true;
			} else {
				isWrited = false;
			}
			readsize += l;
		}
		//由于netty底层会释放bytebuf，因此此处不能释放它，否则会报refCnt错误
		/*
		 * 该方法在连续处理消息时会造成侦头和侦体的长度不正确错误，原因可能是反复写bytebuf时没有完全发送给服务器端造成。
		 * 如果将写的代码改成写入一个bytebuf而不反复创建就行，但问题是如果是文件消息过大，全部写入内存会非常慢。
		 * 之后再调试此BUG
		 */
		if (!isWrited) {
			ctx.write(out, promise);
		}
//		out.release();
		out = null;
	}

}
