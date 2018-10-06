package cj.studio.ecm.net.layer;

public interface ISessionEvent {
	//会话id生成、添加、移除，会话过期，会话的选择id添加、移除。
	void doEvent(String eventType,Object...args);
}
