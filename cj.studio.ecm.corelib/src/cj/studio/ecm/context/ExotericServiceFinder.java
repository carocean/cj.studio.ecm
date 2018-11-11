package cj.studio.ecm.context;

import cj.studio.ecm.IExotericServiceFinder;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;

class ExotericServiceFinder implements IExotericServiceFinder {
	IServiceProvider exotericSite;
	IServiceProvider localSite;
	public ExotericServiceFinder(IServiceProvider localSite,IServiceProvider exotericSite) {
		this.localSite=localSite;
		this.exotericSite=exotericSite;
	}

	@Override
	public <T> ServiceCollection<T> getExotericServices(Class<T> clazz) {
		return exotericSite.getServices(clazz);
	}

	@Override
	public <T> ServiceCollection<T> getLocalServices(Class<T> clazz) {
		return localSite.getServices(clazz);
	}
	
}
