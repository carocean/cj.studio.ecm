package cj.studio.ecm;


//合并器仅用于将不同的解析器所解析的连续的服务定义和元数据合并在一起，它不是用来合并所有具有相同全类名的服务定义的。
public interface ICombinServiceDefinitionStrategy {
	//一个服务可通过多种方式定义，如注解、JSON,XML，而且可能同时用这三种方式描述同一个服务
		//所以方法用于合并不同的服务定义到同一个服务定义中
//合并完成后将服务定义和元数据注册表注册表
		void combinServiceDefinition(IServiceDefinition def, IServiceDefinitionRegistry registry);
}
