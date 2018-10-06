package cj.studio.ecm.container.factory;

import java.util.Iterator;
import java.util.Map;

import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceInstanceFactory;
import cj.studio.ecm.container.describer.ServiceMethod;

//方法工厂。用于服务方法的配置和初始化，方案工厂包含一个方法返回的服务集合，该集合为弱引用，用于临时管理方法返回服务
//方法工厂并不注册到容器中，而且由其它实例工厂在初始化到方法时调用此工厂，此工厂为容器的固有工厂，将在容器初始化时创建
//方法工厂方法返回的服务实例仅存在于被释放后这段区间，方法工厂无区间定义，实例生命期是种默认行为。
//方法工厂对外不提供服务实例，它仅用于跟踪方法返回服务的生命周期
public interface IServiceMethodInstanceFactory extends IServiceInstanceFactory{

	//获取服务定义，该定义具有方法的定义
	IServiceDefinition getServiceDefinition(String className);

	//是否包含了指定类型的服务定义，该定义具有方法定义
	boolean containsServiceDefinition(String sClassName);
	
	IServiceMethodInitializer getServiceMethodInitializer(
			IServiceDefinition def, ServiceMethod sm);
	IServiceMethodInitializer getServiceMethodInitializer(String serviceDefId,String methodName,String desc);
	
	Iterator<String> iteratorServiceWithMethod() ;
	Map<ServiceMethod,IServiceMethodInitializer> getServiceMethodInitializer(String service);
}
