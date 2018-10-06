package cj.studio.ecm.context.pipeline;

import java.util.ArrayList;
import java.util.List;

import cj.studio.ecm.IServiceContainer;
import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.adapter.IAdaptable;
import cj.studio.ecm.adapter.IPrototype;

//本地开放服务提供器，它用来搜索本地服务
public class LocalExotericServiceProvider implements IServiceProvider {
	private IServiceContainer container;

	public LocalExotericServiceProvider(IServiceContainer container) {
		this.container = container;
	}

	@Override
	public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
		ServiceCollection<T> col = container.getServices(serviceClazz);
		if (col.isEmpty())
			return col;
		List<T> list = new ArrayList<T>();
		for (T o : col) {
			if (o instanceof IAdaptable) {
				IAdaptable adapte = (IAdaptable) o;
				IPrototype pt = adapte.getAdapter(IPrototype.class);
				String defId = pt.getServiceDefinitionId();
				IServiceDefinition def = container.getServiceDefinition(defId);
				if (def.getServiceDescriber().isExoteric())
					list.add(o);
			}

		}
		return new ServiceCollection<T>(list);
	}

	@Override
	public Object getService(String serviceId) {
		Object service = container.getService(serviceId);
		if (service == null)
			return null;
		if (service instanceof IAdaptable) {
			IAdaptable adapte = (IAdaptable) service;
			IPrototype pt = adapte.getAdapter(IPrototype.class);
			String defId = pt.getServiceDefinitionId();
			IServiceDefinition def = container.getServiceDefinition(defId);
			if (def.getServiceDescriber().isExoteric())
				return service;
		}
		return null;
	}

}
