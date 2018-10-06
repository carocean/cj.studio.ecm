package cj.studio.ecm;

import cj.studio.ecm.weaving.IWeaverChain;


//服务类型织入器,该对象由资源持有
//类型编织器，它组织和管理一组编织器链，如：工厂方法编织器、适配器编织器，服务代理编织器等
public interface IServiceTypeWeaver {

	//输入要织入的类名，元及回调，生成的一个类型将来生成的实例会织入一个回调实例，回调如方法拦载、属性拦载等
	byte[] weave(String className, byte[] b,IWeaverChain chain);
	boolean hasWeavingType(String sClassName);
}
