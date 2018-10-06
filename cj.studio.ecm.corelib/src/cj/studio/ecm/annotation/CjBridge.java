package cj.studio.ecm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cj.studio.ecm.bridge.IAspect;

//加上此注解表明服务支持桥
/**
 * 桥
 * 
 * <pre>
 * －服务如果是桥可强制转换为IBridgeable接口
 * －是当前服务和方面 @see IAspect 之间的桥梁，用于服务方法的拦截
 * －桥所声明的服务会被内核自动实现为适配器(IAdapter)对象。
 * －桥服务中定义的方面作用到所有引用者，因此称为公共方面，通过属性桥接到此桥服务，可为属性引用添加私有方面。
 * －作用：服务属性引用ServiceRef方式，另一种通过容器上下文直接调用服务。均会发生拦截。
 * 
 * －桥接,是把服务方法的请求桥接出去，一个服务实例至多拥有一个桥 
 * －一个桥至少拥有一个方法体执行切面 
 * －桥是接入的服务类型的一个派生类型或者接口类型
 * －一个服务可以生成适配器也可以生成桥
 * 
 * 相关概念：
 *  一个服务对象是一个桥，服务对象拥有连接点，连接点收集了所在服务对象定义的方面，方面是服务对象不同的使用方式，如同一个人多张脸。
 *  桥
 *  连接点
 *  方面
 *  
 * 区别：
 * －适配器也对java对象的代理，服务如声明为开放服务和桥均自动成为适配器
 * －桥也是一种代理，它但只作用于服务，桥的主要用途是面向方面编程。
 * 
  * <b>注意：方面在桥中声明的顺序，它以职责链模式模拟一个操作方法的执行序，每个方面没有直接执行方法的权限，
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
 * @author carocean
 * @see IAspect
 */
@Target(value = { ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface CjBridge {
	/**
	 * 可以指定一个和多个方面，写作：
	 * 
	 * <pre>
	 * myAspect1+myAspect2-myAspect3 其中＋号表示支持拦截该服务方法的方面，－号为拒绝哪个方面拦截该服务
	 * 
	 * 注意：方面是被切入点ICutPoint管理，按职责链执行，因此具有严格的前后次序，且最后一个（执行器方面才会真正的执行目标方法）
	 * 上所列方面执行序为：myAspect1－》myAspect2－》执行器方面
	 * 
	 * 此处定义的拒绝方面具有最高优先级，它会拒绝属性引用时对应的私有方面
	 * </pre>
	 * @return
	 */
	public String aspects() default "";
}
