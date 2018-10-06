package cj.studio.ecm.container.resolver;

import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceDefinitionResolver;
import cj.studio.ecm.resource.IResource;


public abstract class AbstractServiceDefinitionResolver implements
		IServiceDefinitionResolver {

	@Override
	public abstract IServiceDefinition resolve(String resItem, IResource resource);
	
}
