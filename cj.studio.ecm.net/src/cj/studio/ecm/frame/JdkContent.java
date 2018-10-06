package cj.studio.ecm.frame;


import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

 class JdkContent implements IFlowContent{
	private ByteBuffer buf;
	public JdkContent(ByteBuffer buf) {
		this.buf=buf;
	}
	@Override
	public void readerIndex(int index) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public int readerIndex() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public ByteBuf raw() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void release() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public int maxWritableBytes() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void writeBytes(byte[] b) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void writeBytes(byte[] b, int pos, int len) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void writeBytes(ByteBuf b, int pos, int len) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void writeBytes(ByteBuf b, int pos) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void writeBytes(ByteBuf b) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void writeBytes(ByteBuffer b) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void readBytes(byte[] b) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void readBytes(byte[] b, int pos, int len) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public int readableBytes() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int refCnt() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public ByteBuf slice() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public int capacity() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public boolean isDirect() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public ByteBuf slice(int arg0, int arg1) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public ByteBuf copy(int arg0, int arg1) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public ByteBuf copy() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public IFlowContent copyIt() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public byte[] array() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public ByteBuffer nioBuffers() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public byte[] readFully() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void writeBytes(IFlowContent content) {
		// TODO Auto-generated method stub
		
	}
	
}
