package cj.studio.ecm.frame;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class FieldData {
	byte[] data;
	String disposition;
	String transferEncoding;
	String type;
	private FormData childForm;
	static Pattern fielname = Pattern
			.compile("filename\\s*=\\s*\"([.[^;=]]*)\"");
	static Pattern name = Pattern.compile("name\\s*=\\s*\"([.[^;=]]*)\"");
	static Pattern bound = Pattern.compile("boundary\\s*=\\s*([.[^;=]]*)");

	public String getName() {
		Matcher m = name.matcher(disposition);
		if (m.find()) {
			return m.group(1);
		} // 如果不存在name则可能是文件则

		m = fielname.matcher(disposition);
		if (m.find()) {
			return m.group(1);
		}
		return "";// 从disposition解析
	}

	/**
	 * 如果是混合表单则包含子表单
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @return
	 */
	public boolean isMixed() {
		return !StringUtil.isEmpty(type) && type.contains("multipart/mixed");
	}

	/**
	 * 如果不存在子边界则返回为空
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @return
	 */
	public FormData childForm() {
		return childForm;
	}

	public boolean isFile() {
		return fielname.matcher(disposition).find()
				&& !StringUtil.isEmpty(type);
	}

	public String filename() {
		Matcher m = fielname.matcher(disposition);
		if (m.find()) {
			return m.group(1);
		}
		return "";
	}

	public void data(byte[] data) {
		if(isMixed()){
		System.out.println("+++++++++++++");
		System.out.println(new String (data));
		System.out.println("+++++++++++++");
		}
		if (!StringUtil.isEmpty(type)) {
			Matcher m = bound.matcher(type);
			if (m.find()) {
				String boundary = m.group(1);
				Boundary bd = new Boundary(String.format("--%s", boundary));
				/*
				 * 格式：数据\r\n分隔串下一段分隔串再下一段
				 * 
				 * 最前的数据是当前字段域的数据，后面的段是子表单的。
				 */
				ByteBuf curr=Unpooled.buffer();
				for(int i=0;i<data.length;i++){
					int d=bd.write(data[i]);
					if(d>-1){
						if (bd.cache.readableBytes() > 0) {
							curr.writeBytes(bd.cache);
						}
						curr.writeByte((byte) d);
					}else if(d==-2){
						break;
					}
				}
				bd.reset();
				int len=curr.readableBytes();
				if(len>0&&curr.getByte(len-2)=='\r'&&curr.getByte(len-1)=='\n'){
					len=len-2;
				}
				byte[] currData=new byte[len];
				curr.readBytes(currData);
				byte[] newCurr=currData;
				if(currData[currData.length-1]==10&&currData[currData.length-2]==13){
					newCurr=new byte[currData.length-2];
					System.arraycopy(currData, 0, newCurr, 0, newCurr.length);
				}
				byte[] childData=new byte[data.length-currData.length];
				System.arraycopy(data, currData.length, childData, 0, childData.length);
				this.data=currData;
				childForm = new FormData();
				for (int i = 0; i < childData.length; i++) {
					childForm.write(childData[i], bd);
				}
				return;
			}
		}
		this.data = data;
	}

	public byte[] data() {
		return data;
	}

	public void setDisposition(String disposition) {
		this.disposition = disposition;
	}

	public String getDisposition() {
		return disposition;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public String getTransferEncoding() {
		return transferEncoding;
	}

	public void setTransferEncoding(String transferEncoding) {
		this.transferEncoding = transferEncoding;
	}

	public void parseKeyPair(byte[] bh) {
		String kp = new String(bh);
//		try {
//			kp = URLDecoder.decode(kp, "utf-8");
//		} catch (UnsupportedEncodingException e) {
//		}
		if (kp.startsWith("Content-Disposition")) {
			this.disposition = kp.substring(kp.indexOf(":") + 1, kp.length());
			return;
		}
		if (kp.startsWith("Content-Type")) {
			this.type = kp.substring(kp.indexOf(":") + 1, kp.length());
			return;
		}
		if (kp.startsWith("Content-Transfer-Encoding")) {
			this.transferEncoding = kp.substring(kp.indexOf(":") + 1,
					kp.length());
		}
	}
}
