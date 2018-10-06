package cj.studio.ecm;

import cj.studio.ecm.container.factory.FactoryType;
import cj.ultimate.IDisposable;

/**
 * 服务实例工厂，它用于生成Serivce。服务实例工厂用于存储和管理一个区间的对象。<br>
 * 单例工厂在扫描后即执行。其它的工厂在特定请求时生成。<br>
 * 如temp工厂，为请求随机分配实例，并采用弱引用，当引用者释放，此工厂中的实例也被释放。
 * 
 * @author Administrator
 * 
 */
public interface IServiceInstanceFactory extends IServiceProvider,IDisposable {
	FactoryType getType();
	String getDefinitionNameOfInstance(String instanceName);
	@Override
	public Object getService(String serviceId);
	public void parent(IServiceProvider parent);

	@Override
	public <T> ServiceCollection<T> getServices(Class<T> serviceClazz);

	/**
	 * 初始化实例工厂。
	 * @param registry 注册表
	 * @param serviceNameGenerator 服务名生成器，它用来为服务实例生成名字，它在上下文刷新时为各服务实例工厂分配，以便统一为各工厂中的实例命名。
	 */
	void initialize(IServiceDefinitionRegistry registry, IServiceNameGenerator serviceNameGenerator);
	void refresh();
}
