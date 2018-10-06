package cj.studio.ecm.container.resolver;

import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceMetaData;
import cj.studio.ecm.container.registry.XmlServiceDefinition;
import cj.studio.ecm.resource.IResource;

public class XmlServiceMetaDataResolver extends ServiceMetaDataResolver {

	@Override
	public IServiceMetaData resolve(IServiceDefinition def, IResource resource) {
		if(!(def instanceof XmlServiceDefinition))
			return null;
		return super.resolve(def, resource);
	}
}
