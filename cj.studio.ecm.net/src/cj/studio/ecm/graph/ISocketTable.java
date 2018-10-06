package cj.studio.ecm.graph;

import java.util.List;

import cj.ultimate.IDisposable;

/**
 * 用于维护sink与所插入的pin之间的关系表
 * <pre>
 * 一个pin中不能插入多个同名sink实例
 * 一个同名sink可以插入到多个pin中
 * 对于同一sink上的各个插头，称之为socket
 * </pre>
 * @author carocean
 *
 */
public interface ISocketTable extends IDisposable{
	public void put(Plug plug, String onPin) ;
	IPlug getSocket(int sinkHashCode,String pinName);
	public List<String> getSockets(int sinkHashCode) ;
	public void remove(int sinkHashCode, String onPin) ;
	void remove(String onPin) ;
	public void remove(int sinkHashCod);

	public boolean contains(int sinkHashCod) ;
}
