package cj.studio.ecm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cj.studio.ecm.bridge.IBridgeable;
import cj.studio.ecm.bridge.UseBridgeMode;
import cj.studio.ecm.container.describer.UncertainObject;

/**
 * 用于引用服务<br>
 * 1.不指定参数使用，以所注解的字段的名字作为要查找服务ID，如果找不到则报错<br>
 * 2.以指定的类型或服务名查找服务 <br>
 * <b>i主：</b>
 * 
 * @author Administrator
 * 
 */
@Target(value = { ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CjServiceRef {
	/**
	 * 按类型查询。引用服务类型，如果不设此值，会以所注解的字段类型赋值，即此值永不为空
	 * <pre>
	 * －如果查询的服务实例超过1个元素，，则必须是ICollection或其派生类或派生接口。
	 * －如果小于等于1个元素，则其修饰属性类型可以接受单个注入也可以是ICollection及派生类型<br>
	 * －在使用单元素时，开发者须保证唯一性<br>
	 * <br>
	 * <b>注：</b>
	 * </pre>
	 * @return
	 */
	//注：目前还有bugger，因为类型引用是搜索服务容器内有服务类型，而它不会强制所引用服务的初始化，因此导致服务搜索不全。理应在服务容器装载完之后使之生效。
	public Class<?> refByType() default UncertainObject.class;

	/**
	 * 按服务名查询
	 * 
	 * @return
	 */
	public String refByName() default "";
	/**
	 * 引用服务方法
	 * <pre>
	 * －refByName用于指定方法所在服务，如为空表示为当前服务的方法
	 * </pre>
	 * @return
	 */
	// 引用在CjService中定义的方法,如果方法带有参数
	public String refByMethod() default "";
	/**
	 * 使用桥接器。见UseBridgeMode中的用法
	 * <pre>
	 * －默认 normal
	 * －通过 CjJoinpoint 注解到当前属性 以设定私有方面
	 * 
	 * 注：只有通过refByName引用到的服务才适用本注解词，如果是通过refByMethod或refByType引用桥服务，该注解词无效，总是采用UseBridgeMode.normal
	 * </pre>
	 * @see CjJoinpoint
	 * @see IAdapter
	 * @see IBridgeable
	 * @return
	 */
	public UseBridgeMode useBridge() default UseBridgeMode.normal;
	// public EServiceSearchMode[] searchMode() default {} ;
}
