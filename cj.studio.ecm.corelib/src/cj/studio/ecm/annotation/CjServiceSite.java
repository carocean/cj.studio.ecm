package cj.studio.ecm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 服务站点注解<br>
 * <pre>
 * －为对象或属性注入服务站点，用于对象动态的获取或设置运行时服务和一般服务
 * －如果不想作为字段可选用IServiceAfter接口
 * </pre>
 * @author Administrator
 *
 */
@Target(value={ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CjServiceSite {

}
