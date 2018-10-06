package cj.studio.ecm;

/**
 * 动态工厂，该工厂可由使用者在运行时添加和管理。<br>
 * 动态工厂中的服务由使用者管理其生命期
 * @author Administrator
 *
 */
public interface IRuntimeServiceInstanceFactory extends IServiceInstanceFactory {
	public void addService(Class<?> serviceClazz, Object serviceInstance);
	public void addService(String serviceId, Object serviceInstance);
	public void removeService(String serviceId);
	public void removeService(Class<?> serviceClazz);
}
