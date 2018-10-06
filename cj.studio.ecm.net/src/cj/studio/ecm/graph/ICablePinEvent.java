package cj.studio.ecm.graph;

public interface ICablePinEvent {

	void onNewWired(ICablePin owner,String name, IWirePin wire);

	/**
	 * 在从电缆导线集合中移除后，开发者可释放端子，如果未有释放net内核会释放它
	 * <pre>
	 *
	 * </pre>
	 * @param name
	 * @param wire
	 */
	void onDestoryWired(ICablePin owner,String name, IWirePin wire);
	/**
	 * 在从电缆导线集合中移除前，但未释放端子
	 * <pre>
	 *
	 * </pre>
	 * @param name
	 * @param wire
	 */
	void onDestoringWired(ICablePin owner,String name, IWirePin wire);
}
