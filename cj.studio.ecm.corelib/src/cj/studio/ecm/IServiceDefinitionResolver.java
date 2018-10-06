package cj.studio.ecm;

import cj.studio.ecm.resource.IResource;



//由于对一个服务可以有多种定义方式并存，所以同一服务被多种解析器解析后，将在扫描器中合并服务
public interface IServiceDefinitionResolver {
	/**
	 * 解析为服务定义
	 * @param resItem 
	 * @param resource TODO
	 * @return
	 */
	//用于解析服务，也解析声明的开放类型，如注解方式，或其它的如JSON注解方式都可能声明了开放类型，所以在扫描器中收集解析的开放类型
	IServiceDefinition resolve(String resItem, IResource resource);

}
