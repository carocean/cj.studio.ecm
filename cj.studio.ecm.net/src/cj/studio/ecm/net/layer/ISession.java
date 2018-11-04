package cj.studio.ecm.net.layer;

import java.io.Serializable;

/**
 * net会话
 * <pre>
 * 将net层抽象会话层，应用层将通过会话层使用net
 * 
 * 因为充许一个终端可能同时发起多个连接，而此终端仍可代表同一会话，
 * 因此一个会话拥有多个name+selectid对，而一个name+selectid对只能对应一个会话
 * </pre>
 * @author carocean
 *
 */
public interface ISession {
	String sid();
	SessionInfo info();
	
	void createTime(long time);
	void lastVisitTime(long time);
	long createTime();
	long lastVisitTime();
	Object attribute(String key);
	void attribute(String key,Serializable value);
	
	
	void removeAttribute(String key);
	
}
