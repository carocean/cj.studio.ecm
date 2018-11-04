package cj.studio.ecm.net.layer;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.nio.netty.http.CookieHelper;
import cj.studio.ecm.net.web.HttpFrame;
import cj.studio.ecm.net.web.SiteConfig;
import cj.ultimate.util.StringUtil;

/**
 * 拦截通讯添加会话层。
 * 
 * <pre>
 * netsite有会话层和路由层两层，路由层在会话层之上
 * </pre>
 * 
 * @author carocean
 *
 */
public class SessionLayerCheckSink implements ISink {
	ISessionManager sessionManager;
	ILogging logger;
	public SessionLayerCheckSink(ISessionManager sm) {
		sessionManager = sm;
		logger=CJSystem.current().environment().logging();
	}

	@Override
	public void flow(Frame frame,Circuit circuit, IPlug plug) throws CircuitException {
		if (frame.protocol().equals("NET/1.1")) {
			if ("disconnect".equals(frame.command())) {
			}
			return;
		}
		SiteConfig wc = (SiteConfig) plug
				.optionsGraph(SiteConfig.WEBCONFIG_KEY);
		// 每次请求均检查会话，会话即令牌
		doRequestSession(frame, circuit, wc);
		checkSession(frame,circuit);
		plug.flow(frame, circuit);
	}
	
	private void checkSession(Frame frame,Circuit circuit) throws CircuitException {
		boolean nullSession = false;
		if (frame instanceof HttpFrame) {
			HttpFrame f = (HttpFrame) frame;
			if (f.session() == null)
				nullSession = true;
		} else {
			if (circuit.attribute("session") == null) {
				nullSession = true;
			}
		}
		if (nullSession) {
			throw NetConstans.newCircuitException(NetConstans.STATUS_560);
		}
	}

	protected void doRequestSession(Frame frame, Circuit circuit,
			SiteConfig conf) {
		// 由于http不能主动向客户端输出，而ws才能，因此会话层仅保持ws会话的selectId

		// 如果不存在会话cookie则说明要非兼容http请求，第一个请求来了就是要请求握手，
		// 则要发起重定向使其下次再握，因为如果不发起重定向，客户端流浪器无法接收到令牌，
		// 而是通过websocket写回，或者客户端用js实现写入接收到的令牌也行

		// 本方案采用在不存在会话cookie则说明要非兼容http请求情况下
		// 由客户端js通过websocket管理令牌，为了节省流量。
		String cjtoken = CookieHelper.cjtoken(frame);
		ISession session = null;
		if (StringUtil.isEmpty(cjtoken)) {
			SessionInfo info = new SessionInfo(
					(String) circuit.attribute("select-simple"),
					(String) circuit.attribute("select-name"),
					(String) circuit.attribute("remote-address"),
					(String) circuit.attribute("local-address"));
			session = sessionManager.genSession(info);
			long expire = sessionManager.expire((String) circuit
					.attribute("select-simple"));
//			logger.info(getClass(),"客户端cookie失效，因此产生新session");
		} else {
			session = sessionManager.get(cjtoken);
			if (session == null) {// 这种情况，比如服务器重启了。
				SessionInfo info = new SessionInfo(
						(String) circuit.attribute("select-simple"),
						(String) circuit.attribute("select-name"),
						(String) circuit.attribute("remote-address"),
						(String) circuit.attribute("local-address"));
				session = sessionManager.genSession(info);
				long expire = sessionManager.expire((String) circuit
						.attribute("select-simple"));
				CookieHelper.appendCookie(circuit, "CJTOKEN", session.sid(),
						expire);// 覆盖客户端令牌
//				logger.info(getClass(),"服务器端会话过期，因此产生新session");
			}
		}
		// if (isWebSocketReq(frame)) {
		// session.lastVisitTime(Long.MAX_VALUE);// 会话为无限期
		// }
		String sid = (String) circuit.attribute("select-id");

		//在前面已添加，而且此代码未考虑使用tcp,udt其它协议连website的情况
//		if (!"http".equals(circuit.attribute("select-simple"))
//				&& !session.exists(sid)) {
//			session.add(sid);
//		}
		if (frame instanceof HttpFrame) {
			HttpFrame f = (HttpFrame) frame;
			f.setSession(session);
		} else {
			circuit.attribute("session", session);
		}
	}
}
