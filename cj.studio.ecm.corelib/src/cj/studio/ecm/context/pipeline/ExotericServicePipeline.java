package cj.studio.ecm.context.pipeline;

import java.util.ArrayList;
import java.util.List;

import cj.studio.ecm.IDownriverPipeline;
import cj.studio.ecm.IExotericServiceFinder;
import cj.studio.ecm.ServiceCollection;

//开放服务的实例管理
public class ExotericServicePipeline implements IDownriverPipeline {
	private List<IExotericServiceFinder> downFinders;
	IExotericServiceFinder owner;

	public ExotericServicePipeline() {
		this.downFinders = new ArrayList<IExotericServiceFinder>();
	}

	public ExotericServicePipeline(ExotericServiceFinder owner) {
		this();
		this.owner = owner;
		owner.pipeline = this;
	}

	@Override
	public <T> ServiceCollection<T> getExotericServices(Class<T> serviceClazz) {
		List<T> all = new ArrayList<T>();
		for (IExotericServiceFinder finder : downFinders) {
			ServiceCollection<T> col = finder.getExotericServices(serviceClazz);
			if (!col.isEmpty())
				all.addAll(col.asList());
		}
		return new ServiceCollection<T>(all);
	}

	@Override
	public void addExotericServiceFinder(IExotericServiceFinder finder) {
		downFinders.add(finder);
	}

	@Override
	public void removeExotericServiceFinder(IExotericServiceFinder finder) {
		downFinders.remove(finder);
	}

	@Override
	public IExotericServiceFinder getOwner() {
		return owner;
	}

	@Override
	public void dispose() {
		downFinders.clear();
	}
}
