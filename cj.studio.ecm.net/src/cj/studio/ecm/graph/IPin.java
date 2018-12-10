package cj.studio.ecm.graph;

import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.ultimate.IDisposable;
/**
 * 端子，代表线的一端
 * <pre>
 * 端子即是电线的插头，它用于表明有回路连接，有：
 * 1.导线：是单根
 * 2.电缆线：由多根导线组成，它实质上等同于导线的模板类。电缆线的调用者可根据需要创建新导线。只有一根导线的电缆，作用同导线。
 * 		电缆线的插头和sink是由一类型的多个实例，即，每根导线的功能相同，只是实例不同
 * 		作用是区分多线程环境和异步网络使用，不致于拥堵。
 * 		因此：GRAPH的输入输出端子均是电缆线，只有内部端子可用导线。
 * 
 * 
 * 端子是对外的功能接口，开发者可派生为之增加逻辑方法供第三方显示调用。
 * 因此，端子往往代表一个功能。下面是一个用户端子，调用其saveUser接口，第一个sink中保存用户，分支pin中查找dept。
 * 由于graph结构是面向连接的，因此可以随意为现有的节点添加逻辑
 * 示例：pin.saveUser(user);
 * 		user to frame
 * 		pin.flow(frame);
 * 			sink.flow(frame);
 * 				frame.param.userName();
 * 				branches(deptframe);
 * 				frame.param(dept);
 * 注：图的端子类型不需要区分输入端子和输出端子，如果两个图连接，只需要在一个图的输入端子上加了sink，且将另一个图的输入端子作为该sink的分支端子
 * </pre>
 * @author carocean
 *
 */
public interface IPin extends IPrinter ,IDisposable{
	int count();
	/**
	 * 传送侦，它保证顺序执行，分支选择和分支功能由sink完成
	 * <pre>
	 * </pre>
	 * @param frame 传输的侦，分为同步、异步模式
	 * @param circuit 输入新的回路，或其为分支可从主plug.circuit()获得
	 * @throws CircuitException TODO 如果发现异常，将向上层调用者抛出异常
	 */
	void flow(Frame frame, Circuit circuit) throws CircuitException ;
	
	String[] enumSinkName();
	boolean contains(String sinkName);
	/**
	 * 返回服务站点
	 * <pre>
	 * 注意：可能返回null，只有所在graph声明为服务才有服务站点
	 * </pre>
	 * @return
	 */
	IServiceSite site();
	/**
	 * 端子名，命名规范一般是：first sink>last sink
	 * <pre>
	 * 注意：端子名只是pin在初始化时依据第一个到最后一个的端子命名，如果之后为pin在头部前面插入了sink或者尾部sink后面插入了sink，其命名并不变。
	 * </pre>
	 * @return
	 */
	String name();
	/**
	 * 将当前端子和槽连接起来，形成一对一映射关系。
	 * 如果存在该关系，则返回现有的插头，如果不存在则为之创建新的插头
	 * @param sinkName 
	 * @param sink
	 * @see 该方法将追加指定的sink到端子的末尾
	 * @return
	 */
	IPlug plugLast(String sinkName,ISink sink);
	IPlug plugFirst(String sinkName,ISink sink);
	IPlug plugBefore(String posSinkName,String sineName,ISink sink);
	IPlug plugAfter(String posSinkName,String sineName,ISink sink);
	void unPlug(String sinkName);
	IPlug plugReplace(String sinkName,ISink sink);
	IPlug getPlug(String sinkName);
	/**
	 * 设置选项。端子的选项目的是为槽提供参数
	 * 在当前端子及其插头中可见
	 * @param key
	 * @param value
	 */
	void options(String key,Object value);
	String[] enumOptions();
	void removeOptions(String key);
	Object options(String key);
	/**
	 * 图的选项参数，在图中的所有端子及插头中可见
	 * <pre>
	 *
	 * </pre>
	 * @param key
	 * @return
	 */
	Object optionsGraph(String key);
	void optionsGraph(String key,Object value);
	Access access();
	void setOptionsEvent(IPinOptionsEvent optionsEvent);
	boolean isDisposed();
}
