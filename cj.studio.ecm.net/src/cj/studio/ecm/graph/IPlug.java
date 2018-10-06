package cj.studio.ecm.graph;

import cj.studio.ecm.IChipInfo;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.ultimate.IDisposable;

/**
 * 插头：一个插头有且只有一个输入pin(非结构上，每次正在执行该插头的调用者pin即称之输入pin)用来输入，n个分支pin用来输出<br>
 * 任何端子均可成为某个sink的输入端子，只要是pin1.add(sink1)，pin2.add(sink1)，有pin1,pin2，如果pin2正在执行，则pin2此时为sink1的输入端子<br><br>
 * 通过插头的功能使得多个端子与一个槽能够聚合。如：端子p1,p2,p3，分别插入槽s1，则产生三个插头u1,u2,u3分别与端子对应。当执行某个端子时，s1的主端子总是其执行端子。如p2执行时，则s1.plug.trunk=p2
 * <br>
 * 注：一个插头有且只有一个输入pin，是运行时才这样，当多个pin共享同一个plug实例时（只有分支pin才能这么做），结构上是有多个输入pin的，但在运行时确实有且只有一个pin作为输入。
 * <br>  插头实例在同一个端子上是唯一的
 * @author carocean
 *
 */
public interface IPlug extends IPrinter,IDisposable{
	IProtocolFactory protocol();
	boolean isDisposed();
	boolean hasNext();
	/**
	 * 获取原子服务。
	 * <pre>
	 *
	 * </pre>
	 * @return 返回值可能为空
	 */
	IAtom atom(String name);
	/**
	 * 当前执行序所有在的pin的名
	 * <pre>
	 *
	 * </pre>
	 * @return
	 */
	String fromPinName();
	/**
	 * 来源pin的外部访问等级。
	 * <pre>
	 *
	 * </pre>
	 * @return
	 */
	Access fromPinAccess();
	IChipInfo chipInfo();
	/**
	 * 所在的pin名称，该名称在pin执行时才能获取到，因此是动态的。
	 * <pre>
	 *
	 * </pre>
	 * @return
	 */
	String owner();
	/**
	 * 插头名，该名即是sink名。
	 * <pre>
	 * 注意：插头名针对同一个被插入的sink不是唯一的名称。
	 * </pre>
	 * @return
	 */
	String name();
	/**
	 * 全名。该名称是动态的。
	 * <pre>
	 * 正在执行的所属的输入端子名＋#＋sink名
	 * </pre>
	 * @return
	 */
	String fullName();
	/**
	 * 当前插头所在sink的插座上（sink的圆周上）的所有插头，包括当前插头和邻接插头。
	 * <pre>
	 * 如果不存在指定的插头则为空
	 * 该方法不进行集合运算。
	 * 
	 * 注意：如果取出当前的插头再执行会陷入循环♻️
	 * </pre>
	 * @param key 是插头的owner()，即持有插头的端子名
	 * @return
	 */
	IPlug socket(String key) ;
	/**
	 * 列出当前插头所在sink的插座上（sink的圆周上）的所有插头名。包括当前插头和邻接插头。
	 * <pre>
	 * 该方法进行集合运算。
	 * </pre>
	 * @return
	 */
	String[] enumSocket();
	/**
	 * 在当前插件上追加一个插件，如果该插件已存在后继插件，则后继插件作为新插件的后续插件。
	 * 
	 * <pre>
	 * 为插头设置下一个插头。
	 * 
	 * 如果当前插头已有后续插头，则后续插头作为新插头的后续，而将新插头在原位置插入。
	 * 
	 * 该方法使得pin的线路构建更加易读。如：output.plugLast("",sink).plugin("",s).plugin("",s2);更像是一条线
	 * </pre>
	 * @param sinkName
	 * @param sink
	 * @return 返回新插头。
	 */
	IPlug plugin(String sinkName, ISink sink);
	/**
	 * 选项为插头独有
	 * <pre>
	 *
	 * </pre>
	 * @param key
	 * @return
	 */
	Object option(String key);
	IPlug option(String key,Object v);
	IServiceSite site();
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
	/**
	 * 执行输入pin上的下一个sink
	 * <pre>
	 * 分支pin的执行需要获取分支而后执行
	 * </pre>
	 * @param frame
	 * @param circuit 插件的回路从上一插件而来
	 * @throws CircuitException TODO 如果发现异常，将向上层调用者抛出异常
	 */
	void flow(Frame frame,Circuit circuit) throws CircuitException;
	String[] enumBranch();
	IBranchKey[] enumBranchKey();
	boolean containsBranch(String name);
	boolean containsBranch(IBranchKey key);
	/**
	 * 返回当前的plug
	 * <pre>
	 *
	 * </pre>
	 * @param name
	 * @param pin
	 * @return
	 */
	IPlug plugBranch(String name,IPin pin);
	void plugBranch(IBranchKey key,IPin pin);
	IPin branch(String name);
	IPin branch(IBranchKey key);
	IPin branch(String name,IBranchSearcher searcher);
	int branchCount();
	void removeBranch(String name);
	void removeBranch(IBranchKey key);
	void emptyBranches();
	Object optionsPin(String key);
	void optionsPin(String key, Object value);
}
