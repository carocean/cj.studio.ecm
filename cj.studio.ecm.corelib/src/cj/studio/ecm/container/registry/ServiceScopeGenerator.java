package cj.studio.ecm.container.registry;

import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceDefinitionRegistry;
import cj.studio.ecm.IServiceScopeGenerator;
import cj.studio.ecm.Scope;

public class ServiceScopeGenerator implements IServiceScopeGenerator {

	@Override
	public String generateServiceScope(IServiceDefinition definition,
			IServiceDefinitionRegistry registry) {
		Scope scope = definition.getServiceDescriber().getScope();
		int form =0;
		if(definition instanceof AnnotationServiceDefinition)
			form=((AnnotationServiceDefinition) definition).getAnnotateForm();
		if (null == scope) {
			//如果服务的区间为空，就看服务类型如果是服务、适配器、
			switch (form) {
			case 0:
			case 1:
			case 2:
				scope=Scope.singleon;
				break;
			case 3:
			case 4:
				break;
			}
		}
		return scope.name();
	}

}
