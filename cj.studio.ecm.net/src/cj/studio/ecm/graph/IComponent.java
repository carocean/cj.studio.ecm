package cj.studio.ecm.graph;

import cj.studio.ecm.IServiceSite;

/**
 * 组件
 * <pre>
 * 作用：
 * 1.组件将对图的调用显示化、接口化，包装协议的内部实现。
 * 2.组件优点在于可按业务需求构建回路
 * 
 * 它与原子的区别是：组件偏向于在图外的调用，它自成回路，而原子依附于当前回路流的处理当中，且当前回路的后续sink中必有其处理实现。
 * </pre>
 * @author carocean
 *
 */
public interface IComponent {
	/**
	 * 构建组件回路
	 * <pre>
	 * 
	 * </pre>
	 * @param creator
	 */
	void buildComponentCircuit(GraphCreator creator);
	/**
	 * 获取图中的组件。
	 * <pre>
	 * 也可直接在plug.site()中通过$.graph.component.xxx调用
	 * 
	 * 该方法内部代码：
	 * site.getService(String.format("$.graph.component.%s",name))
	 * </pre>
	 * @param name
	 * @param site
	 * @return
	 */
	public static IComponent component(String name,IServiceSite site){
		return (IComponent)site.getService(String.format("$.graph.component.%s",name));
	}
	
}
