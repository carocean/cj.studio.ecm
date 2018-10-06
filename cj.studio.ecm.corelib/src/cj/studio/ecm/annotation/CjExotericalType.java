package cj.studio.ecm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <pre>
 * //修饰开放的类型 
 * //开放的类型对依赖它的程序集可见
 * //开放类型支持多种配置方式，Assembly.json中声明开放包，单类或单包的xml\json配置声明方式，及本文件实现的这种注解方式
 * //如果Assembly.json中声明不开放包，但却声明了该包中的某个服务的类型，则该服务类型还是开放的
 * </pre>
 * @author carocean
 *
 */
@Target(value = { ElementType.TYPE, ElementType.PACKAGE })
@Retention(RetentionPolicy.RUNTIME)
public @interface CjExotericalType {
	/**
	 * 开放类型的类型名
	 * 
	 * <pre>
	 * Iworkbin.exotericalType()可按此名获取所有匹配的开放类型
	 * </pre>
	 * 
	 * @return
	 */
	public String typeName() default "";
}
