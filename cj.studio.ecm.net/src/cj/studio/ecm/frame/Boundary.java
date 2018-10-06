package cj.studio.ecm.frame;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class Boundary {

	private String boundary;
	ByteBuf cache;
	int boundaryLen;

	public Boundary(String boundary) {
		this.boundary = boundary;
		cache = Unpooled.buffer();
		boundaryLen = boundary.length();
	}

	public String getBoundary() {
		return boundary;
	}

	public int write(byte b) {
		if (((char) b) == boundary.charAt(cache.readableBytes())) {
			cache.writeByte(b);
			if (cache.readableBytes() == boundaryLen) {
				cache.clear();
				if (boundaryLen == 2) {
					boundaryLen = boundary.length();
					return -3;//表示遇到结束符，则是整个formData的结束
				} else {
					boundaryLen = 2;//当发现是分隔符时则再接收2个字符，以判断是域开头还是结束
					return -2;// 表示是分隔符，但不确定是开始还是结束，因此需要再接收两个字节。因为结层是两个--，而每个分隔符前也必是--因此使用boundary再从头判断两个字符即可
				}
			}
			return -1;// 发现是分隔符，所以被分隔符类接收
		} else {
			if (boundaryLen == 2) {//为正数时表示数据，为－4时需要从缓存中取数，而当前字节也从缓存取
				cache.writeByte(b);
				boundaryLen = boundary.length();
				return -4;//确认当前分隔只是域开始，因此表示开始一个域,该数是新域的最前的一个或两个字节
			}
			return b & 0xFF;
		}
	}

	public void reset() {
		this.cache.clear();
		boundaryLen = boundary.length();
	}

}
