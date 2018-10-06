package cj.studio.ecm.adapter;

//适配器是一种代理，它固定为代理的目标是其自身,也就是被代理。
//而普通的代理代理的目标一般为别的对象
//适配器能转换成任意接口或类型进行调用，而其类型不必实现它
public interface IAdaptable {
	/**
	 * 
	 * 获得指定类型的适配对象<br>
	 * 如果参数为空，则返回IObjectSetter对象，该对象用于对适配目标赋值<br>
	 * 如果参数为接口，则返回接口适配对象，该接口不必是当前对象实现的接口<br>
	 * 可以获取执行器，进行命令方式的操作
	 * 
	 * 如果输入类类型，则适配指定类的所有接口
	 * <pre>
	 * 以下是基础适配类型
	 * IObjectSetter 参数为空则默认对象该问器
	 * IActuator 是执行器方法执行
	 * IPrototype 当前适配器的原型
	 * IProxySetter 用于拦截当前的适配器方法
	 * other custom interface by developer 其它任意包含了当前方法的接口，注意：并不一定是当前适配器原型实现的接口。
	 * 
	 * </pre>
	 * @param clazz
	 * @return
	 * @see AdapterFactory AdapterFactory 可以使用适配器工厂将任何java对象转换为适配器对象
	 */
	//如果参数是从icommand实现的类类型，则适配器输出指令
	<T> T getAdapter(Class<T> clazz);
}
