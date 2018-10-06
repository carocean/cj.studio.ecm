package cj.studio.ecm.context.pipeline;

import java.util.ArrayList;
import java.util.List;

import cj.studio.ecm.IDownriverPipeline;
import cj.studio.ecm.IExotericServiceFinder;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
//外部服务查询器，它用于查询下游的开放服务，同时它带有一个本地服务提供器，即可以查询下游的芯片的服务
public class ExotericServiceFinder implements IExotericServiceFinder {
	IDownriverPipeline pipeline;
	private IServiceProvider localExotericServiceProvider;
	public ExotericServiceFinder(LocalExotericServiceProvider sp) {
		localExotericServiceProvider = sp;
	}

	@Override
	public IServiceProvider getLocalExotericServiceProvider() {
		// TODO Auto-generated method stub
		return localExotericServiceProvider;
	}

	@Override
	public IDownriverPipeline getPipeline() {
		return pipeline;
	}

	@Override
	public <T> ServiceCollection<T> getExotericServices(
			Class<T> serviceClazz) {
		ServiceCollection<T> localServices = localExotericServiceProvider
				.getServices(serviceClazz);
		ServiceCollection<T> downServices = pipeline
				.getExotericServices(serviceClazz);
		List<T> llist=localServices.asList();
		List<T> dlist=downServices.asList();
		List<T> all=new ArrayList<T>();
		all.addAll(llist);
		all.addAll(dlist);
		ServiceCollection<T> col=new ServiceCollection<T>(all);
		return col;
	}
}
