package cj.studio.ecm;
/**
 * 在入注册表前执行命名策略
 * @author Administrator
 *
 */
public interface IServiceNameGenerator {
	String generateServiceName(IServiceDefinition definition,
			IServiceDefinitionRegistry registry);
	
}
