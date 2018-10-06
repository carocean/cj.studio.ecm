package cj.studio.ecm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cj.studio.ecm.IServiceSetter;
/**
 * 服务反向注射器
 * <pre>
 * 与CjServiceRef结合使用形成两对象间的交互注入
 * 
 * 对于多例模式，使用此注解无法在编译期反向注入，因为多例模式只有在请求该服务时才实例化
 * 因为一般的多例服务在有请求时生成实例并在生成时反向注入也是合理的。
 * 为了更容易理解，考虑为反向注解增加属性isForce，如果为真则强制生成服务实例并反向注入，等于反向强制该服务请求
 * </pre>
 * <h3>解释</h3>
 * <ul>
 * <li>将当前修饰所在的服务实例注入到所修饰的字段里</li>
 * <li>注入目标类型必须实现IServiceSetter</li>
 * </ul>
 * @author C.J 赵向彬 <br>
 *   2012-1-28<br>
 * @see
 * <li>{@link IServiceSetter IServiceSetter}
 * <li>{@link CjServiceRef CjServiceRef}
 */
@Target(value={ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
//对于多例模式，使用此注解无法在编译期反向注入，因为多例模式只有在请求该服务时才实例化
//特别是在反向注入到端子时，由于端子并没请求此服务，所以不会注入到端子。
//故而需要设定：当多例中存在反向注入属性且属性为端子时，需要为此在编译期间生成服务，非端子属性不必。
//因为一般的多例服务在有请求时生成实例并在生成时反向注入也是合理的。
//为了更容易理解，考虑为反向注解增加属性isForce，如果为真则强制生成服务实例并反向注入，等于反向强制该服务请求,这样就不用判断属性是否为端子了
public @interface CjServiceInvertInjection {
	//强制所注解的服务生成实例，主要用于多例模式下。
	//由于多例服务只有请求时才生成实例，如果想让端子得到服务，必须强制，这样反向目标等于向当前服务在容器启动期间发送了服务请求。
	public boolean isForce() default false;
}
