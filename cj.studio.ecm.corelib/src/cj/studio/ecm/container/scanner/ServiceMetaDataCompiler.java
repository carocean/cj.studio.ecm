package cj.studio.ecm.container.scanner;

import java.util.ArrayList;
import java.util.List;

import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceDefinitionRegistry;
import cj.studio.ecm.IServiceMetaData;
import cj.studio.ecm.IServiceMetaDataCompiler;
import cj.studio.ecm.IServiceMetaDataResolver;
import cj.studio.ecm.resource.IResource;
import cj.studio.ecm.weaving.ServiceTypeWeaverChain;
import cj.ultimate.IDisposable;
import cj.ultimate.collection.ICollection;

public class ServiceMetaDataCompiler implements IServiceMetaDataCompiler,
		IDisposable {
	private List<IServiceMetaDataResolver> resolvers;
	private IResource resource;
	private IServiceDefinitionRegistry registry;

	public ServiceMetaDataCompiler() {
		resolvers = new ArrayList<IServiceMetaDataResolver>();
	}

	@Override
	public void addResolver(IServiceMetaDataResolver resolver) {
		this.resolvers.add(resolver);
	}

	@Override
	public void removeResolver(IServiceMetaDataResolver resolver) {
		this.resolvers.remove(resolver);
	}

	@Override
	public void setResource(IResource resource) {
		this.resource = resource;
	}

	@Override
	public void setRegistry(IServiceDefinitionRegistry registry) {
		this.registry = registry;

	}

	@Override
	public void setWeaverChain(ServiceTypeWeaverChain weaverChain) {
		this.resource.setWeaverChain(weaverChain);
	}

	@Override
	public void compile() {
		ICollection<String> names = registry.enumServiceDefinitionNames();
		for (String name : names) {
			IServiceDefinition def = registry.getServiceDefinition(name);
			// 注册到注册表中的服务定义，必须是可实例化为服务实例的服务定义,所以服务描述属性不能为空
			IServiceMetaData md = null;
			if (def.getServiceDescriber() != null) {
				for (IServiceMetaDataResolver mr : resolvers) {
					md = mr.resolve(def, resource);
					if (md != null)
						break;
				}
				if (md == null)
					throw new RuntimeException("服务定义缺少元数据");
				registry.assignMetaData(def, md);
			}
		}
	}

	protected void dispose(boolean disposing) {
		if (disposing) {
			this.resolvers.clear();
			this.registry = null;
			this.resource = null;
		}

	}

	@Override
	public void dispose() {
		this.dispose(true);

	}

	@Override
	protected void finalize() throws Throwable {
		this.dispose(true);
		super.finalize();
	}
}
