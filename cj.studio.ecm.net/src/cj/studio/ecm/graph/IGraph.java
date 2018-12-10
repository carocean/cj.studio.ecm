package cj.studio.ecm.graph;

import java.util.Map;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.IServiceSite;
import cj.ultimate.IDisposable;

/**
 * <pre>
 * 图。用于组装处理流程，主要实现指定的应用协议包。将外部消息格式转换为协议的消息
 * 图是无状态的，端子的执行序由链表保证无状态执行，即一个槽在处理时，其它对图的请求仍可进行
 * 
 * 
 * @author carocean
 * </pre>
 */
// 类似于netty中ChannelFactory架构，它决定了管道，槽和端子类型，因而图即是协议的包装
public interface IGraph extends IPrinter, IDisposable {
	public IComponent component(String name);
	public String[] enumComponent();
	public boolean containsComponent(String name);
	public boolean containsInput(String name);
	public boolean containsOutput(String name);
	void parent(IServiceProvider parent);
	boolean isInit();
	String name();
	public IServiceSite site();
	IGraphCreator creator();
	/**
	 * 进程号，在netsite中的进程
	 * <pre>
	 * 如果非在netsite中部署，必方法返回name
	 * </pre>
	 * @return
	 */
	public String processId();
	/**
	 * 选项，图的选项为所有端子共享，即端子的options可取出
	 * <pre>
	 *
	 * </pre>
	 * @param key
	 * @param value
	 */
	void options(String key,Object value);
	Object options(String key);
	boolean containsOptions(String key);
	IProtocolFactory protocolFactory();
	/**
	 * 可接受的协议侦输入
	 * <pre>
	 * 格式：
	 * 	为正则表达式，注意转义，一般用法：
	 * 	1. 以|号隔开，如：http/1.1|router/1.0|nl/1.0
	 * 	2. .*，表示接受所有协议侦输入,含.号
	 * 一般用于netsite按协议路由需要此信息。
	 * 
	 * 如果没有重载此方法，则默认为protocolFactory指定的协议。
	 * 
	 * 
	 * 
	 *   
	 * </pre>
	 * @return
	 */
	String acceptProtocol();
//	/**
//	 * 
//	 * <pre>
//	 * 调用该方法将导致重新初始化图
//	 * </pre>
//	 * @param creator
//	 */
//	void setCreator(GraphCreator creator);
	/**
	 * 无配置初始化图
	 * <pre>
	 * 初始化图
	 * </pre>
	 */
	void initGraph();
	void initGraph(Map<String,Object> options);
	String[] enumInputPin();
	String[] enumOutputPin();
	IPin in(String pinName);
	/**
	 * <pre>
	 * 输出端子。如果想接收图的输出信息，请调用者为输出端子添加自己的sink到输出端子末尾。 
	 * 实现：图有许多端子分支，但输出只有一个，因此需要汇聚端子信息。方式是为要输出的各分支端子添加同一个sink实例，而由此实例再启分支，该分支即为输出端子。
	 * 
	 * 注：图的端子类型不需要区分输入端子和输出端子，如果两个图连接，只需要在一个图的输入端子上加了sink，且将另一个图的输入端子作为该sink的分支端子
	 * </pre>
	 * 
	 * @return
	 * 
	 */
	IPin out(String pinName);

}
