package cj.studio.ecm.graph;


/**
 * 带有导线的端子
 * 
 * @author carocean
 *
 */
public interface IWirePin extends IPin {
	/**
	 * 如果已存在事件则覆盖。
	 * <pre>
	 *
	 * </pre>
	 * @param changedEvent
	 */
	void setChangedEvent(IWirePinChangedEvent changedEvent);
	/**
	 * 如果在电缆线内，且在当前导线中找不到key，将会到电缆线中查找。
	 */
	@Override
	public Object options(String key);
}
