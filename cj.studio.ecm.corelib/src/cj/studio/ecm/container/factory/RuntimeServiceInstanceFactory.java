package cj.studio.ecm.container.factory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cj.studio.ecm.IRuntimeServiceInstanceFactory;
import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceDefinitionRegistry;
import cj.studio.ecm.IServiceNameGenerator;
import cj.studio.ecm.Scope;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.container.describer.ServiceDescriber;
import cj.studio.ecm.container.registry.RuntimeServiceDefinition;

public class RuntimeServiceInstanceFactory extends ServiceInstanceFactory
		implements IRuntimeServiceInstanceFactory {
	private Map<String, Object> serviceMap;

	@Override
	public void initialize(IServiceDefinitionRegistry registry,
			IServiceNameGenerator serviceNameGenerator) {
		// TODO Auto-generated method stub
		super.initialize(registry, serviceNameGenerator);
		this.serviceMap = createServiceInstanceMap();
	}

	@Override
	public void refresh() {
		// TODO Auto-generated method stub
		this.serviceMap = createServiceInstanceMap();
	}

	@Override
	public void addService(Class<?> serviceClazz, Object serviceInstance) {
		String className = serviceClazz.getName();
		IServiceDefinition def = this.createRuntimeServiceDefinition("$."
				+ className, className);
		String inistaceName = this.getNameGenerator().generateServiceName(def,
				getRegistry());
		this.serviceMap.put(inistaceName, serviceInstance);
	}

	protected IServiceDefinition createRuntimeServiceDefinition(
			String definitionId, String className) {
		IServiceDefinition def = new RuntimeServiceDefinition();
		ServiceDescriber des = new ServiceDescriber();
		def.setServiceDescriber(des);
		des.setScope(Scope.runtime);
		des.setServiceId(definitionId);
		des.setClassName(className);
		def.setServiceDescribeForm(IServiceDefinition.SERVICE_DESCRIBEFORM);
		getRegistry().registerServiceDefinition(definitionId, def);
		return def;
	}

	@Override
	public void addService(String serviceId, Object serviceInstance) {
		IServiceDefinition def = this.createRuntimeServiceDefinition(serviceId,
				serviceInstance.getClass().getName());
		String inistaceName = this.getNameGenerator().generateServiceName(def,
				getRegistry());
		this.serviceMap.put(inistaceName, serviceInstance);
	}

	@Override
	public void removeService(String serviceId) {
		this.serviceMap.remove(serviceId);
	}

	@Override
	public Object getService(String serviceId) {

		Object service = null;
		if (serviceMap.containsKey(serviceId))
			service = serviceMap.get(serviceId);
		if (service == null) {
			if (serviceId.startsWith("$.")) {
				String className = serviceId.substring(2, serviceId.length());
				try {
					// class不能在此反射，因为此处反射取不到systemResource，而以当前cj.coreLib所在的系统classloader加载肯定找不到芯片的类
					// class的加载应在类定义或元数据解析时从芯片的systemResource直接反射好类，其它的地方使用即可
					// 其它的工厂也存在同样问题,废除此代码：Class<?> serviceClazz =
					// Class.forName(className,true,);
					// 因此在注册表中增加方法:
					Class<?> serviceClazz = getRegistry().getResource()
							.loadClass(className);
					if(serviceClazz==null)return null;
					ServiceCollection<?> col = this.getServices(serviceClazz);
					for (Object s : col) {
						return s;
					}
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		}
		if (service == null)
			service = super.getService(serviceId);
		return service;
	}

	//运行时只需在现有容器中找就行了，只此重写了基类
	@SuppressWarnings("unchecked")
	@Override
	public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
		List<T> list = new ArrayList<T>();
//		ServiceCollection<T> col = super.getServices(serviceClazz);
//		list.addAll(col.asList());
		for (Object service : this.serviceMap.values()) {
			if (serviceClazz.isInstance(service)) {
				list.add((T)service);
			}
		}
		return new ServiceCollection<T>(list);
	}

	@Override
	public void removeService(Class<?> serviceClazz) {
		this.serviceMap.remove(serviceClazz.getName());
	}

	@Override
	protected Map<String, Object> createServiceInstanceMap() {
		return new HashMap<String, Object>();
	}

	@Override
	public FactoryType getType() {
		// TODO Auto-generated method stub
		return FactoryType.runtime;
	}

	@Override
	protected void dispose(boolean disposing) {
		serviceMap.clear();
		super.dispose(disposing);
	}
}
