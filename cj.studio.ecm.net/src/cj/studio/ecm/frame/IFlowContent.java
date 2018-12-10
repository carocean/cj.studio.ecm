package cj.studio.ecm.frame;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

public interface IFlowContent {

	public abstract ByteBuf raw();

	public abstract void release();

	public abstract int maxWritableBytes();

	public abstract void writeBytes(byte[] b);
	void readerIndex(int index);
	int readerIndex();
	public abstract void writeBytes(byte[] b, int pos, int len);

	public abstract void writeBytes(ByteBuf b, int pos, int len);

	public abstract void writeBytes(ByteBuf b, int pos);

	public abstract void writeBytes(ByteBuf b);

	public abstract void writeBytes(ByteBuffer b);

	public abstract void readBytes(byte[] b);

	public abstract void readBytes(byte[] b, int pos, int len);

	public abstract int readableBytes();

	public abstract int refCnt();

	public abstract ByteBuf slice();

	public abstract int capacity();

	public abstract boolean isDirect();

	public abstract ByteBuf slice(int arg0, int arg1);

	public abstract ByteBuf copy(int arg0, int arg1);

	public abstract ByteBuf copy();

	public abstract IFlowContent copyIt();

	public abstract byte[] array();

	public abstract void clear();

	public abstract ByteBuffer nioBuffers();

	public abstract byte[] readFully();

	public abstract void writeBytes(IFlowContent content);

}