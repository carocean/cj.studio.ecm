package cj.studio.ecm;

/**
 * 用于发现芯片中的开放服务
 * 
 * @author Administrator
 * 
 */
public interface IExotericServiceFinder extends IValve {
	/**
	 * 获取下游芯片包含的指定服务类型的服务
	 * @param serviceClazz
	 * @return
	 */
	public <T> ServiceCollection<T> getExotericServices(Class<T> serviceClazz);
	//获取本地开放服务提供器，该提供器可查询本地芯片包含的开放服务
	/**
	 * 获取本地开放服务提供器，该提供器可查询本地芯片包含的开放服务
	 * @return
	 */
	public IServiceProvider getLocalExotericServiceProvider();
	/**
	 * 获取管道，该方法返回一个不可用的管道
	 */
	@Override
	public IPipeline getPipeline();
}
