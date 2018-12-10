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
	public static final int DEFAULT_EXPIRE_TIME=1800;//1800秒
	SessionInfo info();
	/**
	 * 有查询端子视为在线
	 * <pre>
	 * 注：只要是http协议的会话，只要有会话则视为在线
	 * </pre>
	 * @return
	 */
	boolean isOnline();
	void createTime(long time);
	void lastVisitTime(long time);
	long createTime();
	long lastVisitTime();
	Object attribute(String key);
	void attribute(String key,Serializable value);
	String id();
	String[] enumSelectIds();
	String selectId(int i);
	boolean exists(String selectId);
	/**
	 * 
	 * <pre>
	 *
	 * </pre>
	 * @param selectId 如果是客户端名，此值可为空。
	 */
	void add(String selectId);
	void remove(String selectId);
	void removeAttribute(String key);
	
}
