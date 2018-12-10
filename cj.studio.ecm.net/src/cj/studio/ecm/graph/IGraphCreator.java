package cj.studio.ecm.graph;

import cj.studio.ecm.IServiceSite;
/**
 * 图构建器
 * <pre>
 * 1.它是构建图的sink,pin工厂
 * 2.它可以通过site（）方法获取芯片的服务容器，如果将graph声明为服务的话
 * 3.它是graph的ioc容器，并提供给图开发者和图使用者来替换相应的服务。
 * 
 * 推荐图的开发逻辑结构：
 * 1.分为三层，sink层用于service方法和协议之间转换；service层实现业务逻辑；dao层实现持久
 * 2.图开发者预定三层之间的组装逻辑。
 * 3.图使用者可通过自建图构建器，或从图开发者提供的构建器派生，以实现对三层任何一层的替换
 * 
 * 派生类介绍：
 * 1.cjservice实现模式，该模式的图定义到程序集中，其各个组件均声明为服务，组件内部从site()中取得相关的sink和pin。
 * 		此模试在GraphCreator中对接了服务站点。
 *  	此模式第三方使用时，需将该图所在的程序集指定为自己程序集的父，不能只以类型引用，否则在自己程序集中声明的图可能没有相应组件
 * 2.非cjservice实现模式，需自行实现，但推荐在build方法中装配自己的类，作用等同于完成ioc注入。这种方式实现的图，可在自己的程序集中声明为服务以引用
 * </pre>
 * @author carocean
 *
 */
public interface IGraphCreator {
	/**
	 * 用来将一段回路协议化，提供友好的访问后续回路的方式。
	 * <pre>
	 *
	 * </pre>
	 * @param name
	 * @return
	 */
	IAtom newAtom(String name);
	public boolean containsInputPin(String pinName);
	public boolean containsOutputPin(String pinName);
	
	/**
	 * 应用协议
	 * <pre>
	 * 各种状态码与信息的定义
	 * </pre>
	 * @return
	 */
	IProtocolFactory protocolFactory();
	/**
	 * 为图的服务站点
	 * <pre>
	 *
	 * </pre>
	 * @return
	 */
	IServiceSite site();
	/**
	 * 根据sink服务名新建sink
	 * <pre>
	 *
	 * </pre>
	 * @param sinkName
	 * @return
	 */
	ISink newSink(String sinkName);
	/**
	 * 根据组件名新建组件，组件建立后需调用其init方法初始化。
	 * <pre>
	 *
	 * </pre>
	 * @param compName
	 * @return
	 */
	IComponent newComponent(String compName);
	/**
	 * 新建一个内部电缆
	 * <pre>
	 *
	 * </pre>
	 * @param name
	 * @param b
	 * @return
	 */
	ICablePin newCablePin(String pinName);
	/**
	 * 按访问模式新建电缆。
	 * <pre>
	 *
	 * </pre>
	 * @param name
	 * @param access
	 * @param b
	 * @return
	 */
	ICablePin newCablePin(String pinName,Access access);
	
	/**
	 * 新建一个内部端子
	 * <pre>
	 *
	 * </pre>
	 * @param pinName
	 * @return
	 */
	IWirePin newWirePin(String pinName);
	
	/**
	 * 
	 * <pre>
	 * 按访问模式创建
	 * </pre>
	 * @param access
	 * @return
	 */
	IWirePin newWirePin(String pinName,Access access);
}
