package cj.studio.ecm.net.graph;

import io.netty.handler.codec.http.HttpHeaders;

import java.util.HashMap;
import java.util.Map;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.nio.netty.http.CookieHelper;

/**
 * 每个输出端子上的cookie容器，类似于浏览器。
 * 
 * <pre>
 * 检查回路中如有SetCookie，则为记录它，以域作为key
 * 如果没有，则在容器中按当前frame的请求地址的域在容器中查找，如果发现有cookie，则附加到frame上Cookie。
 * 
 * 所以它只作用于output端子上。
 * </pre>
 * 
 * @author carocean
 *
 */
public class CookieContainer implements ISink {
	
	private Map<String, String> cookies;// key为域,v为cookie串

	public CookieContainer() {
		cookies = new HashMap<String, String>();
	}

	@Override
	public void flow(Frame frame,Circuit circuit, IPlug plug) throws CircuitException {
		String domain = frame.rootPath();
		if (cookies.containsKey(domain)) {
			String theCookie = "";
			theCookie = cookies.get(domain);
//			System.out.println(plug.site().getProperty("$.graph.name")
//					+ " the " + theCookie);
			CookieHelper.cookieString(frame, theCookie);
		}
		try {
			plug.flow(frame, circuit);
		} catch (Exception e) {
			throw e;
		} finally {
			if (circuit.containsHead(HttpHeaders.Names.SET_COOKIE.toString())) {
				String cookie = CookieHelper.cookieString(circuit);
				cookies.put(domain, cookie);
//				System.out.println(plug.site().getProperty("$.graph.name")
//						+ " set " + cookie);
			}
		}
	}
}
