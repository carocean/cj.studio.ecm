package cj.studio.ecm.container.registry;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import cj.studio.ecm.IServiceMetaData;
import cj.studio.ecm.container.describer.FieldWrapper;
import cj.studio.ecm.container.describer.MethodWrapper;
import cj.studio.ecm.container.describer.ServiceMethod;
import cj.studio.ecm.container.describer.ServiceProperty;
//元数据不必针对JSON/XML/ANNOTATION派生，因为元数据都是一致的，实际上是将不同的服务定义源转换为相同的元数据
//服务实例工厂再从元数据生成服务
public class ServiceMetaData implements IServiceMetaData {
	private Map<ServiceProperty, FieldWrapper> serviceProperties;
	private Map<ServiceMethod, MethodWrapper> serviceMethods;
	private Class<?> serviceTypeMeta;

	public ServiceMetaData(Class<?> serviceTypeMeta) {
		this.serviceMethods = new HashMap<ServiceMethod, MethodWrapper>();
		this.serviceProperties = new HashMap<ServiceProperty, FieldWrapper>();
		this.serviceTypeMeta = serviceTypeMeta;
	}

	
	@Override
	public FieldWrapper getServicePropMeta(ServiceProperty propDef) {
		
		return serviceProperties.get(propDef);
	}

	/**
	 * meta的用法是先增加字段，再取出包装字段,便可为字段附加信息
	 */
	@Override
	public void addServicePropMeta(ServiceProperty key, Field value) {
		FieldWrapper fw=new FieldWrapper();
		fw.setField(value);
		this.serviceProperties.put(key, fw);
	}

	@Override
	public Class<?> getServiceTypeMeta() {
		// TODO Auto-generated method stub
		return serviceTypeMeta;
	}

	@Override
	public MethodWrapper getServiceMethodMeta(ServiceMethod methodDef) {
		return serviceMethods.get(methodDef);
	}
	@Override
	public void addServiceMethodMeta(ServiceMethod methodDef, AccessibleObject value) {
		MethodWrapper mw=new MethodWrapper();
		mw.setMethod(value);
		this.serviceMethods.put(methodDef, mw);
	}
}
