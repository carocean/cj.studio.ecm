package cj.studio.ecm.container.scanner;

import java.util.ArrayList;
import java.util.List;

import cj.studio.ecm.ICombinServiceDefinitionStrategy;
import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceDefinitionRegistry;
import cj.studio.ecm.container.describer.MethodDescriber;
import cj.studio.ecm.container.describer.PropertyDescriber;
import cj.studio.ecm.container.describer.ServiceDescriber;
import cj.studio.ecm.container.describer.ServiceMethod;
import cj.studio.ecm.container.describer.ServiceProperty;
import cj.studio.ecm.container.describer.TypeDescriber;
import cj.studio.ecm.container.registry.AnnotationServiceDefinition;
import cj.studio.ecm.container.registry.JsonServiceDefinition;
import cj.studio.ecm.container.registry.XmlServiceDefinition;
import cj.ultimate.collection.ICollection;

public class CombinServiceDefinitionStrategy implements
		ICombinServiceDefinitionStrategy {

	// 何为相等？只要类型名一致即相等。
	// 因为：注解生成的类型为所注解的类本身，而JSON/XML要么是修饰的类型，如果为空解析定义时已用JSON文件路径名作为其类名了
	// 合并器仅用于将不同的解析器所解析的连续的服务定义和元数据合并在一起，它不是用来合并所有相同类型的服务定义的。
	// 结构：同一类型可能存在三种定义，anno,json,xml，这三种服务除了scope,serviceId不合并之外，其它的描述器均合并，但如果其serviceId相同，则只注册为一个服务定义，而默认优先使用xml/json的scope，如果xml/json的scope不同且都不为空，则服务定义失败
	// 支持不同serviceId的目的，是为了让用户为一个服务定义多种原型

	@Override
	public void combinServiceDefinition(IServiceDefinition def,
		 IServiceDefinitionRegistry registry) {
			if (def == null)
				return;
			String serviceName = def.getServiceDescriber().getServiceId();
			String className = def.getServiceDescriber().getClassName();
			// 待合并的集合

			ICollection<String> col = registry.enumServiceDefinitionNames();
			boolean hasCombine=false;
			for (String name : col) {
				IServiceDefinition registedDef = registry
						.getServiceDefinition(name);
				// 懒合并算法：找出要合并的不注册它，而是将新的DEF和META合并到注册表中注册的对象上，即变量registedDef上，
				if (className.equals(registedDef.getServiceDescriber()
						.getClassName())
						&& !def.getClass().equals(registedDef.getClass())) {
					IServiceDefinition mixutureDef = registedDef;
					//如果是不是混合服务定义，此处就新建，然后移除已登记定义，将登记定义复制入混合服务，登记此混合服务,登记过程放到了combinServiceDefDescriber方法中
					if (!(registedDef instanceof MixutureServiceDefinition)){
						registry.assignMetaData(registedDef, null);
						registry.removeServiceDefinition(serviceName);
						MixutureServiceDefinition mdef = createMixutureServiceDefinition();
						mdef.setMixutureDefinitionTypes(registedDef);
						mdef.setMixutureDefinitionTypes(def);
						mdef.copyFrom(registedDef);//拷贝已登记的服务
						mixutureDef=mdef;
					}
					this.combin(registry, mixutureDef, def);
					hasCombine=true;
					continue;
				}
			}
			//另外要创建混合服务定义类型，否则将JSON注解合并到XML定义下，或其它的服务定义下虽然程序不会有错，但有岐义
			if (!hasCombine) {
				registry.registerServiceDefinition(serviceName, def);
			}

	}
	//派生类要从本类中的MixutureServiceDefinition类型中派生，该类型在类外部不可见
	protected MixutureServiceDefinition createMixutureServiceDefinition() {
		return new MixutureServiceDefinition();
	}

	// 懒合并算法：将from的定义和元数据合并到to上
	protected void combin(IServiceDefinitionRegistry registry,
			IServiceDefinition toDef,
			IServiceDefinition fromDef) {
//		System.out.println("合并" + toDef.getServiceDescriber().getClassName());
		int form = toDef.getServiceDescribeForm()
				| fromDef.getServiceDescribeForm();
		toDef.setServiceDescribeForm((byte) form);
		this.combinServiceDefDescriber(registry, toDef,
				toDef.getServiceDescriber(),
				(toDef instanceof AnnotationServiceDefinition),
				toDef.getExtraDescribers(), fromDef.getServiceDescriber(),
				(fromDef instanceof AnnotationServiceDefinition),
				fromDef.getExtraDescribers());
		this.combinDefProperties(toDef.getProperties(), fromDef.getProperties());
		this.combinDefMethods(toDef.getMethods(), fromDef.getMethods());
	}


	private void combinDefMethods(List<ServiceMethod> toMethods,
			List<ServiceMethod> fromMethods) {
		List<ServiceMethod> complementSmList = new ArrayList<ServiceMethod>();
		for (ServiceMethod smTo : toMethods) {
			for (ServiceMethod smFrom : fromMethods) {
				if (smTo.equals(smFrom)) {// 相等表示要合并其描述器
					int form = smFrom.getMethodDescribeForm()
							| smTo.getMethodDescribeForm();
					smTo.setMethodDescribeForm((byte) form);
					List<MethodDescriber> mdList = new ArrayList<MethodDescriber>();
					for (MethodDescriber mdTo : smTo.getMethodDescribers()) {
						for (MethodDescriber pdFrom : smFrom
								.getMethodDescribers()) {
							
							//因为不同的方法描述器由不同的派生类型来描述，所以直接判断类型是否相等即可。
							if (mdTo.getClass().equals(pdFrom.getClass())) {
								continue;
							}
							// 补集
							mdList.add(pdFrom);
						}
					}
					smTo.getMethodDescribers().addAll(mdList);
					continue;
				}
				// 补集
				complementSmList.add(smFrom);
			}
		}
		toMethods.addAll(complementSmList);
	}

	private void combinDefProperties(List<ServiceProperty> toProps,
			List<ServiceProperty> fromProps) {
		List<ServiceProperty> complementSpList = new ArrayList<ServiceProperty>();
		for (ServiceProperty spTo : toProps) {
			for (ServiceProperty spFrom : fromProps) {
				if (spTo.equals(spFrom)) {// 相等表示要合并其描述器
					int form = spFrom.getPropDescribeForm()
							| spTo.getPropDescribeForm();
					spTo.setPropDescribeForm((byte) form);
					List<PropertyDescriber> pdList = new ArrayList<PropertyDescriber>();
					for (PropertyDescriber pdTo : spTo.getPropertyDescribers()) {
						for (PropertyDescriber pdFrom : spFrom
								.getPropertyDescribers()) {
							if (pdTo.getClass().equals(pdFrom.getClass())) {//因为不同的属性描述器由不同的派生类型来描述，所以直接判断类型是否相等即可。
								continue;
							}
							// 补集
							pdList.add(pdFrom);
						}
					}
					spTo.getPropertyDescribers().addAll(pdList);
					continue;
				}
				// 补集
				complementSpList.add(spFrom);
			}
		}
		toProps.addAll(complementSpList);
	}

	private void combinServiceDefDescriber(IServiceDefinitionRegistry registry,
			IServiceDefinition def, ServiceDescriber toSd,
			boolean isAnnotationDefTo, List<TypeDescriber> toExtra,
			ServiceDescriber fromSd, boolean isAnnotationDefFrom,
			List<TypeDescriber> fromExtra) {
		if (toSd.getScope() != fromSd.getScope()) {
			if (!isAnnotationDefTo && !isAnnotationDefFrom) {
				throw new RuntimeException("非注解定义的服务的scope不能重复定义");
			}
		}
		String serviceIdTo = toSd.getServiceId();
		String serviceIdFrom = fromSd.getServiceId();
		if (!serviceIdTo.equals(serviceIdFrom)) {
			if (!isAnnotationDefTo && !isAnnotationDefFrom) {
				throw new RuntimeException("非注解定义的服务的服务ID不能重复定义");
			}
		}
		
		// 在注册表中去掉已注册的定义，然后用新的ID重新注册
		registry.removeServiceDefinition(toSd.getServiceId());
		registry.registerServiceDefinition(fromSd.getServiceId(), def);
		// 以外部配置的条件为准
		toSd.setScope(fromSd.getScope());
		toSd.setDescription(fromSd.getDescription());
		toSd.setServiceId(fromSd.getServiceId());

		List<TypeDescriber> list = new ArrayList<TypeDescriber>();
		for (TypeDescriber tdTo : toExtra) {
			for (TypeDescriber tdFrom : fromExtra) {
				if (tdTo.getClass().equals(tdFrom.getClass()))
					continue;
				list.add(tdFrom);
			}
		}
		if (!list.isEmpty())
			toExtra.addAll(list);
	}
	//为了兼容注解类，josn,xml等定义类型，该类是个大合集，将特有的属性全归并过来,暂时注解定义有私有属性，所以选择从它派生
	protected class MixutureServiceDefinition extends AnnotationServiceDefinition {
		private String mixutureDefinitionTypes="";
		public MixutureServiceDefinition() {

		}
		public void setMixutureDefinitionTypes(IServiceDefinition def) {
			if(def instanceof AnnotationServiceDefinition){
				AnnotationServiceDefinition asd=(AnnotationServiceDefinition)def;
				mixutureDefinitionTypes+="annotation,";
			}
			if(def instanceof JsonServiceDefinition){
				mixutureDefinitionTypes+="json,";
			}
			if(def instanceof XmlServiceDefinition){
				mixutureDefinitionTypes+="xml,";
			}
		}
		// 混合的服务定义类型名，为：annotation,xml,json
		public String getMixutureDefinitionTypes() {
			if(mixutureDefinitionTypes.endsWith(","))
				return mixutureDefinitionTypes.substring(0,mixutureDefinitionTypes.length()-1);
			return mixutureDefinitionTypes;
		}
		public void copyFrom(IServiceDefinition def){
			//以下条件中设置不同类型服务定义的私有属性
			if(def instanceof AnnotationServiceDefinition){
				AnnotationServiceDefinition asd=(AnnotationServiceDefinition)def;
				this.setAnnotatedClass(asd.getAnnotatedClass());
				this.setAnnotateForm(asd.getAnnotateForm());
			}
			if(def instanceof JsonServiceDefinition){
				JsonServiceDefinition json=(JsonServiceDefinition)def;
			}
			//以下设置共有属性
			this.setServiceDescribeForm(def.getServiceDescribeForm());
			this.setServiceDescriber(def.getServiceDescriber());
			for(TypeDescriber td:def.getExtraDescribers()){
				this.getExtraDescribers().add(td);
			}
			for(ServiceProperty sp:def.getProperties()){
				this.getProperties().add(sp);
			}
			for(ServiceMethod sm:def.getMethods()){
				this.getMethods().add(sm);
			}
			
		}
	}
}
