package cj.studio.ecm;

import java.net.URL;
import java.util.List;

import cj.studio.ecm.resource.IResource;

//上行类型搜索管道，用于资源类型的搜索
//在依赖的资源中如何搜索开放类型
//一个资源对应一个上行管道,而一个程序集一个资源，即：一个程序集只有一个上行管道
public interface IExotericalResourcePipeline{
	//管道。有上行管道和下行管道派生接口。
	//阀门持有一个管道实例，管道组装多个阀门实例，每个阀门内可以持有一个独立的管道，也可与其它阀门共享管道。阀门将管道串连起来。阀门实现处理、外部通讯功能
	//每个管道被一个阀持有，对于上行管道，它还拥有多个向上的阀，对于下行管道，其拥有多个下级的阀
//	IValve getOwner();
	/**
	 * 外部服务类型集合
	 * <pre>
	 *
	 * </pre>
	 * @return
	 */
	List<Class<?>> enumExotericalType();
	boolean contains(IResource resource);

	void addReference(IResource resource);
	//在依赖的资源中搜寻引用的类型
	Class<?> searchReferenceType(String className)throws ClassNotFoundException ;
	//在依赖的资源中搜寻引用的资源
	URL searchResource(String sName);

	void removeReference(IResource resource);
	//设置开放类型
	void addExotericalTypeName(String name,boolean isPackagePath);
	void removeExotericalTypeName(String name);
	boolean isContainsExotericalTypeName(String name);
	//管道的持有者
	IResource getOwner();

	void dispose();
}
