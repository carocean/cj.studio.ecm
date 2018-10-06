package cj.studio.ecm.bridge;

import java.lang.annotation.Annotation;

/**
 * 切入点是在执行桥的方法时，传到方面叫切入点，表示方面正切入到服务的哪个方法。
 * 
 * <pre>
 * 它用于连接桥与一串方面，切入点的概念，就是它表示了在调用者与桥服务之间有一串方面，而它管理着这串方面的执行序
 * 
 * －桥拥有一个连接点字段，一个方法被执行时，会根据连接点创建一个切入点JoinPoint，一个切入点按职责链模式将方面按先后次序穿起来
 * -切入点用于确定哪个服务的哪些方法被拦截，或者为桥提供接口，因为接口也就包含了连接的方法
 * </pre>
 * 
 * @author carocean
 *
 */
public interface ICutpoint {
	/**
	 * 桥服务的名称
	 * 
	 * @return
	 */
	String getServiceDefId();

	/**
	 * 切入的方法的注解
	 * 
	 * @param clazz
	 * @return
	 */
	<T extends Annotation> T getMethodAnnotation(Class<T> clazz);

	/**
	 * 切入方法名
	 * 
	 * @return
	 */
	String getMethodName();

	/**
	 * 切入桥服务的类型
	 * 
	 * @return
	 */
	Class<?> getType();

	/**
	 * 切入方法描述
	 * 
	 * @return
	 */
	String getMethodDesc();

	/**
	 * 切入到下个方面，直到切入链的末尾（内部执行器方法）被切入方法才会被调用。
	 * 
	 * <pre>
	 * －桥拥有一个连接点字段，一个方法被执行时，会根据连接点创建一个切入点JoinPoint，一个切入点按职责链模式将方面按先后次序穿起来
	 * 
	 *   注意：方面在桥中声明的顺序，它以职责链模式模拟一个操作方法的执行序，每个方面没有直接执行方法的权限，
	*      系统为每条切入点线的末尾插入了执行方面，只有执行到链的末尾被代理的方法才会被执行,这是因为：
	*      －如果方面中设置了执行真实方法的api，假设此时方面a执行了方法，然后传给其后方面b，而其后方面并不知道该方法是否执行过（不同的开发者）
	*        b也执行了这个方法，则此时一个方法被代理，却被执行莫名的次数，这会造成方法执行序的混乱，因此：
	*        采用最尾执行原则，要想执行方法必须向后个方面传递。
	*        举个实体的例子：
	*        欲为服务s加上事务和日志，由于日志也要记录事务作用下的异常情况，则：
	*        1.日志方面必须放在事务方面前面
	*        2.日志－》事务－》执行器方面
	 * </pre>
	 * 
	 * @param bridge 桥，被切入的服务
	 * @param args
	 * @return
	 */
	Object cut(Object bridge, Object[] args);
}
