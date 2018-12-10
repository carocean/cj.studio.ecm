package cj.studio.ecm.net;

public interface IRioClientCloseEvent {
	/**
	 * 在关闭之前发生
	 * <pre>
	 *
	 * </pre>
	 * @param client
	 */
	void onClosing(IClient client);
}
