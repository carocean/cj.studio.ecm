package cj.studio.ecm.graph;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;

/**
 * 电缆线，是集线束。
 * 
 * <pre>
 * 1.用于管理同一类线的多个实例，电缆线中的导线依据一个默认导线新建，因此是一种动态同一作用但创建多线路实例的模式，这在多线程下实现多通道非常有用。
 * 2.每个导线由电缆线管理。
 * 3.直接调用电缆线插入的sink，将作为创建新导线的模板
 * 
 * </pre>
 * 
 * @author carocean
 *
 */
public interface ICablePin extends IPin {
	void onEvent(ICablePinEvent e);
	String defaultWireName();
	boolean isEmpty();
	/**
	 * 执行电缆
	 * <pre>
	 * 注意：
	 * － 该方法根据导线的忙闲状态选择空闲的一个执行，如果不存在空闲的，则：
	 * － 从其中根据侦的哈希随机选择一个执行
	 * </pre>
	 * 
	 */
	@Override
	public void flow(Frame frame, Circuit circuit) throws CircuitException;

	/**
	 * 选定导线执行
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param selectWireName 如果选择为空，则仅执行默认导线。
	 * @param frame
	 * @param circuit
	 * @throws CircuitException
	 */
	void flow(String selectWireName, Frame frame, Circuit circuit)
			throws CircuitException;

	/**
	 * 用当前电览中的指定名称的导线，连接另一个电览中指定的导线。
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param theWireName
	 * @param other
	 * @param otherWireName
	 * @param sink
	 *            要连接的导线在该参数sink的plug.branch(otherWireName)中，
	 *            其中otherWireName为参数中要连接的导线名。<br>
	 *            因此，开发者需实现sink，并在flow方法中获取连接的导线分支，并执行它的flow，以此完成通路。
	 */
	void link(String theWireName, ICablePin other, String otherWireName,
			ISink sink);

	int wireCount();

	void emptyWire();

	/**
	 * 按指定名称申请一根新导线
	 * 
	 * <pre>
	 * 导线的新建由电览线的导线构建器负责，因此该方法无返回。
	 * </pre>
	 * 
	 * @param name
	 *            导线名字。
	 * @return true成功创建 该方法不抛异常。
	 */
	boolean newWire(String name);
	boolean newWire(String name, ICableWireCreateCallback cb);
	void removeWire(String name);

	String[] enumWire();

	boolean containsWire(String name);

	/**
	 * 所有导线中共享的
	 */
	public Object options(String key);
	public boolean containsOption(String key);
	/**
	 * 所有导线中共享的
	 */
	public void options(String key, Object value);

	public void removeWireOptions(String wireName, String key);

	/**
	 * 返回导线的选项。如果该选项不存在，则在默认导线中搜。
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param wireName
	 * @param key
	 * @return
	 */
	public Object wireOptions(String wireName, String key);

	/**
	 * 指导导线的
	 */
	public void wireOptions(String wireName, String key, Object value);
	void wireEvent(String wireName, IWirePinChangedEvent changedEvent);
	void flowRandomSelect(Frame frame, Circuit circuit) throws CircuitException;
	void flowAll(Frame frame, Circuit circuit) throws CircuitException;

}
