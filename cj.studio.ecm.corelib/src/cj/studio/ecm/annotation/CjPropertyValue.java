package cj.studio.ecm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 为属性注入值或对象及代码段<pre>
 * －不同的解析器提供不同的注入功能
 * 
 * </pre>
 * @author carocean
 *
 */
//为属性设置值
@Target(value={ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CjPropertyValue {
	/**
	 * 解析器，默认cj.basic
	 * <pre>
	 * －cj.propfile 引用assembly.properters中配置的属性
	 * －cj.jsonList 元素中可以$.xxx来引用名为xxx的服务
	 * －cj.jsonMap 元素中可以$.xxx来引用名为xxx的服务
	 * －cj.basic 在用json,xml注入值时，可用${new Object}格式注入代码段
	 * －以及自定义解析器 @see IValueParser or ValueParser
	 *   自写义解析器的名字是解析器声明的CjSerivce(name=''的名字
	 * </pre>
	 * @return
	 */
	public String parser() default "cj.basic";
	public String value() ;
}
