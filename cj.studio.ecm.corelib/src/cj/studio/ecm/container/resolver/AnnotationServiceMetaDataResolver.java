package cj.studio.ecm.container.resolver;

import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceMetaData;
import cj.studio.ecm.container.registry.AnnotationServiceDefinition;
import cj.studio.ecm.resource.IResource;

public class AnnotationServiceMetaDataResolver extends ServiceMetaDataResolver {
	

	@Override
	public IServiceMetaData resolve(IServiceDefinition def, IResource resource) {
		//由于混合定义类型也是注解类型，所以注解的元解析器可以解析混合类型
		if (!(def instanceof AnnotationServiceDefinition))
			return null;
		return super.resolve(def, resource);
	}


}
