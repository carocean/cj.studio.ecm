package cj.studio.ecm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * 方法定义
 * 如果方法具有定义，将会被内核自动执行。如果方法被调用，则会按定义注入相应的数据，或初始化返回的服务对象。
 * @author Administrator
 *
 */
//如果是构造方法，则转换后的bind为<init>
//方法支持静态方法且可带参数
@Target(value={ElementType.METHOD,ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
//由于它直接修饰到方法上，所以省去了方法描述器中的bind,argType的配置
//如果返回的修饰都为空，则表明不修饰方法返回值
public @interface CjMethod {
	//如果无设置则取注解的方法名为此赋值,总之此字段非空
	/**
	 * 方法别名，调用者以该别名作为方法本身。
	 * <pre>
	 * －默认为方法签名
	 * </pre>
	 * @return
	 */
	public String alias() default "";
//	/**
//	 * 调用模式表明该方法被调用的时机
//	 * <pre>
//	 * 1.ref表示只有其它服务引用了该方法时被呼叫，而在其所在的服务初始化时是无效的
//	 * 2.self表示在方法所在的服务初始化时被呼叫，而引用此方法是无效的
//	 * 3.both表示不论在引用还是服务初始化时均可被呼叫
//	 * 
//	 * 非构造默认是both，构造默认是ref且开发者必须设为ref，否则陷死循环，程序已将构造这种情况定死为ref，因此不论开发者为之设任何值
//	 * </pre>
//	 * @return
//	 */
//	public MethodMode callMode() default MethodMode.both;
	/**
	 * <pre>
	 * 如果方法返回对象类型不确定，则可以使用该注解
	 * 注解返回对象的类型，如果该对象是服务，则：
	 * －以指定id的服务定义为返回对象初始化的依据并注入
	 * －如果为.号表示采用当前返回对象的定义
	 * －如果同时为returnDefinitionId和returnDefinitionType赋值，优先用ID
	 * 
	 * 注：如果服务不存在或非返回类型则方法仍被执行，但返回的实例不会被注入
	 * 
	 * 建议指明返回的服务定义id，如果不指定将以返回类型在容器中查找，损耗性能。
	 * </pre>
	 * @return
	 */
	//如果同时存在反回ID和类型，优先用ID
	public String returnDefinitionId() default "";
	//如果为.号则表示以当前返回类型搜索服务定义来初始化服务实例
	/**
	 * <pre>
	 * 如果方法返回对象类型不确定，则可以使用该注解
	 * 注解返回对象的类型，如果该对象是服务，则：
	 * －以指定type的服务定义为返回对象初始化依据并注入
	 * －如果为.号表示采用当前返回对象的定义
	 * －如果同时为returnDefinitionId和returnDefinitionType赋值，优先用ID
	 * 
	 * 注：如果服务不存在或非返回类型则方法仍被执行，但返回的实例不会被注入
	 * </pre>
	 * @return
	 */
	public String returnDefinitionType() default "";
}
