package cj.studio.ecm.adapter;

import java.lang.reflect.Method;

import cj.ultimate.net.sf.cglib.proxy.InvocationHandler;

/**
 * 适配器拦截器
 * <pre></pre>
 * <h3>解释</h3>
 * <ul>
 * <li>功能点</li>
 * </ul>
 * @author C.J 赵向彬 <br>
 *   2012-2-4<br>
 * @see
 * <li>{@link type1 label}
 * <li>{@link type2 label}
 */
public interface IAdapterInterrupter extends InvocationHandler{
	/**
	 * 执行适配器的方法
	 */
	@Override
	public Object invoke(Object adapter, Method method, Object[] args)
			throws Throwable;
}
