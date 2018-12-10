package cj.studio.ecm.net.layer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Session implements ISession {
	private List<ISessionEvent> events;
	private String sessionId;
	private Map<String, Serializable> attmap;
	private List<String> selectIds;
	private long createTime;
	private long lastVisitTime;
	private SessionInfo info;
	public Session(String sid,SessionInfo info, List<ISessionEvent> events) {
		sessionId = sid;
		attmap = new HashMap<String, Serializable>(4);
		selectIds = new ArrayList<String>(4);
		this.events=events;
		this.info=info;
	}
	
	@Override
	public SessionInfo info() {
		// TODO Auto-generated method stub
		return info;
	}
	@Override
	public boolean isOnline() {
//		if("http".equals(info.getNetSourceSimple())){
//			return true;
//		}
		return !selectIds.isEmpty();
	}
	@Override
	public boolean exists(String selectId) {
		// TODO Auto-generated method stub
		return selectIds.contains(selectId);
	}
	@Override
	public String selectId(int i) {
		// TODO Auto-generated method stub
		return selectIds.get(i);
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
		for(ISessionEvent e:events){
			e.doEvent("attributeRemoved", this,key);
		}
	}
	@Override
	public void attribute(String key, Serializable value) {
		attmap.put(key, value);
		for(ISessionEvent e:events){
			e.doEvent("attributeAdd", this,key,value);
		}
	}

	@Override
	public String id() {
		return sessionId;
	}

	@Override
	public String[] enumSelectIds() {
		return selectIds.toArray(new String[0]);
	}

	@Override
	public void add(String selectId) {
		if(selectIds.contains(selectId))return;
		selectIds.add(selectId);
		if(!events.isEmpty()){
			for(ISessionEvent e:events){
				e.doEvent("selectAdd",this,selectId);
			}
		}
	}

	@Override
	public void remove(String selectId) {
		if(!selectIds.contains(selectId))return;
		selectIds.remove(selectId);
		if(!events.isEmpty()){
			for(ISessionEvent e:events){
				e.doEvent("selectRemove",this,selectId);
				if(selectIds.isEmpty()&&!"http".equals(info.netSourceSimple)){
					e.doEvent("offline",this, selectId);
				}
			}
		}
	}

}