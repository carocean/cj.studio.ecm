package cj.studio.ecm.annotation;

public enum MethodMode {
	/**
	 * 只有采用属性引用的方式引用该方法时，该方法才会被呼叫，且返回值被注入。构造函数永远是ref
	 */
	ref,
	/**
	 * 服务只有被调用，不能被引用，它会使所有对它的引用均失效，它主要用来直接被调用时注入返回值
	 * 注意：自引用的方法在非ioc的调用下仍可注入返回值；自引用方法内的属性虽可引用方法，但返回值为空。
	 */
	self,
	/**
	 * 该方法即可在被引用时呼叫，也可在服务本身初始化时被呼叫
	 */
	both
}
