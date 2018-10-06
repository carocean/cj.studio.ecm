package cj.studio.ecm;


public interface IServiceScopeGenerator {
	String generateServiceScope(IServiceDefinition definition,
			IServiceDefinitionRegistry registry);
}
