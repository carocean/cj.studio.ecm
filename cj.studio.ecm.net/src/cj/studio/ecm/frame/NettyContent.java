package cj.studio.ecm.frame;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

 class NettyContent implements IFlowContent {
	private ByteBuf buf;
	public NettyContent(ByteBuf buf) {
		this.buf=buf;
	}
	@Override
	public void writeBytes(IFlowContent content) {
		buf.writeBytes(content.raw());
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#raw()
	 */
	@Override
	public ByteBuf raw(){
		return buf;
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#release()
	 */
	@Override
	public void release() {
		buf.release();
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#maxWritableBytes()
	 */
	@Override
	public int maxWritableBytes(){
		return buf.maxWritableBytes();
	}
	public int readerIndex(){
		return buf.readerIndex();
	}
	public void readerIndex(int index){
		buf.readerIndex(index);
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#writeBytes(byte[])
	 */
	@Override
	public void writeBytes(byte[] b){
			
		buf.writeBytes(b);
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#writeBytes(byte[], int, int)
	 */
	@Override
	public void writeBytes(byte[] b,int pos,int len){
		buf.writeBytes(b,pos,len);
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#writeBytes(io.netty.buffer.ByteBuf, int, int)
	 */
	@Override
	public void writeBytes(ByteBuf b,int pos,int len){
		buf.writeBytes(b,pos,len);
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#writeBytes(io.netty.buffer.ByteBuf, int)
	 */
	@Override
	public void writeBytes(ByteBuf b,int pos){
		buf.writeBytes(b,pos);
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#writeBytes(io.netty.buffer.ByteBuf)
	 */
	@Override
	public void writeBytes(ByteBuf b){
		buf.writeBytes(b);
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#writeBytes(java.nio.ByteBuffer)
	 */
	@Override
	public void writeBytes(ByteBuffer b){
		buf.writeBytes(b);
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#readBytes(byte[])
	 */
	@Override
	public void readBytes(byte[] b){
		buf.readBytes(b);
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#readBytes(byte[], int, int)
	 */
	@Override
	public void readBytes(byte[] b,int pos,int len){
		buf.readBytes(b,pos,len);
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#readableBytes()
	 */
	@Override
	public int readableBytes(){
		return buf.readableBytes();
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#refCnt()
	 */
	@Override
	public int refCnt(){
		return buf.refCnt();
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#slice()
	 */
	@Override
	public ByteBuf slice(){
		return buf.slice();
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#capacity()
	 */
	@Override
	public int capacity(){
		return buf.capacity();
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#isDirect()
	 */
	@Override
	public boolean isDirect(){
		return buf.isDirect();
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#slice(int, int)
	 */
	@Override
	public ByteBuf slice(int arg0,int arg1){
		return buf.slice(arg0, arg1);
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#copy(int, int)
	 */
	@Override
	public ByteBuf copy(int arg0,int arg1){
		return buf.copy(arg0, arg1);
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#copy()
	 */
	@Override
	public ByteBuf copy(){
		return buf.copy();
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#copyIt()
	 */
	@Override
	public NettyContent copyIt(){
		NettyContent c=new NettyContent(buf.copy());
		return c;
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#array()
	 */
	@Override
	public byte[] array(){
		return buf.array();
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#clear()
	 */
	@Override
	public void clear(){
		 buf.clear();
	}
	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#nioBuffers()
	 */
	@Override
	public ByteBuffer nioBuffers(){
		 return buf.nioBuffer();
	}

	/* (non-Javadoc)
	 * @see cj.studio.ecm.frame.IFlowContent#readFully()
	 */
	@Override
	public byte[] readFully(){
		byte[] b=new byte[buf.readableBytes()
		                  ];
		buf.readBytes(b);
		return b;
	}
}
