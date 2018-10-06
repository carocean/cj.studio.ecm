package cj.studio.ecm;

import cj.ultimate.IDisposable;

//下行管理道，用于在依赖的芯片间查找开放服务
//下行管道通过每个芯片服务容器中的IExotericServiceFinder将查找链联系起来，注意：每个服务容器中的IExotericServiceFinder是服务，每次请求便会新建，但拥有一个实例，便代表对一个芯片内开放服务的索引
//下行管道必须持有芯片的服务提供者，该提供者在注入体系中为请求的服务提供服务
//故而，下行管道必须在ModuleSite中定义实现类，并为每个实例工厂指定下行通道，在ModuleSite.createSite中将下行管道与服务站点连动起来。
//如果不是为了根据服务引用描述器参数决定如何引用，则最简单的办法是在ModuleSite.getSevices方法中实现对开放服务的查找，但这样做开发者便不能有选择的使用芯片或是依赖芯片间的开放服务的获取。
//在assembly.depen方法内通过阀门将上下游的芯片连接起来
//类似于上行管道，finder作为下行管道的阀口
//一个芯片内只有一个下行通道，对应的类型搜索的上行通道实际上也只有一个
//由于依赖的芯片必定先加载，之后才能加载下游芯片，所以下游的开放服务的实例化过程晚过上游，所以上游芯片不能在服务编译时注入，而只能在运行时通过IServiceSite动态获取下游的开放服务
public interface IDownriverPipeline extends IPipeline,IDisposable{
	//增加下游的开放查询器
	void addExotericServiceFinder(IExotericServiceFinder finder);
	void removeExotericServiceFinder(IExotericServiceFinder finder);
	@Override
	public IExotericServiceFinder getOwner();
	public  <T> ServiceCollection<T> getExotericServices(Class<T> serviceClazz);
}
