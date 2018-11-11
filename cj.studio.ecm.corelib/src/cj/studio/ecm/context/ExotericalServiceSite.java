package cj.studio.ecm.context;

import java.util.ArrayList;
import java.util.List;

import cj.studio.ecm.IExotericServiceFinder;
import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceDefinitionRegistry;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.adapter.IAdaptable;
import cj.studio.ecm.adapter.IPrototype;

public class ExotericalServiceSite implements IServiceProvider,IExotericalServiceSite {
	IServiceProvider parent;
	IExotericServiceFinder finder;
	public ExotericalServiceSite(IServiceProvider parent) {
		this.parent = parent;
	}

	@Override
	public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
		IModuleContext ctx = (IModuleContext) parent;
		IServiceDefinitionRegistry registry = ctx.getRegistry();
		ServiceCollection<T> col = parent.getServices(serviceClazz);
		if (col.isEmpty())
			return col;
		List<T> list = new ArrayList<T>();
		for (T o : col) {
			if (o instanceof IAdaptable) {
				IAdaptable adapte = (IAdaptable) o;
				IPrototype pt = adapte.getAdapter(IPrototype.class);
				String defId = pt.getServiceDefinitionId();
				IServiceDefinition def = registry.getServiceDefinition(defId);
				if (def.getServiceDescriber().isExoteric())
					list.add(o);
			}

		}
		return new ServiceCollection<T>(list);
	}

	@Override
	public Object getService(String serviceId) {
		if("$.exoteric.service.finder".equals(serviceId)) {
			return finder;
		}
		Object service = parent.getService(serviceId);
		if (service == null)
			return null;
		IModuleContext ctx = (IModuleContext) parent;
		IServiceDefinitionRegistry registry = ctx.getRegistry();
		if (service instanceof IAdaptable) {
			IAdaptable adapte = (IAdaptable) service;
			IPrototype pt = adapte.getAdapter(IPrototype.class);
			String defId = pt.getServiceDefinitionId();
			IServiceDefinition def = registry.getServiceDefinition(defId);
			if (def.getServiceDescriber().isExoteric())
				return service;
		}
		return null;
	}

	@Override
	public void setExotericalServiceProvider(IServiceProvider provider) {
		this.finder=new ExotericServiceFinder(((IModuleContext)parent).getDownSite(),provider);
		
	}

}
