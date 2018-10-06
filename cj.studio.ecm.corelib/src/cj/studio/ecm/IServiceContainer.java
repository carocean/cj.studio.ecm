package cj.studio.ecm;

import cj.studio.ecm.container.factory.FactoryType;
import cj.ultimate.IDisposable;

/**
 * 服务容器.服务定义注册表、实例工厂
 * @author Administrator
 *
 */
public interface IServiceContainer extends IServiceProvider, IServiceDefinitionRegistry,IDisposable {
	IServiceInstanceFactory getServiceInstanceFactory(FactoryType type);
	void registerServiceInstanceFactory(IServiceInstanceFactory factory);
}

