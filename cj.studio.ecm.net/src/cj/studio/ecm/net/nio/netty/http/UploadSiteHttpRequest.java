package cj.studio.ecm.net.nio.netty.http;

import io.netty.buffer.CompositeByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

public class UploadSiteHttpRequest extends DefaultFullHttpRequest
		implements FullHttpMessage {
	public UploadSiteHttpRequest(HttpVersion protocolVersion,
			HttpMethod method, String uri, CompositeByteBuf compositeBuffer) {
		super(protocolVersion, method, uri, compositeBuffer);
		msgNum(0);
	}
	public long msgNum(){
		return Long.valueOf(super.headers().get("Is-Upload-Chunked-Num"));
	}
	public void msgNum(long num){
		super.headers().set("Is-Upload-Chunked-Num",num);
	}
}
