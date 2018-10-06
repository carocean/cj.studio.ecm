package cj.studio.ecm.frame;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import cj.ultimate.gson2.com.google.gson.Gson;

public class TcpFrame extends Frame{
	
	private long length;
	private byte protocolType;
	private IBytesWriter writer;
	private IBytesReader reader;
	public TcpFrame() {
	}
	public void readFrom(IBytesReader r){
		reader=r;
		length=r.length();
	}
	public void writeTo(IBytesWriter w){
		writer=w;
		writer.setLength(length);
	}
	
	public byte[] readHeader(){
		if(headmap.isEmpty())return new byte[0];
		Gson gson=new Gson();
		String json=gson.toJson(headmap);
//		System.out.println(json);
		try {
			return json.getBytes("utf-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	public static void main(String...strings){
		TcpFrame f=new TcpFrame();
		f.head("ddd", "sss");
		System.out.println(f.readHeader().length);
		f.writeHeader(f.readHeader());
	}
	@SuppressWarnings("unchecked")
	public void writeHeader(byte[] b){
		try {
			String json=new String(b,"utf-8");
			Gson g=new Gson();
			headmap=g.fromJson(json, HashMap.class);
//			System.out.println(header.get("ddd"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	// 支持流式消息，可作为流的触发器，拉数据
	/**
	 * 如果返回－1表示结束
	 * @param b
	 * @param i
	 * @param len
	 * @return
	 */
	public int read(byte[] b, int i, long len) {
		return reader.read(b, i, len);
	}

	public void readFully(byte[] frame, int i, long len) {
		if(reader==null)throw new RuntimeException("侦读取的消息为空");
		reader.readFully(frame, i, len);
	}

	// 支持流式消息，可作为流的触发器，推数据
	public void write(byte[] b, int i, int length) {
		if(writer==null)throw new RuntimeException("侦输出的消息为空");
		writer.write(b, i, length);
	}

	public long getLength() {
		return length;
	}

	public void setLength(long length) {
		this.length = length;
	}

	public byte getProtocolType() {
		return protocolType;
	}

	public void setProtocolType(byte protocolType) {
		this.protocolType = protocolType;
	}
}
