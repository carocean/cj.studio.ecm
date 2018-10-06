package cj.studio.ecm.graph;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注解协议工厂的错误代码。
 * <pre>
 *	
 * </pre>
 * @author carocean
 *
 */
@Target(value = { ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CjStatusDef {
	public String message();
}
