package cj.studio.ecm;

import java.net.URL;
import java.util.List;

import cj.studio.ecm.resource.IResource;

//上行管道，用于资源类型的搜索
//在依赖的资源中如何搜索开放类型
//一个资源对应一个上行管道,而一个程序集一个资源，即：一个程序集只有一个上行管道
public interface IUpriverPipeline extends IPipeline{
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
