package cj.studio.ecm.graph;

public interface ICableWireCreateCallback {
	/**
	 * 已创建了一个导线
	 * <pre>
	 * 在构建后且在电缆的onNewWired事件前发生。
	 * 
	 * 一般用于在onNewWired事件前为导线赋予属性
	 * </pre>
	 * @param name
	 * @param wire
	 */
	void createdWire(String name, IWirePin wire);

}
