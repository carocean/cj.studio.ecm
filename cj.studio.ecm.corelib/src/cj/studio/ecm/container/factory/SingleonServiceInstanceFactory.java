package cj.studio.ecm.container.factory;

import java.util.HashMap;
import java.util.Map;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceDefinitionRegistry;
import cj.studio.ecm.IServiceMetaData;
import cj.studio.ecm.IServiceNameGenerator;
import cj.studio.ecm.Scope;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.container.describer.ServiceDescriber;
import cj.studio.ecm.context.IModuleContext;
import cj.ultimate.collection.ICollection;

/**
 * 单例工厂
 * <p>
 * 用于管理和生成单例服务对象
 * </p>
 * <ul>
 * <li>它是单例服务容器</li>
 * <li>它创建单例服务</li>
 * </ul>
 * 
 * @author C.J 赵向彬 <br>
 *         2012-1-16<br>
 * @see
 *      <li>{@link IServiceDefinitionRegistry 注册表}</li>
 *      <li>{@link ServiceInstanceFactory}</li>
 */
public class SingleonServiceInstanceFactory extends ServiceInstanceFactory {

	public SingleonServiceInstanceFactory() {
	}

	@Override
	public void initialize(IModuleContext context, IServiceNameGenerator serviceNameGenerator) {
		super.initialize(context, serviceNameGenerator);
	}

	@Override
	public void refresh() {
		this.instanceSingleonServices();
	}

	protected void instanceSingleonServices() {
		ICollection<String> names = getRegistry().enumServiceDefinitionNames();
		for (String name : names) {
			IServiceDefinition def = getRegistry().getServiceDefinition(name);

			ServiceDescriber sd = def.getServiceDescriber();
			if (sd.getScope() != Scope.singleon) {
				continue;
			}
			String instancename = getNameGenerator().generateServiceName(def, getRegistry());
			if (getServiceInstances().containsKey(instancename))
				continue;
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
						this.onAfterServiceCreated(def, meta, instancename, service);
						getServiceInstances().put(instancename, service);
					} catch (Exception e) {
						CJSystem.current().environment().logging().error(getClass(),
								String.format("服务:%s构建失败，原因：%s", instancename, e));
						throw new EcmException(e);
					}

				}
			}

		}
	}

	@Override
	public Object getService(String serviceId) {
		Object service = getServiceInstances().get(serviceId);
		if (service != null) {
			return service;
		}
		service = super.getService(serviceId);
		if (service != null) {
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
				if (serviceClazz != null) {
					ServiceCollection<?> col = this.getServices(serviceClazz);
					if (!col.isEmpty()) {
						for (Object s : col) {
							service = s;
							break;
						}
					}
				}
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		return service;
	}

	@Override
	protected Map<String, Object> createServiceInstanceMap() {
		// TODO Auto-generated method stub
		return new HashMap<String, Object>();
	}

	@Override
	public FactoryType getType() {
		// TODO Auto-generated method stub
		return FactoryType.singleon;
	}

}
