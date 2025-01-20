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
 * 注：设有A和B两程序集，B依赖于A，那么在A中的服务属性能否直接引用B程序集中实现的开放类型服务呢？答案是不能，因为：
 * 在A程序集服务容器装载时，B还没有装载，此时B中还没有相应的服务实例，故而只能在使用时主动获取B中的实现，而且B中的外部类型实现也必须声明为开放服务才可。
 * 可以在A中通过获取finder以查找在B中的外部类型实现，site.getService("$.exoteric.service.finder")
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
