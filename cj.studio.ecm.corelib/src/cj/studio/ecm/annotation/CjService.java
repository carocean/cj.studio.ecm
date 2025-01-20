package cj.studio.ecm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cj.studio.ecm.Scope;
/**
 * 服务注解
 * <pre>
 * cj服务支持：
 * －支持annotation，json, xml方式声明服务，此为annotation方式。注解方式也可与json,xml方式同并用，注解方式优先级最低
 * －注入属性
 * －注入值
 * －注入代码段
 * －默认构造函数创建（仅scope为单例时）
 * －静态方法和工厂方法（一视同仁，支持几种scope下使用）
 * －成员方法
 * －服务芯片外可见声明，即声明为isExoteric＝true才能通过调用程序集的workbin.part和assembly.parent得到。
 * 注意：
 * 1.无参数定义服务，将以所注解的类型名作为服务名，并默认为单例
 * 2.如果指定区域为非单例声明而不指定名称，将以所注解的服务的类型名+实例的hashcode作为服务名
 * </pre>
 * @author Administrator
 *
 */
@Target(value={ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CjService {
	/**
	 * 服务名。为空时以所注解的类型名作为服务名
	 * @return
	 */
	public String name() default "";
	/**
	 * 服务作用域，决定实例的创建方式
	 * @return
	 */
	public Scope scope()  default Scope.singleon;
	/**
	 * 声明为开放服务
	 * 
	 * 
	 * <pre>
	 * －true为开放，默认是false
	 * －只有声明为开放服务，在程序集外部通过workbin.part和assembly.parent才能获到得到
	 * －开放意思为可以在芯片外获取到该服务，任何服务类型都能成为开放服务。
	 * 
	 * 概念区别：开放服务和开放类型 @CjExotericalType为开放类型
	 * －开放服务是服务对象在程序集内外的可见性
	 * －开放类型是某个类型（不一定是服务类型）在程序集内外的可见性。往往用于让下游程序集依赖且扩展功能。
	 *    
	 * 注意：
	 * 
	 * - 声明为开放的服务，会被内核实现为适配器IAdapter对象，因此便可在程序集外部由调用者强制转换为所需的任何类型
	 * - 在程序集内实现程序集外的类型，在外部直接使用该适配器类型是很不错的方式。
	 * - 子程序集只能引用父程序集中的开放服务，即：isExoteric=true
	 * - 开放类型的实现类也必须声明为开放服务才可在程序集外被搜索到
	 * </pre>
	 * @return
	 */
	public boolean isExoteric() default false;
	//指定构造方法的原因是，所定义的服务可能没有默认的构造函数，这种情况下就无法新建服务了
	/**
	 * 服务构造器
	 * <pre>
	 * 它用于在服务创建时生成服务的实例，因此称之为服务构造器
	 * 
	 * ·只有在声明为单例时才有效
	 * ·可以是构造函数或静态方法，如果是静态方法，必须返回该服务的可转换类型（如服务实现的接口类型、服务的派生类型）
	 * 
	 * </pre>
	 * @return
	 */
	public String constructor() default "";
}
