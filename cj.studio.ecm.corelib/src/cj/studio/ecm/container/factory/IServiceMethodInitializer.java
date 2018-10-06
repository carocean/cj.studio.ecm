package cj.studio.ecm.container.factory;

import cj.studio.ecm.annotation.MethodMode;

//方法的初始化和执行器
public interface IServiceMethodInitializer {
	//执行方法
	Object invoke(Object service,MethodMode callMode);
	//初始化返回服务
	void initReturnService(Object result);
}
