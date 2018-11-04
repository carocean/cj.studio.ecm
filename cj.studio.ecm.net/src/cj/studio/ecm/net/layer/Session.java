package cj.studio.ecm.net.layer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Session implements ISession {
	private List<ISessionEvent> events;
	private String sessionId;
	private Map<String, Serializable> attmap;
	private long createTime;
	private long lastVisitTime;
	private SessionInfo info;
	public Session(String sid,SessionInfo info, List<ISessionEvent> events) {
		sessionId = sid;
		attmap = new HashMap<String, Serializable>(4);
		this.events=events;
		this.info=info;
	}
	
	@Override
	public SessionInfo info() {
		// TODO Auto-generated method stub
		return info;
	}
	
	

	@Override
	public long createTime() {
		return createTime;
	}

	@Override
	public long lastVisitTime() {
		return lastVisitTime;
	}

	@Override
	public void createTime(long time) {
		this.createTime = time;
	}

	@Override
	public void lastVisitTime(long time) {
		this.lastVisitTime = time;
	}

	@Override
	public Object attribute(String key) {
		return attmap.get(key);
	}
	@Override
	public void removeAttribute(String key){
		attmap.remove(key);
		if(attmap.containsKey(key)) {
			for(ISessionEvent e:events){
				e.doEvent("attributeRemoved", this,key);
			}
		}
	}
	@Override
	public void attribute(String key, Serializable value) {
		
		if(attmap.containsKey(key)) {
			Object old=attmap.get(key);
			attmap.put(key, value);
			for(ISessionEvent e:events){
				e.doEvent("attributeReplace", this,key,value,old);
			}
		}else {
			attmap.put(key, value);
			for(ISessionEvent e:events){
				e.doEvent("attributeAdd", this,key,value);
			}
		}
		
	}

	@Override
	public String sid() {
		return sessionId;
	}

	

}