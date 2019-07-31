package cj.studio.ecm.container.factory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cj.studio.ecm.IRuntimeServiceCreator;
import cj.studio.ecm.IRuntimeServiceInstanceFactory;
import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceNameGenerator;
import cj.studio.ecm.Scope;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.container.describer.ServiceDescriber;
import cj.studio.ecm.container.registry.RuntimeServiceDefinition;
import cj.studio.ecm.context.IModuleContext;

public class RuntimeServiceInstanceFactory extends ServiceInstanceFactory implements IRuntimeServiceInstanceFactory {

	@Override
	public void initialize(IModuleContext context, IServiceNameGenerator serviceNameGenerator) {
		// TODO Auto-generated method stub
		super.initialize(context, serviceNameGenerator);
		createServiceInstanceMap();
	}

	@Override
	public void refresh() {
		
	}
	
	@Override
	public void addService(Class<?> serviceClazz, Object serviceInstance) {
		String className = serviceClazz.getName();
		IServiceDefinition def = this.createRuntimeServiceDefinition("$." + className, className);
		String inistaceName = this.getNameGenerator().generateServiceName(def, getRegistry());
		this.getServiceInstances().put(inistaceName, serviceInstance);
	}

	protected IServiceDefinition createRuntimeServiceDefinition(String definitionId, String className) {
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
		IServiceDefinition def = this.createRuntimeServiceDefinition(serviceId, serviceInstance.getClass().getName());
		String inistaceName = this.getNameGenerator().generateServiceName(def, getRegistry());
		this.getServiceInstances().put(inistaceName, serviceInstance);
	}

	@Override
	public void removeService(String serviceId) {
		this.getServiceInstances().remove(serviceId);
	}

	@Override
	public Object getService(String serviceId) {

		Object service = null;
		if (getServiceInstances().containsKey(serviceId)) {
			service = getServiceInstances().get(serviceId);
		}
		if (service != null) {
			if(service instanceof IRuntimeServiceCreator) {
				IRuntimeServiceCreator creator=(IRuntimeServiceCreator)service;
				Object obj=creator.create();
				if(obj==null) {
					obj=service;
				}
				return obj;
			}
			return service;
		}
		if (serviceId.startsWith("$.")) {
			String className = serviceId.substring(2, serviceId.length());
			try {
				// class不能在此反射，因为此处反射取不到systemResource，而以当前cj.coreLib所在的系统classloader加载肯定找不到芯片的类
				// class的加载应在类定义或元数据解析时从芯片的systemResource直接反射好类，其它的地方使用即可
				// 其它的工厂也存在同样问题,废除此代码：Class<?> serviceClazz =
				// Class.forName(className,true,);
				// 因此在注册表中增加方法:
				Class<?> serviceClazz = getRegistry().getResource().loadClass(className);
				if (serviceClazz == null)
					return null;
				ServiceCollection<?> col = this.getServices(serviceClazz);
				for (Object s : col) {
					return s;
				}
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		return super.getService(serviceId);
	}

	// 运行时只需在现有容器中找就行了，只此重写了基类
	@SuppressWarnings("unchecked")
	@Override
	public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
		List<T> list = new ArrayList<T>();
		for (Object service : this.getServiceInstances().values()) {
			if (serviceClazz.isInstance(service)) {
				if(service instanceof IRuntimeServiceCreator) {
					IRuntimeServiceCreator creator=(IRuntimeServiceCreator)service;
					Object obj=creator.create();
					if(obj==null) {
						obj=service;
					}
					list.add((T) obj);
					continue;
				}
				list.add((T) service);
			}
		}
		return new ServiceCollection<T>(list);
	}

	@Override
	public void removeService(Class<?> serviceClazz) {
		this.getServiceInstances().remove(serviceClazz.getName());
	}

	@Override
	protected Map<String, Object> createServiceInstanceMap() {
		return new HashMap<String, Object>();
	}

	@Override
	public FactoryType getType() {
		return FactoryType.runtime;
	}

	@Override
	protected void dispose(boolean disposing) {
		super.dispose(disposing);
	}
}
