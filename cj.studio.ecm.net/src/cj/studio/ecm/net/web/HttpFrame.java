package cj.studio.ecm.net.web;

import io.netty.handler.codec.http.Cookie;

import java.util.HashSet;
import java.util.Set;

import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.net.layer.ISession;
import cj.studio.ecm.net.nio.netty.http.CookieHelper;

public class HttpFrame extends Frame {
	ISession session;
	
	public HttpFrame(byte[] frameRaw) {
		super(frameRaw);
		// TODO Auto-generated constructor stub
	}

	public HttpFrame(String frame_line) {
		super(frame_line);
		// TODO Auto-generated constructor stub
	}

	public HttpFrame(String frameline, ISession session2) {
		super(frameline);
		this.session=session2;
	}

	public void setSession(ISession session){
		this.session=session;
	}
	public Set<Cookie> cookie(String key){
		Set<Cookie> set=CookieHelper.cookies(this);
		return set==null?new HashSet<Cookie>():set;
	}
	public ISession session(){
		return session;
	}
}
