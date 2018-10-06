package cj.studio.ecm.net.layer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.web.SiteConfig;
import cj.ultimate.util.StringUtil;

/**
 * 基础会话管理器。 提供内存会话管理。
 * 
 * <pre>
 * 一个netsite管理具有统一的会话管理，统一为其内的服务器请求发放令牌
 * 
 * 提供对上层管理器的支持，上层管理器可能实现为：分布式会话、各种持久化、默认的会话
 * </pre>
 * 
 * @author carocean
 *
 */
public class SessionManager implements ISessionManager {
	private Map<String, ISession> sessions;// key=sessionId;
	private List<ISessionEvent> events;
	private SiteConfig sc;
	private ITokenDistributionStrategy cjtokenDistributionStrategy;
	private String name;
	private String id;

	public SessionManager() {
		sessions = new ConcurrentHashMap<String, ISession>();
		events = new ArrayList<ISessionEvent>(2);
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}

	// @Override
	// public void id(String key) {
	// this.id = key;
	// }

	public String id() {
		return id;
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		this.name = name;
	}

	@Override
	public void dispose() {
		sessions.clear();
		events.clear();
	}

	@Override
	public void siteConfig(SiteConfig wc) {
		sc = wc;
	}

	@Override
	public void setCjTokenDistributionStrategy(
			ITokenDistributionStrategy strategy) {
		cjtokenDistributionStrategy = strategy;
	}

	@Override
	public boolean exists(String sessionId) {
		doExpire(/* session */);
		return sessions.containsKey(sessionId);
	}

	@Override
	public void removeSelectId(String netSourceName, String localAddress,
			String selid) {
		for (String key : sessions.keySet()) {
			ISession s = sessions.get(key);
			if (netSourceName.equals(s.info().getNetSourceName())) {
				s.remove(selid);
			}
		}
	}

	@Override
	public ISession get(String sessionId) {
		try {
			checkLegal(sessionId);
		} catch (CircuitException e) {
			throw new RuntimeException(e);
		}
		ISession session = sessions.get(sessionId);
		if (session == null)
			return null;
		if (checkExpire(session)) {
			doExpire(/* session */);// 不论如何，每次均要检查所有的过期
			// session = genSession();
		}
		// if (session.lastVisitTime() < System.currentTimeMillis())
		session.lastVisitTime(System.currentTimeMillis());
		return session;
	}

	protected void doExpire(/* ISession session */) {
		String[] keys = sessions.keySet().toArray(new String[0]);
		for (String key : keys) {
			ISession s = sessions.get(key);
			if (s == null)
				continue;
			if (!checkExpire(s)) {
				continue;
			}
			for (ISessionEvent e : events) {
				s.createTime(-1);
				s.lastVisitTime(-1);
				e.doEvent("removeSession", s);
			}
			sessions.remove(key);
		}
	}

	protected boolean checkExpire(ISession session) {
		if (session.createTime() == -1 || session.lastVisitTime() == -1)
			return true;
		long expire = this.sc
				.getSessionTimeout(session.info().getNetSourceSimple());
		// expire依赖于netty对cookie的管理，见：cookiehelper.appendCookie方法的注释，意思为：
		// 当为Long.min_value时在浏览器退出时cookie过期，因此在浏览器退出时，服务器端会话也得将会话移除
		// 当为0时浏览器的cookie立即过期，因此服务器端的会话得立即移除
		//永不过期的情况是设一个Long.Max_value，这个值在有生之年是过期不了，因此等同于永不过期
		if (expire == 0)// 立即过期
			return true;
		// 浏览器退出时过期，因此要捕捉浏览器是否退出
		//（注意：如果浏览器不是keep-live模式，请求完后所有连接均断开，
		// 因此捕获select-id集合是否为空是不行的，因此浏览器并未关闭，
		// 特别是通过http-tcp-website转来转去时）
		
		//但是，除此之外别无它法，服务端的过期还是按select-id是否为空判断过期
		//http://blog.csdn.net/lgj1025/article/details/8148853
		//这是tomcat的会话过期策略：http://blog.csdn.net/bingjing12345/article/details/20693631,基本上都是后台线程扫描
		//http://zddava.iteye.com/blog/311136
		//http://liudeh-009.iteye.com/blog/1584876
		//由此可见tomcat是每1分钟检查一次session
		if (expire < 0){
			expire=1800;//当为负时：cookie是当浏览器关闭时过期，但服务器无法判断，暂时给它30分钟,之后再找方案.
//			return !session.isOnline();//这是不对的，对于keep-live可以，非保持连接的情况会话的select-id集合一定是空的，但浏览器仍在用
		}
		long v = System.currentTimeMillis() - session.lastVisitTime();
		if (v / 1000 >= expire) {
			return true;
		}
		return false;
	}

	/**
	 * 检询sessionid是否合法。
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param sessionId
	 * @throws CircuitException
	 */
	protected void checkLegal(String sessionId) throws CircuitException {
		if (!sessionId.equals(sessionId)) {
			throw NetConstans.newCircuitException(NetConstans.STATUS_561);
		}
	}

	/**
	 * 客户端限制，限制远程同一地址和端口仅能申请一个会话。
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param info
	 * @return
	 */
	protected String existsRemote(SessionInfo info) {
		for (String key : sessions.keySet()) {
			ISession s = sessions.get(key);
			// if (s.info().getNetRemoteAddress()
			// .equals(info.getNetRemoteAddress())) {
			// return key;
			// }
		}
		return null;
	}

	@Override
	public ISession genSession(SessionInfo info) {
		String existsSid = existsRemote(info);
		if (!StringUtil.isEmpty(existsSid)) {
			throw new EcmException(String.format(
					"同一客户端只能申请一个会话。sessionManager：%s,已存在会话:%s.客户端地址：%s", name,
					existsSid, info.getNetRemoteAddress()));
		}
		String cjtoken = cjtokenDistributionStrategy.genToken(info);
		ISession session = createSession(cjtoken, info, events);
		sessions.put(cjtoken, session);
		session.createTime(System.currentTimeMillis());
		long expire = this.sc
				.getSessionTimeout(session.info().getNetSourceSimple());
		if (expire <0) {
			session.lastVisitTime(Long.MAX_VALUE);
		} else {
			session.lastVisitTime(System.currentTimeMillis());
		}
		for (ISessionEvent e : events) {
			e.doEvent("addSession", session);
		}
		return session;
	}

	protected ISession createSession(String sid, SessionInfo info,
			List<ISessionEvent> events) {
		ISession session = new Session(sid, info, events);
		return session;
	}

	@Override
	public void addEvent(ISessionEvent event) {
		events.add(event);

	}

	@Override
	public void removeEvent(ISessionEvent event) {
		events.remove(event);

	}
	@Override
	public long expire(String selectSimple) {
		return sc.getSessionTimeout(selectSimple);
	}
}
