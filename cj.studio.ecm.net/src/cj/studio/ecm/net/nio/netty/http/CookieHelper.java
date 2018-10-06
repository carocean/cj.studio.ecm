package cj.studio.ecm.net.nio.netty.http;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.ServerCookieEncoder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.ultimate.util.StringUtil;

public final class CookieHelper {
	public static Set<Cookie> cookies(Frame frame) {
		String cookieString = frame.head(HttpHeaders.Names.COOKIE.toString());
		if (StringUtil.isEmpty(cookieString))
			return null;
		Set<Cookie> cookies = CookieDecoder.decode(cookieString);
		return cookies;
	}

	public static Set<Cookie> decode(String cookieString) {
		Set<Cookie> set = CookieDecoder.decode(cookieString);
		if (set == null)
			return new HashSet<Cookie>();
		return set;
	}

	public static List<String> encode(Set<Cookie> set) {
		return ServerCookieEncoder.encode(set);
	}

	public static String encodeToString(Set<Cookie> set) {
		String cookieString = "";
		for (Cookie c : set) {
			cookieString += ServerCookieEncoder.encode(c) + ";";
		}
		return cookieString;
	}

	public static Set<Cookie> cookies(Circuit circuit) {
		String cookieString = circuit
				.head(HttpHeaders.Names.SET_COOKIE.toString());
		if (StringUtil.isEmpty(cookieString))
			return null;
		Set<Cookie> cookies = CookieDecoder.decode(cookieString);
		return cookies;
	}
	public static Set<Cookie> cookiesSet(Frame backFrame) {
		String cookieString = backFrame
				.head(HttpHeaders.Names.SET_COOKIE.toString());
		if (StringUtil.isEmpty(cookieString))
			return null;
		Set<Cookie> cookies = CookieDecoder.decode(cookieString);
		return cookies;
	}
	public static String cjtokenSet(Frame backFrame) {
		String cjtoken = "";
		Set<Cookie> set = CookieHelper.cookiesSet(backFrame);
		if (set == null || set.isEmpty()) {
			return cjtoken;
		} else {
			for (Cookie c : set) {
				if ("CJTOKEN".equals(c.getName())) {
					cjtoken = c.getValue();
					break;
				}
			}

			return cjtoken;
		}

	}
	public static String cjtoken(Circuit cir) {
		String cjtoken = "";
		Set<Cookie> set = CookieHelper.cookies(cir);
		if (set == null || set.isEmpty()) {
			return cjtoken;
		} else {
			for (Cookie c : set) {
				if ("CJTOKEN".equals(c.getName())) {
					cjtoken = c.getValue();
					break;
				}
			}

			return cjtoken;
		}

	}

	public static String cjtoken(Frame frame) {
		String cjtoken = "";
		Set<Cookie> set = CookieHelper.cookies(frame);
		if (set == null || set.isEmpty()) {
			return cjtoken;
		} else {
			for (Cookie c : set) {
				if ("CJTOKEN".equals(c.getName())) {
					cjtoken = c.getValue();
					break;
				}
			}

			return cjtoken;
		}

	}

	public static String cookieString(Frame frame) {
		return frame.head(HttpHeaders.Names.COOKIE.toString());
	}

	public static void cookieString(Frame frame, String str) {
		frame.head(HttpHeaders.Names.COOKIE.toString(), str);
	}

	public static String cookieString(Circuit cir) {
		return cir.head(HttpHeaders.Names.SET_COOKIE.toString());
	}

	public static void removeCookie(Circuit circuit, String key) {
		Set<Cookie> set = cookies(circuit);
		if (set == null) {
			set = new HashSet<Cookie>();
		}
		Cookie found = null;
		for (Cookie c : set) {
			if (c.getName().equals(key)) {
				found = c;
				break;
			}
		}
		if (found != null) {
			found.setDiscard(true);
			found.setMaxAge(0);
		} else {
			Cookie c = new DefaultCookie(key, "");
			c.setDiscard(true);
			c.setMaxAge(0);
			set.add(c);
		}
		String cookieString = "";
		for (Cookie c : set) {
			cookieString += ServerCookieEncoder.encode(c) + ";";
		}
		circuit.head(HttpHeaders.Names.SET_COOKIE.toString(), cookieString);
	}

	public static void appendCookie(Circuit circuit, String key, String v) {
		appendCookie(circuit, key, v, -1);
	}

	
	 /**
     * Sets the maximum age of this {@link Cookie} in seconds.
     * If an age of {@code 0} is specified, this {@link Cookie} will be
     * automatically removed by browser because it will expire immediately.
     * If {@link Long#MIN_VALUE} is specified, this {@link Cookie} will be removed when the
     * browser is closed.
     *
     * @param maxAge The maximum age of this {@link Cookie} in seconds
     */
	public static void appendCookie(Circuit circuit, String key, String v,
			long maxAge) {
		Set<Cookie> set = cookies(circuit);
		boolean exists = false;
		String cookieString = "";
		if (set == null) {
			set = new HashSet<Cookie>();
		}
		for (Cookie c : set) {
			if (c.getName().equals(key)) {
				c.setValue(v);
				if (maxAge < 0){
					maxAge=Long.MIN_VALUE;
				}
				c.setMaxAge(maxAge);
				if (StringUtil.isEmpty(c.getPath()))
					c.setPath("/");//路径决定了cookie的共享区间，/表示站点的所有资源均共享同一cookie，如果以资源路径设为cookie路径，则各个资源均有独自的cookie，这会导致请求一个页面时，页面内的资源各自产生新的会话，因此必须设定此值
				exists = true;
				break;
			}
		}

		if (!exists) {
			Cookie c = new DefaultCookie(key, v);
//			c.setDomain("localhost");
			if (maxAge < 0){
				maxAge=Long.MIN_VALUE;
			}
			c.setMaxAge(maxAge);
			if (StringUtil.isEmpty(c.getPath()))
				c.setPath("/");//路径决定了cookie的共享区间，/表示站点的所有资源均共享同一cookie，如果以资源路径设为cookie路径，则各个资源均有独自的cookie，这会导致请求一个页面时，页面内的资源各自产生新的会话，因此必须设定此值
			set.add(c);
//			Cookie c2 = new DefaultCookie("key", v);//永久保持此会话，用于在cjtoken过期后在服务器端判断并退出session，这一招还是在实在没办法时再这么做吧
//			if (maxAge < 0){
//				maxAge=Long.MIN_VALUE;
//			}
//			c2.setMaxAge(maxAge);
//			if (StringUtil.isEmpty(c2.getPath()))
//				c2.setPath("/");//路径决定了cookie的共享区间，/表示站点的所有资源均共享同一cookie，如果以资源路径设为cookie路径，则各个资源均有独自的cookie，这会导致请求一个页面时，页面内的资源各自产生新的会话，因此必须设定此值
//			set.add(c2);
		}
		for (Cookie c : set) {
			cookieString += ServerCookieEncoder.encode(c) + ";";
		}
		circuit.head(HttpHeaders.Names.SET_COOKIE.toString(), cookieString);
	}

	public static void main(String... key) {
		System.out.println(Long.MIN_VALUE);
		long l = System.currentTimeMillis();
		Date d = new Date(l + 1800 * 1000);
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		String s = sdf.format(d);
		System.out.println(s);
		System.out.println(sdf.getCalendar().getTime());
		Cookie c = new DefaultCookie("ss", "do");
		c.setMaxAge(18000);
		String tmp = ServerCookieEncoder.encode(c);
		System.out.println(tmp);
	}

	public static void appendCookie(Frame frame, String key, String v,
			long maxAge) {
		Set<Cookie> set = cookies(frame);
		boolean exists = false;
		String cookieString = "";
		if (set == null) {
			set = new HashSet<Cookie>();
		}
		for (Cookie c : set) {
			if (c.getName().equals(key)) {
				c.setValue(v);
				if (maxAge > -1)
					c.setMaxAge(maxAge);
				exists = true;
				break;
			}
		}

		if (!exists) {
			Cookie c = new DefaultCookie(key, v);
			if (maxAge > -1)
				c.setMaxAge(maxAge);
			set.add(c);
		}
		for (Cookie c : set) {
			cookieString += ServerCookieEncoder.encode(c) + ";";
		}
		frame.head(HttpHeaders.Names.SET_COOKIE.toString(), cookieString);
	}
}
