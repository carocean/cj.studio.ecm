package cj.studio.ecm.net.web;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.ultimate.util.StringUtil;

public class WebUtil {
	/*
	 * 注意：Chrome浏览器对于客户端的#请求不在地址后加#
	 * > 在一个文档页面中，如果包含的资源地址指定是#时，请求到来不能视为文档，因为它是资源，如果未正确判断是资源请求，则会进入文档处理，导致文档被处理多次（请求地址后均有#)
	 * > 方法是WebUtil.documentMatch(frame, plug)方法中识别文档和资源类型，并以accept头信息判断
	 */
	public static synchronized boolean documentMatch(Frame frame, IPlug plug)
			throws CircuitException {

		SiteConfig wc = (SiteConfig) plug
				.optionsGraph(SiteConfig.WEBCONFIG_KEY);
		String ruri = frame.relativePath();
		// String ruri = frame.relativeUrl();
		String fn = ruri.substring(ruri.lastIndexOf("/") + 1, ruri.length());// 求出文件名或目录名
		if (fn.contains("#")) {
			fn = fn.substring(0, fn.lastIndexOf("#"));
		}
		if (!fn.contains(".")) {
			String welcome = wc.getSiteWelcome();
			if (ruri.equals("/") && !StringUtil.isEmpty(welcome)) {// 是首页地址重写
				ruri = welcome;
				if (frame.containsQueryString()) {
					ruri = String.format("%s?%s", ruri, frame.queryString());
				}
				frame.head("url",
						String.format("%s%s", frame.rootPath(), ruri));
			} /*else {
				ruri = String.format("%s%s",(
						ruri.endsWith("/") ? ruri : String.format("%s/",ruri)),
						wc.getInDirDefaultDoc());
				}*/
			return true;// 只要是目录就视为文档，不论是否是首页
		}

		// Referer http://localhost:8500/sos/public/login.html
		// GET /public/listServicews.html?owner=zhaoxb HTTP/1.1
		// 即便通过地址判断是文档，但有一种情况得排除，否则导致文档处理类被执行多次。
		// 比如：<img src="#">这种请求是资源，但服务器收到的是url#格式，
		// 即同一文档发起的资源＃请求好像是文档一样，而且不同的浏览器发起这样的请求连#号都不加
		// 因此，判断是不是源自同一文档的资源只能依据Referer，它表示当前请求源自哪里
		// 这种情况可以不必考虑：重定向当前页Referer相同（如果开发者控制了不陷入死循环，则等同于刷新，要么其Referer为先前路进时的原头网页不变，要么就没Referer）
		// if (matches && frame.containsHead("Referer")) {
		// String Referer = frame.head("Referer");
		// int pos = Referer.indexOf("?");
		// Referer = pos > -1 ? Referer.substring(0, pos) : Referer;
		// if (Referer.endsWith(oldruri)) {
		// return false;
		// }
		// }

		Pattern p = Pattern.compile(String.format("%s$",
				wc.getHttpdocument().replace(".", ".*\\.")));
		Matcher m = p.matcher(fn);
		return m.matches();
	}

	/**
	 * 
	 * <pre>
	 * 注意：无扩展名的视为目录，目录被视为文档，因为如果是资源必有扩展名
	 * </pre>
	 * 
	 * @param path
	 *            请求的资源的url
	 * @param docType
	 *            格式如：.html|.htm|.app
	 * @return
	 */
	public static synchronized boolean documentMatch(String path,
			String docType) {
		String fn = path.substring(path.lastIndexOf("/") + 1, path.length());
		if (!fn.contains(".")) {
			return true;// 是目录则一定是文档
		}
		if (fn.contains("#")) {
			fn = fn.substring(0, fn.lastIndexOf("#"));
		}
		Pattern p = Pattern
				.compile(String.format("%s$", docType.replace(".", ".*\\.")));
		Matcher m = p.matcher(fn);
		return m.matches();
	}

	/**
	 * 将querystring对解析为参数
	 * 
	 * <pre>
	 * 注意：如果js中提交数组，则接收的key即是key[]=xx&key[]=jj，因此将它直接解析为list对象值
	 * </pre>
	 * 
	 * @param data
	 * @return
	 */
	public synchronized static Map<String, Object> parserParam(String data) {
		
		Map<String, Object> map = new HashMap<>();
		String[] kvpair = data.split("&");
		for (String kvStr : kvpair) {
			String[] kv = kvStr.split("=");
			String k = kv[0];
			String v = kv.length > 1 ? kv[1] : "";
			try {
				k = URLDecoder.decode(k, "utf-8");
				v = URLDecoder.decode(v, "utf-8");
			} catch (UnsupportedEncodingException e) {
			}
			if (k.endsWith("[]")) {
				k = k.substring(0, k.length() - 2);
				List<String> list=(List<String>)map.get(k);
				if(list==null){
					list=new ArrayList<>();
					map.put(k, list);
				}
				list.add(v);
			} else {
				map.put(k, v);
			}
		}
		return map;
	}

	public static void main(String... strings) {
		System.out.println(documentMatch("/", ".html|.htm|.app"));
	}
}
