package cj.studio.ecm.container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cj.studio.ecm.IServiceContainer;
import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceDefinitionRegistry;
import cj.studio.ecm.IServiceInstanceFactory;
import cj.studio.ecm.IServiceMetaData;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.container.factory.FactoryType;
import cj.studio.ecm.parser.IValueParserFactory;
import cj.studio.ecm.parser.ValueParserFactory;
import cj.studio.ecm.resource.IResource;
import cj.studio.ecm.script.JssServiceInstanceFactory;
import cj.ultimate.collection.ICollection;
import cj.ultimate.collection.ReadOnlyCollectionBase;

public class ServiceConainer implements IServiceContainer,
		IServiceDefinitionRegistry {
	private HashMap<String, IServiceDefinition> serviceDefinitions;
	private HashMap<IServiceDefinition, IServiceMetaData> definitionMetas;
	private Map<FactoryType, IServiceInstanceFactory> factories;
	private IResource resource;
	private IValueParserFactory valueParserFactory;

	public ServiceConainer(IResource resource) {
		this.resource = resource;
		this.serviceDefinitions = new HashMap<String, IServiceDefinition>();
		this.definitionMetas = new HashMap<IServiceDefinition, IServiceMetaData>();
		this.factories = new HashMap<FactoryType, IServiceInstanceFactory>();
	}

	@Override
	public IServiceContainer getOwner() {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public Object getService(String serviceId) {
		Object service = null;
		if (("$." + IValueParserFactory.class.getName()).equals(serviceId)) {
			if (this.valueParserFactory == null)
				this.valueParserFactory = new ValueParserFactory(this);
			return this.valueParserFactory;
		}
		if ("$.cj.studio.resource".equals(serviceId)) {
			return resource;
		}
		IServiceInstanceFactory jss = this.factories.get(FactoryType.jss);
		if (jss == null)
			return null;
		service = jss.getService(serviceId);
		if (service != null)
			return service;
		if (serviceId
				.startsWith(JssServiceInstanceFactory.JSS_REQUEST_JAVA_SERVICEKEY)) {
			serviceId = serviceId
					.replace(
							JssServiceInstanceFactory.JSS_REQUEST_JAVA_SERVICEKEY
									+ ".", "");
		}
		IServiceInstanceFactory singleon = this.factories
				.get(FactoryType.singleon);
		if (singleon == null)
			return null;
		service = singleon.getService(serviceId);
		if (service != null){
			return service;
		}
		IServiceInstanceFactory multiton = this.factories
				.get(FactoryType.multiton);
		if (multiton == null)
			return null;
		service = multiton.getService(serviceId);
		if (service != null){
			return service;
		}
		IServiceInstanceFactory dynamic = this.factories
				.get(FactoryType.runtime);
		if (dynamic == null)
			return null;
		service = dynamic.getService(serviceId);
		if (service != null){
			return service;
		}
		return service;
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
		if (IResource.class.isAssignableFrom(serviceClazz)) {
			return new ServiceCollection(new IResource[] { this.resource });
		}

		List<T> list = new ServiceArrayList<T>();

		// if (serviceClazz.isAssignableFrom(ScriptObjectMirror.class)) {
		IServiceInstanceFactory factory = this.factories.get(FactoryType.jss);
		if (factory != null) {
			ServiceCollection<T> jsscol = factory.getServices(serviceClazz);
			if (!jsscol.isEmpty())
				list.addAll(jsscol.asList());
			// }
		}
		ServiceCollection<?> services = null;
		IServiceInstanceFactory singleon = this.factories
				.get(FactoryType.singleon);
		if (singleon != null) {
			services = singleon.getServices(serviceClazz);
			for (Object service : services) {
				if (!list.contains(service)) {
					list.add((T) service);
				}
			}
		}
		IServiceInstanceFactory multiton = this.factories
				.get(FactoryType.multiton);
		if (multiton != null) {
			services = multiton.getServices(serviceClazz);
			for (Object service : services) {
				if (!list.contains(service)) {
					list.add((T) service);
				}
			}
		}
		IServiceInstanceFactory runtime = this.factories
				.get(FactoryType.runtime);
		if (runtime != null) {
			services = runtime.getServices(serviceClazz);
			for (Object service : services) {
				if (!list.contains(service)) {
					list.add((T) service);
				}
			}
		}
//		for (IServiceDefinition def : this.serviceDefinitions.values()) {
//			IServiceMetaData meta = this.getMetaData(def);
//			if (meta == null)
//				continue;
//			if (serviceClazz.isAssignableFrom(meta.getServiceTypeMeta())) {
//				//本来从单例工厂位置到运行时工厂在此位置放着，后移出：20151112 by cj
				//原因：循环会造成反复加载，且影响性能。
		//如果注释掉有问题再说，有待观查
//			}
//		}
		return new ServiceCollection<T>(list);
	}

	@Override
	public void removeServiceDefinition(String name) {
		// TODO Auto-generated method stub
		this.serviceDefinitions.remove(name);
	}

	@Override
	public IServiceDefinition getServiceDefinition(String serviceId) {
		// TODO Auto-generated method stub
		return this.serviceDefinitions.get(serviceId);
	}

	@Override
	public ICollection<String> enumServiceDefinitionNames() {
		List<String> list = new ArrayList<String>();
		Set<String> set = this.serviceDefinitions.keySet();
		for (String name : set) {
			list.add(name);
		}
		return new ReadOnlyCollectionBase<String>(list) {
		};
	}

	@Override
	public void registerServiceDefinition(String name,
			IServiceDefinition definition) {
		// TODO Auto-generated method stub
		if (isServiceNameInUse(name)) {
			throw new RuntimeException("服务名为:" + name + " 已存在。");
		}
		this.serviceDefinitions.put(name, definition);
	}

	@Override
	public boolean isServiceNameInUse(String serviceId) {
		// TODO Auto-generated method stub
		return this.serviceDefinitions.containsKey(serviceId);
	}

	@Override
	public IResource getResource() {
		// TODO Auto-generated method stub
		return resource;
	}

	@Override
	public int getServiceDefinitionCount() {
		// TODO Auto-generated method stub
		return this.serviceDefinitions.size();
	}

	protected void dispose(boolean disposing) {
		if (disposing) {
			definitionMetas.clear();
			this.serviceDefinitions.clear();
			Iterator<IServiceInstanceFactory> it = this.factories.values()
					.iterator();
			while (it.hasNext()) {
				IServiceInstanceFactory factory = it.next();
				factory.dispose();
			}
			this.factories.clear();
			// this.resource=null;
			if (this.valueParserFactory != null) {
				this.valueParserFactory.dispose();
				this.valueParserFactory = null;
			}
		}
	}

	@Override
	public void dispose() {
		this.dispose(true);

	}

	@Override
	public IServiceInstanceFactory getServiceInstanceFactory(FactoryType scope) {
		IServiceInstanceFactory factory = this.factories.get(scope);
		return factory;
	}

	@Override
	public void registerServiceInstanceFactory(IServiceInstanceFactory factory) {
		FactoryType scope = factory.getType();
		if (null == scope)
			throw new RuntimeException("工厂只能用于区间的对象管理，工厂的区间不能为空");
		if (this.factories.containsKey(scope))
			throw new RuntimeException("工厂名已存在。" + scope);
		this.factories.put(scope, factory);
	}

	@Override
	public IServiceMetaData getMetaData(IServiceDefinition definition) {
		// TODO Auto-generated method stub
		return this.definitionMetas.get(definition);
	}

	@Override
	public void assignMetaData(IServiceDefinition definition,
			IServiceMetaData meta) {
		if (meta == null) {
			this.definitionMetas.remove(definition);
			// this.definitionMetas.keySet().remove(definition);
			return;
		}
		String id = definition.getServiceDescriber().getServiceId();
		if (!this.serviceDefinitions.containsKey(id)) {
			this.registerServiceDefinition(id, definition);
		}
		this.definitionMetas.put(definition, meta);
	}

}
