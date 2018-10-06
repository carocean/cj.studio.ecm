package cj.studio.ecm.net.web;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.net.nio.netty.http.CookieHelper;
import io.netty.handler.codec.http.HttpHeaders;

public class HttpCircuit extends Circuit {

	public HttpCircuit(String frame_line) {
		super(frame_line);
		// TODO Auto-generated constructor stub
	}

	public void appendCookie(String key, String v) {
		CookieHelper.appendCookie(this, key, v);
	}

	public void removeCookie(String key) {
		CookieHelper.removeCookie(this, key);
	}

	/**
	 * 返回当站网站的根路径
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @return
	 */
	public String httpSiteRoot() {
		return (String) attribute("http.root");
	}

	public void setChunked() {
		head(HttpHeaders.Names.TRANSFER_ENCODING.toString(),
				HttpHeaders.Values.CHUNKED.toString());
	}
}
