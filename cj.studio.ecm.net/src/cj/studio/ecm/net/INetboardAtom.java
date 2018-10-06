package cj.studio.ecm.net;

import java.util.Map;

import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IAtom;

/**
 * 用于在chip的图中访问netsite
 * <pre>
 * 包装了netsite/1.0协议
 * 动态管理连接，net板等
 * </pre>
 * @author carocean
 *@see NetSiteVisitor
 */
public interface INetboardAtom  extends IAtom{
	boolean gcIs(String outputPin,String netName)throws CircuitException;
	void gcPlug(String outputPin,String netName,String flow,String rewrite,String to)throws CircuitException;
	void gcUnplug(String outputPin,String netName)throws CircuitException;
	boolean ccIs(String clientName)throws CircuitException;
	void ccClose(String clientName)throws CircuitException;
	void ccConnect(String ip,String port,String protocol,String clientName,Map<String, String> props)throws CircuitException;
}
