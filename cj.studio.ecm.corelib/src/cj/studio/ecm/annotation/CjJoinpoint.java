package cj.studio.ecm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示所声明的属性引用的服务中声明的方面的子集，如果将所引用的属性的方面看成作用于所有连接到它的全局作用域的话，那么此处是声明一个子集，
 * 即到引用服务的的私有连接，如果省去将使用默认的连接点，如果指定则
 * 
 * @author carocean
 *
 */
@Target(value = { ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CjJoinpoint {
	/**<pre>
	 * －支持＋号－规则。方面必须是所引对象的声明的方面的子集。方面必须声明为cj服务
	 * －私有方面默认插入在引用服务的切入点链的最后，如果欲插入到前面用^号，例：^myAspect3
	 * </pre>
	 * @return
	 */
	public String aspects();
}
