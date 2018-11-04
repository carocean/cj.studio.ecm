package cj.studio.ecm.net.layer;

import cj.studio.ecm.net.web.SiteConfig;
import cj.ultimate.IDisposable;

/**
 * net的会话层实现。如http,udt,local,tcp etc.
 * 
 * <pre>
 * 一个graph拥一个会话管理器，这由NetLayerGraphCreator决定
 * 一个会话可拥有多个活动的selectId
 * 
 * 会话过期由cookie控制，服务器端配的过期时间是生成cookie时的过期默认时间，该时间可被修改，以满足不同用户对会话过期的时间要求
 * </pre>
 * 
 * @author carocean
 *
 */
public interface ISessionManager extends IDisposable{
	public long expire(String selectSimple);
	boolean exists(String sessionId);
	ISession get(String sessionId);
	ISession genSession(SessionInfo info);
	
	void addEvent(ISessionEvent event);
	void removeEvent(ISessionEvent event);
	
	void siteConfig(SiteConfig wc);
	public void setCjTokenDistributionStrategy(
			ITokenDistributionStrategy strategy);
	/**
	 * 为图名
	 * <pre>
	 *
	 * </pre>
	 * @param name
	 */
	public void setName(String name);
	String getName();
//	public void id(String key);
	String id();
}
