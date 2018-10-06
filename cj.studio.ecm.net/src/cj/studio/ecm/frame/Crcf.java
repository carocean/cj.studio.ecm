package cj.studio.ecm.frame;

import io.netty.buffer.ByteBuf;

class Crcf {

	public Crcf() {
	}

	public int getBeginNewLine(ByteBuf buf) {
		for (int i = 0; i < buf.readableBytes(); i++) {
			if (buf.getByte(i) == '\r'&&buf.getByte(i+1)=='\n') {
				i+=1;
				continue;
			}
			return i;
		}
		return 0;
	}
	public int getEndNewLine(ByteBuf buf) {
		int len=buf.readableBytes();
		if (buf.getByte(len-2) == '\r'&&buf.getByte(len-1)=='\n') {
			return len-2;
		}
		return len;
	}
	public int nextNewLine(int prev, ByteBuf buf) {
		for (int i = prev; i < buf.readableBytes(); i++) {
			if (buf.getByte(i) == '\r'&&buf.getByte(i+1)=='\n') {
				return i;
			}
		}
		return -1;
	}

	

	

}
