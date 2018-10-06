package cj.studio.ecm.net.web;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.WirePin;
import cj.studio.ecm.net.nio.netty.http.CookieHelper;

public class HttpPin extends WirePin {

	public HttpPin() {
		// TODO Auto-generated constructor stub
	}
	/**
	 * 
	 * <pre>
	 *
	 * </pre>
	 * @param sessionId
	 * @param contentType frame/json,frame/bin，如果该参为null，则内容按数据传，就不按侦了
	 * @param frame
	 * @param circuit
	 * @throws CircuitException
	 */
	public void sendBySessionId(String sessionId,String contentType, Frame frame, Circuit circuit) throws CircuitException {
		CookieHelper.appendCookie(frame, "CJTOKEN", sessionId, -1);
		frame.head("Content-Type", contentType);
		flow(frame, circuit);
	}
	/**
	 * 
	 * <pre>
	 * @see content-type 按assembly.properties属性中的Content-Type
	 * </pre>
	 * @param sessionId
	 * @param frame
	 * @param circuit
	 * @throws CircuitException
	 */
	public void sendBySessionId(String sessionId, Frame frame, Circuit circuit) throws CircuitException {
		circuit.attribute("session-id", sessionId);
		CookieHelper.appendCookie(frame, "CJTOKEN", sessionId, -1);
		SiteConfig wc = (SiteConfig) this.optionsGraph(SiteConfig.WEBCONFIG_KEY);
		sendBySessionId(sessionId,wc.responseFrameType(), frame, circuit);
	}
}
