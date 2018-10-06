package cj.studio.ecm.util;
//可伸展的字节数组
public class ExtensibleBytesArray {
	private byte[] buf;
	//写的位置
	private int writepos;
	private int extendTimesLen;
	private int ultimateLen;
	//读的位置
	private int readpos;
	//初始数组长度，每次伸展量
	public ExtensibleBytesArray(int ultimateLen, int extendTimesLen) {
		this.ultimateLen=ultimateLen;
		buf=new byte[ultimateLen];
		this.extendTimesLen=extendTimesLen;
	}
	public void put(byte[] b, int offset, int len) {
		int totle=writepos+(len-offset);
		if(totle>buf.length){
			byte[] newbuf=new byte[totle+extendTimesLen];
			System.arraycopy(buf, 0, newbuf, 0, writepos);
			buf=newbuf;
		}
		System.arraycopy(b, offset, buf, writepos, len);
		writepos+=len-offset;
	}
	public void put(byte b){
		int totle=writepos+1;
		if(totle>buf.length){
			byte[] newbuf=new byte[totle+extendTimesLen];
			System.arraycopy(buf, 0, newbuf, 0, writepos);
			buf=newbuf;
		}
		buf[writepos]=b;
		writepos+=1;
	}
	public void update(byte b,int pos){
		buf[pos]=b;
	}
	public byte[] toBytesArray() {
		byte[] newbuf=new byte[writepos];
		System.arraycopy(buf, 0, newbuf, 0, writepos);
		return newbuf;
	}
	public int getSize(){
		return writepos;
	}

	public void clear() {
		// TODO Auto-generated method stub
		buf=new byte[ultimateLen];
		writepos=0;
		readpos=0;
	}

	public int get(int index) {
		// TODO Auto-generated method stub
		return buf[index];
	}
	public void readReset(){
		readpos=0;
	}
	public boolean hasNext(){
		return readpos<getSize()?true:false;
	}
	public byte getNextByte() {
		byte b= buf[readpos];
		readpos++;
		return b;
	}
	/**
	 * 如果缓冲读完则返回-1
	 * @return
	 */
	public int getNextInt() {
		int b= buf[readpos]&0xFF;
		readpos++;
		return b;
	}
}
