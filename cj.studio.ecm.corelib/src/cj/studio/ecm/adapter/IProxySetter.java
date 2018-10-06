package cj.studio.ecm.adapter;

import cj.ultimate.net.sf.cglib.proxy.InvocationHandler;

/**
 * 对适配对象添加代理功能
 * <pre>
 *	以支持编程时的动态代理。
 *	应用场景：经常用于在程序集外对得到的外部服务再进行方法拦截。
 *	而适配器对象ioc模式常用于程序集内对象定义的设计期间
 *	此作为一种适配器在执行期间添加拦截的机制。
 * </pre>
 * @author carocean
 *
 */
public interface IProxySetter {
	void proxy(InvocationHandler callback);
}
