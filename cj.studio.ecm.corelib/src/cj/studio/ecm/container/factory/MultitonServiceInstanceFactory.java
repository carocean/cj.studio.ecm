package cj.studio.ecm.container.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceMetaData;
import cj.studio.ecm.IServiceNameGenerator;
import cj.studio.ecm.Scope;
import cj.studio.ecm.container.describer.PropertyDescriber;
import cj.studio.ecm.container.describer.ServiceDescriber;
import cj.studio.ecm.container.describer.ServiceInvertInjectionDescriber;
import cj.studio.ecm.container.describer.ServiceProperty;
import cj.studio.ecm.context.IModuleContext;
import cj.ultimate.collection.ICollection;

//多例工厂与动态工厂的差别在于，前者从服务定义生成实例，后者是运行时由开发者动态添加到容器中
//只有说明成多例模式，才可在请求时生成新的服务，单例和运行时服务均不会因为请求而生成新服务，所以多例工厂替代了IServiceProvider.getService(String serviceid,boolean ireqnewService)方法的作用
public class MultitonServiceInstanceFactory extends ServiceInstanceFactory {
	private Map<String, IServiceDefinition> multitonServiceDefinitonMap;
	private IServiceNameGenerator serviceNameGenerator;

	@Override
	protected Map<String, Object> createServiceInstanceMap() {
		// TODO Auto-generated method stub
		return new WeakHashMap<String, Object>();
		// return new HashMap<String, Object>();
	}

	@Override
	public FactoryType getType() {
		// TODO Auto-generated method stub
		return FactoryType.multiton;
	}

	@Override
	public void initialize(IModuleContext context,
			IServiceNameGenerator serviceNameGenerator) {
		// TODO Auto-generated method stub
		super.initialize(context, serviceNameGenerator);
		this.multitonServiceDefinitonMap = searchMultitonServiceDefinitons();
		this.serviceNameGenerator = serviceNameGenerator;
	}

	protected Map<String, IServiceDefinition> searchMultitonServiceDefinitons() {
		Map<String, IServiceDefinition> map = new HashMap<String, IServiceDefinition>();
		ICollection<String> col = getRegistry().enumServiceDefinitionNames();
		for (String defName : col) {
			IServiceDefinition def = getRegistry()
					.getServiceDefinition(defName);
			Scope scope = def.getServiceDescriber().getScope();
			if (scope == Scope.multiton) {
				map.put(defName, def);
			}
		}
		return map;
	}

	@Override
	public void refresh() {
		// 实例化拥有强制反向描述属性的服务
		this.instanceInjectionInvertForceServices();
	}
	@Override
	protected void dispose(boolean disposing) {
		super.dispose(disposing);
		if(disposing) {
			multitonServiceDefinitonMap.clear();
		}
	}
	protected void instanceInjectionInvertForceServices() {
		ICollection<String> names = getRegistry().enumServiceDefinitionNames();
		for (String name : names) {
			IServiceDefinition def = getRegistry().getServiceDefinition(name);
			ServiceDescriber sd = def.getServiceDescriber();
			if (sd.getScope() != Scope.multiton)
				continue;
			boolean isForce = false;
			for (ServiceProperty sp : def.getProperties()) {
				int pform = sp.getPropDescribeForm();
				if ((pform & ServiceProperty.SERVICEII_DESCRIBEFORM) == 2) {
					for (PropertyDescriber pd : sp.getPropertyDescribers()) {
						if (pd instanceof ServiceInvertInjectionDescriber) {
							ServiceInvertInjectionDescriber iid = (ServiceInvertInjectionDescriber) pd;
							isForce = iid.isForce();
							break;
						}
					}
					break;
				}
			}
			if (!isForce)
				continue;

			String instancename = getNameGenerator().generateServiceName(def,
					getRegistry());
			IServiceMetaData meta = getRegistry().getMetaData(def);
			Object service = this.createNewService(def, meta);
			Object[] arr = (Object[]) service;
			service = arr[0];
			boolean isServiceInited = (boolean) arr[1];
			// 因为在服务方法执行之后服务已被初始化了
			if (isServiceInited) {
				getServiceInstances().put(instancename, service);
			} else {
				if (service != null) {

					try {
						this.initialService(instancename, service, def, meta);
						this.onAfterServiceCreated(def, meta, instancename,
								service);
						getServiceInstances().put(instancename, service);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					
				}
			}
		}
	}

	@Override
	public Object getService(String serviceId) {
		IServiceDefinition def = null;
		if (serviceId.startsWith("$.")) {
			if (!this.multitonServiceDefinitonMap.isEmpty())
				for (IServiceDefinition sd : this.multitonServiceDefinitonMap
						.values()) {
					try {
						String className = serviceId.substring(2,
								serviceId.length());
						// class不能在此反射，因为此处反射取不到systemResource，而以当前cj.coreLib所在的系统classloader加载肯定找不到芯片的类
						// class的加载应在类定义或元数据解析时从芯片的systemResource直接反射好类，其它的地方使用即可
						// 其它的工厂也存在同样问题,废除此代码：Class<?> serviceClazz =
						// Class.forName(className,true,);
						// 因此在注册表中增加方法:
						Class<?> serviceClazz = getRegistry().getResource()
								.loadClass(className);
						Class<?> defserviceClazz = getRegistry().getResource()
								.loadClass(sd.getServiceDescriber()
										.getClassName());
						if (defserviceClazz == null || serviceClazz == null)
							continue;
						if (defserviceClazz.isAssignableFrom(serviceClazz)) {
							def = sd;
							break;
						}
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				}
		}
		if (def == null)
			def = this.multitonServiceDefinitonMap.get(serviceId);

		if (def == null)
			return null;

		IServiceMetaData meta = getRegistry().getMetaData(def);
		if (meta == null)
			return null;
		Object service = this.createNewService(def, meta);
		Object[] arr = (Object[]) service;
		service = arr[0];
		boolean isServiceInited = (boolean) arr[1];
		// 因为在服务方法执行之后服务已被初始化了
		if (isServiceInited) {
			String name = this.serviceNameGenerator.generateServiceName(def,
					getRegistry());
			getServiceInstances().put(name, service);
		} else {
			if (service != null) {
				try {
					String name = this.serviceNameGenerator
							.generateServiceName(def, getRegistry());
					this.initialService(name, service, def, meta);
					this.onAfterServiceCreated(def, meta, name, service);
					getServiceInstances().put(name, service);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

			}
		}
		return service;
	}


}
