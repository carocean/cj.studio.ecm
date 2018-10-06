package cj.studio.ecm.graph;

import cj.studio.ecm.IServiceSite;

/**
 * 协议工厂
 * 
 * <pre>
 *
 * </pre>
 * 
 * @author carocean
 *
 */
public interface IProtocolFactory {
	CircuitException throwAt(String status, Object... error);

	String get(String status);

	String getProtocol();

	public default IProtocolFactory protocol(IServiceSite site) {
		return (IProtocolFactory) site.getService("$.graph.protocolFactory");
	}
}
