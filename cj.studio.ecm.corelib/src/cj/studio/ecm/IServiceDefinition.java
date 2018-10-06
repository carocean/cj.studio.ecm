package cj.studio.ecm;

import java.util.List;

import cj.studio.ecm.container.describer.ServiceDescriber;
import cj.studio.ecm.container.describer.ServiceMethod;
import cj.studio.ecm.container.describer.ServiceProperty;
import cj.studio.ecm.container.describer.TypeDescriber;

/**
 * 如何定义一个服务，可能json、注解或XML方式。 <br>
 * 一个服务定义对一个服务元数据<br>
 * 在服务容器中，一个服务定义，可能会产生多个服务实例。
 * @author Administrator
 * 
 *
 */
//同一种服务可以有多种定义同时存在，如服务A，同时可以用注解，JSON,XML等定义它
public interface IServiceDefinition {

	public final static byte SERVICE_DESCRIBEFORM = 1;
	//将开放类型视为服务的一种,开放类型的声明可以不必与实例服务一起。
	public final static byte EXOTERICALTYPE_DESCRIBEFORM=2;
	// 使用SERVICEPROVIDER一定是服务，所以包括了服务位
//	public final static byte SERVICEADAPTABLE_DESCRIBEFORM = 5;
	public final static byte BRIDGE_DESCRIBEFORM = 4;
	public final static byte MIRROR_DESCRIBEFORM = 8;//9
	public final static byte MIRRORING_DESCRIBEFORM = 16;//17
	
	/**
	 * 获取服务描述，每个服务定义都至少有一个服务的描述
	 * 
	 * @return
	 */
	ServiceDescriber getServiceDescriber();

	void setServiceDescriber(ServiceDescriber serviceDescriber);

	/**
	 * 获取扩展的描述器,它与服务描述器组合使用 ，如CjServicProvider,CjObjectWrapper
	 * 
	 * @return
	 */
	List<TypeDescriber> getExtraDescribers();

	void setServiceDescribeForm(byte form);

	/**
	 * 用于表示描述器类型的组合方式和组合此服务所包含的所有描述器的支持。 而对于镜子和镜像，不论服务实现了多少个，此方法只需指明这个服务是否实现了
	 * 镜子或镜像，仅用于在建立端子的链接时搜索端子中服务是否实现了镜子或镜像，以减少 在注册表中搜索的服务定义的数量。
	 * 
	 * @return
	 */
	byte getServiceDescribeForm();

	List<ServiceProperty> getProperties();

	List<ServiceMethod> getMethods();
	//什么叫唯一？想想一下在同一个包吧，你能定义两个类全名相同的类吗？肯定不能，所以是否相等，不是取决于ID，而是取决于类型。而ID的重复与否，交由命名策略（服务定义名称产生器）决定。
	//服务容器中注册表是以ID为标识的，因为ID作为标识由命名策略决定是否重复。也就是一个类可以有多个服务定义
	//再由不同的ID产生不同的实例，即，在不同的服务定义下所注入的元素就有不同。无法断定在应用场景种不会遇到这种需求。
	//合并器仅用于将不同的解析器所解析的连续的服务定义和元数据合并在一起，它不是用来合并所有相同类型的服务定义的。
	@Override
	public boolean equals(Object obj);
}
