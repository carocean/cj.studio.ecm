package cj.studio.ecm;

import cj.studio.ecm.resource.IResource;
import cj.ultimate.collection.ICollection;
/**
 * 注册表
 * <h3>解释</h3>
 * <ul>
 * <li>功能点</li>
 * </ul>
 * @author C.J 赵向彬 <br>
 *   2012-1-16<br>
 * @see
 */
public interface IServiceDefinitionRegistry {
	void registerServiceDefinition(String name, IServiceDefinition definition);

	void removeServiceDefinition(String name);

	IServiceDefinition getServiceDefinition(String serviceId);

	ICollection<String> enumServiceDefinitionNames();

	boolean isServiceNameInUse(String serviceId);

	int getServiceDefinitionCount();
	//注册表相关资源，公开注册表资源是否合理？注册表只是ecm内部类使用，如实例工厂，对芯片是隐藏的，所以可以公开。
	//从理念上来讲，注册表附带资源是在情理之中的。目前仅被用到实例工厂反射类型，因为只有它提供了芯片的类型
	IResource getResource();
	/**
	 * 服务定义修饰的元数据，它由服务定义解析器完成解析。
	 * 
	 * @param definition
	 * @return
	 */
	IServiceMetaData getMetaData(IServiceDefinition definition);
	
	/**
	 * 为服务定义分配元数据。如果不存在此定义，则先注册定义而后分配
	 * @param definition
	 * @param meta如果该参数为null则移除先前的定义和元数据
	 */
	void assignMetaData(IServiceDefinition definition, IServiceMetaData meta);

	IServiceContainer getContainer();
	IServiceSite getOwner();
}
