package cj.studio.ecm.graph;

/**
 * 提供Graph 的事件
 * 
 * <pre>
 * 仅监听当前graph
 * 
 * 该类型可从graph.site().getService("$.graph.handler")获取，
 * 创建者将实例放入ecm容器graph.site().addService("$.graph.handler",handler);
 * </pre>
 * 
 * @author carocean
 *
 */
public interface IGraphHandler {
	String KEY_GRAPHHANDLER="$.graph.handler";
	/**
	 * 该事件用于编程定义到线板的连接。
	 * 无参数<br>
	 * 返回IGraphPlugNetboardEvent事件
	 */
	public String EVENT_GRAPH_PLUG_NETBOARD_EVENT = "getGraphPlugNetboardEvent";
	/**
	 * 当芯片的当前graph进入netsite容器事件，参数：<br>
	 * 如果一个芯片拥有多个graph，则会晌应多次该事件。<br><br>
	 * 0：pid<br>
	 * 1:Graph<br> 当前进程的持有芯片被添加到graph容器的一个graph<br>
	 * 2:IGraph[] 此参数是在晌应该事件后，graph容器中全部的芯片<br>
	 * 无返回值。
	 */
	public String EVENT_ENTERING_NETSITE = "enteringNetSite";
	/**
	 * 当芯片杀死时的当前graph退出容器事件，参数：<br>
	 * 如果一个芯片拥有多个graph，则会晌应多次该事件。<br><br>
	 * 0：pid<br>
	 * 1:graph，从graph移除的图
	 * 无返回值
	 */
	public String EVENT_LEAVED_NETSITE = "leavedNetSite";
	/**
	 * netsite向芯片的graph请求pin事件，参数：<br>
	 * 参数0:要申请的端子名,<br>
	 * 参数1:当前进程号，<br>
	 * 参数2:端子类型输入或是输出端子，<br>
	 * 参数3:申请的端子欲连接net目标<br>
	 * 返回值为：pin实例
	 */
	public String EVENT_DEMAND_PIN = "demandPin";
	/**
	 * 参数0:正在插入net的chipGraph的input端子名,<br>
	 * 参数1:当前进程号，<br>
	 * 参数2:chipGraph实例，<br>
	 * 参数3:欲插入的netGraph实例<br>
	 * 参数4:当前容器中chipgraphis全量，形式：IGraph[]<br>
	 * 参数5:当前容器中netgraphis全量，形式：IGraph[]<br>
	 * 无返回值
	 */
	public String EVENT_PLUG_INPUT_NET = "plugInputToNet";
	/**
	 * gc之前，netsite的服务、客户端、进程已经装载完备。<br>
	 * 参数0:正在插入net的chipGraph的output端子名,<br>
	 * 参数1:当前进程号，<br>
	 * 参数2:chipGraph实例，<br>
	 * 参数3:欲插入的netGraph实例<br>
	 * 参数4:当前容器中chipgraphis全量，形式：IGraph[]<br>
	 * 参数5:当前容器中netgraphis全量，形式：IGraph[]<br>
	 * 无返回值
	 */
	public String EVENT_PLUG_OUTPUT_NET = "plugOutputToNet";
	/**
	 * 参数0:拔出net的chipGraph的input端子名,<br>
	 * 参数1:当前进程号，<br>
	 * 参数2:chipGraph实例，<br>
	 * 参数3:拔出的netGraph实例<br>
	 * * 参数4:当前容器中chipgraphis全量，形式：IGraph[]
	 * 无返回值
	 */
	public String EVENT_UNPLUG_INPUT_NET = "unplugInputToNet";
	/**
	 * 参数0:拔出net的chipGraph的output端子名,<br>
	 * 参数1:当前进程号，<br>
	 * 参数2:chipGraph实例，<br>
	 * 参数3:拔出的netGraph实例<br>
	 * * 参数4:当前容器中chipgraphis全量，形式：IGraph[]
	 * 无返回值
	 */
	public String EVENT_UNPLUG_OUTPUT_NET = "unplugOutputToNet";
	/**
	 * 返回一个INetboardEvent对象。用于侦听线板<br>
	 * 参数：无<br>
	 * 返回：INetboardEvent对象<br>
	 * <b>注意：</b>事件触发时机。当前graph的芯片启动可能晚于一些芯片，因此只能监听到其后发生的线板事件。
	 */
	String EVENT_NETBOARD_EVENT = "getNetboardEvent";
	/**
	 * 响应net启动后，该事件执行在plug事件之后<br>
	 * 参数：<br>
	 * 0:netName<br>
	 * 返回值无<br>
	 */
	String EVENT_NET_STARTED = "doNetStarted";

	/**
	 * 图的事件
	 * 
	 * <pre>
	 * 事件说明请使用igraphHandler中定义的常量，前缀：EVENT_
	 * 
	 * 
	 * 可以是外部传入事件，也可以是图传出事件
	 * 提供的事件有：
	 * 1.程序集入站前enteringNetSite
	 * 2.和序集入站后enteredNetSite
	 * 3.互动站向图要求新的端子用于连接时demandPin
	 * </pre>
	 * 
	 * @param cmd
	 *            命令
	 * @param args
	 */
	Object event(String cmd,  Object... args);

}
