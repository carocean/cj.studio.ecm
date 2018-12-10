package cj.studio.ecm.net;

public interface INetboardEvent {
	/**
	 * 
	 * 参数： <br>
	 * 参数0:String：进程<br>
	 * 参数1:IAssembly 程序集<br>
	 * 参数2:ServiceCollection<IGraph> 程序集内的graphs <br>
	 * 参数3:IGraph[] 所有先前已在容器中的graph<br>
	 * 返回：无 <b>注意：</b>事件触发时机。当前graph的芯片启动可能晚于一些芯片，因此只能监听到其后发生的线板事件。
	 */
	String EVENT_RUNPROCE = "notifyRunProc";
	/**
	 * 
	 * 参数：<br>
	 * 参数0:String：进程<br>
	 * 参数1:IAssembly 程序集<br>
	 * 参数2:ServiceCollection<IGraph> 程序集内的graphs 返回：无
	 * <b>注意：</b>事件触发时机。当前graph的芯片启动可能晚于一些芯片，因此只能监听到其后发生的线板事件。
	 */
	String EVENT_KILLPROCE = "notifyKillProc";
	/**
	 * 当netsite启动时已完成所有进程的启动后发生。
	 * 参数：<br>
	 * 参数0:Map<String,IAssembly> key:pid 程序集<br>
	 * 返回：无 <b>
	 */
	String 	EVENT_CHIPS_LOADED="chipsLoaded";
	/**
	 * 当netsite启动时已完成所有服务器的启动后发生。
	 * 参数：<br>
	 * 参数0:serverNames array<br>
	 * 参数1:List<IServer>
	 * 返回：无 <b>
	 */
	String EVENT_SERVERS_LOADED = "serversLoaded";
	/**
	 * 当netsite启动时已完成所有客户端的启动后发生。
	 * 参数：<br>
	 * 参数0:clientNames array<br>
	 * 参数1:List<IClient>
	 * 返回：无 <b>
	 */
	String EVENT_CLIENTS_LOADED = "clientsLoaded";
	/**
	 * netsite启动完成后发生<br>
	 * 无参无返回
	 */
	String EVENT_NETSITE_LOADED = "netsiteLoaded";
	Object event(String cmd, Object... args);
}
