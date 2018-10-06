package cj.studio.ecm;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;

import cj.studio.ecm.container.describer.FieldWrapper;
import cj.studio.ecm.container.describer.MethodWrapper;
import cj.studio.ecm.container.describer.ServiceMethod;
import cj.studio.ecm.container.describer.ServiceProperty;

/**
 * service元数据<br>
 * 代表IOC机制所要注入的对象中的关键数据，可能是类、属性和方法对象<br>
 * 与服务定义不同的是，服务定义可能是通过JSON或注解定义的描述，这些描述用于修饰元数据。
 * @author Administrator
 *
 */
public interface IServiceMetaData {
	FieldWrapper getServicePropMeta(ServiceProperty propDef);
	MethodWrapper getServiceMethodMeta(ServiceMethod methodDef);
	Class<?> getServiceTypeMeta();
	void addServicePropMeta(ServiceProperty key, Field value);
	void addServiceMethodMeta(ServiceMethod methodDef,AccessibleObject value);
}
