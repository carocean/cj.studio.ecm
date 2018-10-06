package cj.studio.ecm.bridge;

/**
 * 桥是一种代理服务的形式
 * <pre>
 * 桥接,是把服务方法的请求桥接出去
 * 一个服务可以生成适配器也可以生成桥，桥一定适配器，所以可以强制转换为IAdaptable接口
 * 引用者到桥的调用是一个请求链，这个链是由方面(IAspect)组成的
 * 
 * 限制：
 * 1.在一个类c中的属性p通过桥接到桥服务b时，p不能直接声明为b的类类型，p只能是b的某个实现接口或其某个方面中指定的某个接口
 * 2.当然，限制也是合理的，这可以引导开发者必须关注于b的某个方面(接口)而使用它，而不是直接引用b的类类型。
 * </pre>
 * 
 * @author carocean
 *
 */
//原先做的可能是支持多个桥的，但现在没时间测，权当一个桥吧
//只所以不以原类类型生成桥或代理的原因写在BridgeClassAdapter中
public interface IBridge {
//	/**
//	 * // 在通过桥接器连接的服务还可以指定特定的方面，因为每个连接都生成新的代理
//	 * 
//	 * @param aspects
//	 */
//	public void attachAspects(String aspects);
	/**
	 * 切入指定的方法
	 * <pre>
	 * － 通过此方法可以切入桥服务的任何接口的方法
	 * </pre>
	 * @param actionName
	 * @param matchArgTypes
	 * @param args
	 * @return
	 */
	Object cutMethod(String actionName,Class<?>[] matchArgTypes, Object... args);
}
