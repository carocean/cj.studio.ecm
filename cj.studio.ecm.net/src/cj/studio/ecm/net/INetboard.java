package cj.studio.ecm.net;

import cj.ultimate.IDisposable;


/**
 * 表示一个net线路板
 * 
 * <pre>
 * net线路板是netsite的主要组件
 * 用于管理server,client网络组件
 * 只有安装在netsite的芯片才有此功能
 * 
 * netsite将线路板注入到芯片中的开放graph服务容器之内。
 * </pre>
 * 
 * @author carocean
 *
 */
public interface INetboard extends IDisposable{
	String KEY_NETBOARD = "$.netsite.netborad";
	/**
	 * 执行net线路板指令
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param line
	 * @return
	 */
	Object doCommand(String line);

	/**
	 * netborad的事件，见INetboard中的事件常量说明
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param cmd
	 * @param args
	 * @return
	 */
	void doEvent(String cmd, Object... args);
	
	public void addEvent(INetboardEvent e);
	public void removeEvent(INetboardEvent e);

	boolean containsEvent(Object ret);
}
