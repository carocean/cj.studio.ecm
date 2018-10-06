package cj.studio.ecm;

import cj.studio.ecm.resource.IResource;


public interface IServiceMetaDataResolver {
	/**
	 * 它根据服务定义解析出类型中定义的元数据。如json方式，properties:{name:"zhaoxb"}<br>
	 * 解析后则对应相应类型的name属性的Field对象
	 * @param definition
	 * @param resource TODO
	 * @return
	 */
	//解析器将真正的加载类型
	IServiceMetaData resolve(IServiceDefinition definition, IResource resource);
}
