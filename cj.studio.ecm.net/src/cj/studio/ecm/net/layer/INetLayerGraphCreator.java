package cj.studio.ecm.net.layer;

public interface INetLayerGraphCreator {
	public final static String KEY_SESSION_SINK = "KEY_SESSION_SINK";
	public final static String KEY_COOKIE_CONTAINER_SINK = "KEY_COOKIE_CONTAINER_SINK";
	ISessionManager getSessionManager();
}
