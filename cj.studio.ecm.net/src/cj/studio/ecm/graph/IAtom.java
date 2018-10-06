package cj.studio.ecm.graph;

import cj.studio.ecm.IServiceSite;

/**
 * 原子<br>
 * 原子主要使回路中后续的功能规范化，为前路调用者提供规范的api
 * <pre>
 * 用于为回路流提供通用功能，它包装一组协议实现。
 * 
 * 它类似于组件的作用，但组件偏向于在图外的调用，它自成回路，而原子依附于当前回路流的处理当中，且当前回路的后续sink中必有其处理实现。
 * 
 * 
 * </pre>
 * 
 * @author carocean
 *
 */
public interface IAtom {
	void bind(IPlug plug);

	public static IAtom get(String name, IServiceSite site) {
		return (IAtom) site.getService(String.format("$.graph.atom.%s", name));
	}
}
