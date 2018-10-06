package cj.studio.ecm;

/**
 * 服务站点，提供或增加、移除动态服务。
 * @author Administrator
 *
 */
public interface IServiceSite extends IServiceProvider{
	String KEY_SERVICE_SITE="cj.studio.ecm.IServiceSite";
	void addService(Class<?> clazz,Object service);
	void removeService(Class<?> clazz);
	void addService(String serviceName,Object service);
	void removeService(String serviceName);
	String getProperty(String key);
	String[] enumProperty();
}
