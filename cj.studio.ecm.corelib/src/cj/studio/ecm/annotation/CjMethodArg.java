package cj.studio.ecm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//方法参数注解
/**
 * 方法参数注解
 * 
 * <pre>
 * －只有被内核调用的方法才起作用，否则与非服务方法一样使用
 * </pre>
 * 
 * @author carocean
 *
 */
@Target(value = { ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface CjMethodArg {
	/**
	 * 值注解
	 * 
	 * @return
	 */
	public String value() default "";

	/**
	 * 引用的服务id
	 * 
	 * <pre>
	 * －注入时机：只有被内核调用的方法才起作用，内核调用有两种：一是构造调用该方法，二是服务属性引用调用该方法
	 *   非构造调用（一般指开发者直接对方法的调用），参数引用及注入值都不起作用，此时与非服务方法一样使用。
	 * </pre>
	 * 
	 * @return
	 */
	public String ref() default "";
}
