package cj.studio.ecm.net.graph;

import cj.studio.ecm.graph.IGraph;
import cj.studio.ecm.graph.IPin;
/**
 * 用于网络通讯的图
 * <pre>
 * 每个server和client均有一个netgraph
 * 它主要作用是转换物理服务器架构到graph架构
 * 另外还包括诸如同步、分流等功能。
 * 
 * 注意：该图内的功能可由调用者替换
 * 
 * 说明：
 * 有两种方案：一种是聚合式输出，将所有输入汇聚于sink而后分发到输出分支，这不利于多线程，但可以简化实现结构。输入是采用辐射式。
 * 一种是并行式，独立的输入序列，这有利于充分利用线程，但对于输入应是副射式，因为调用者不需要关心实际的信道，由图来分发给相应的信道。
 * 
 * 现实现的方案是一。
 * </pre>
 * @author carocean
 *
 */
public interface INetGraph extends IGraph {
	public static String KEY_SWITCH_GRAPH="is-switch-graph";
	/**
	 * 该名字来自于所绑定的net名，服务器、客户端名字
	 * <pre>
	 *
	 * </pre>
	 * @return
	 */
	String name();
	/**
	 * 网络通讯图的端子
	 * <pre>
	 * 用于连接开发者的业务图
	 * 
	 * 具有一个固定方法pull，该方法作用是同步调用。也可不用该方法，而采用frame.parameter("sync")方式.
	 * 如果采用同步方式，则服务器要晌应消息id在回路中。否则客户端将等待超时，这会影响性能。
	 * 说明：
	 *  对于client端，每调用一次产生一个输入端子，它是动态的。
	 *  详细：每调用一次产生一个输入端子且为之分配一个信道,如果信道池中已无空闲端子且到上限则堵塞，直到有可用为止。
	 * </pre>
	 * @return
	 */
	IPin netInput();
	/**
	 * 网络输出端子。
	 * <pre>
	 * 
	 * </pre>
	 * @return
	 */
	IPin netOutput();
	
}
