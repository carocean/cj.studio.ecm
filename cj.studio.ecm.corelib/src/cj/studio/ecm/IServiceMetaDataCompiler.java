package cj.studio.ecm;

import cj.studio.ecm.resource.IResource;
import cj.studio.ecm.weaving.ServiceTypeWeaverChain;
import cj.ultimate.IDisposable;


//元数据装配器，或编译器，从注册表定义编译成元数据，并分配
public interface IServiceMetaDataCompiler extends IDisposable {
	//遍历服务定义，并通过相应解析器编译成元数据，并分配给对应的服务定义
	public void compile();
	public void addResolver(IServiceMetaDataResolver resolver);
	public void removeResolver(IServiceMetaDataResolver resolver);
	public void setWeaverChain(ServiceTypeWeaverChain weaverChain);
	void setResource(IResource  resource);
	void setRegistry(IServiceDefinitionRegistry registry);
}
