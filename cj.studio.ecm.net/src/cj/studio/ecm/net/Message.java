package cj.studio.ecm.net;

/**
 * <pre>
 * 芯片消息。
 * 文本消息、字节消息，流式消息(用于文件接收）、http消息
 * 用于芯片内传输的消息。
 * 消息分为内部消息和网络消息，内部消息用于在芯片内处理，网络消息用于芯片间处理。由翻译器翻译。
 * @author cj
 * 
 */
public class Message {
	private long length;

	// 支持流式消息，可作为流的触发器，拉数据
	public int read(byte[] b, int i, long len) {
		return 0;
	}

	public void readFully(byte[] frame, int i, long len) {
		// TODO Auto-generated method stub

	}

	// 支持流式消息，可作为流的触发器，推数据
	public void write(byte[] b, int i, int length) {
		// TODO Auto-generated method stub

	}

	public long getLength() {
		return length;
	}

	public void setLength(long length) {
		this.length = length;
	}

}
