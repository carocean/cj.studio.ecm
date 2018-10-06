package cj.studio.ecm.container.registry;

import java.util.ArrayList;
import java.util.List;

import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.container.describer.ServiceDescriber;
import cj.studio.ecm.container.describer.ServiceMethod;
import cj.studio.ecm.container.describer.ServiceProperty;
import cj.studio.ecm.container.describer.TypeDescriber;

public abstract class ServiceDefinition implements IServiceDefinition {

	private byte serviceForm;
	private List<ServiceProperty> propertyRefs;
	private List<TypeDescriber> extraDescriber;
	private ServiceDescriber serviceDescriber;
	private List<ServiceMethod> serviceMethods;

	public ServiceDefinition() {
		this.propertyRefs = new ArrayList<ServiceProperty>();
		this.extraDescriber = new ArrayList<TypeDescriber>();
		this.serviceMethods = new ArrayList<ServiceMethod>();
	}

	//什么叫唯一？想想一下在同一个包吧，你能定义两个类全名相同的类吗？肯定不能，所以是否相等，不是取决于ID，而是取决于类型。而ID的重复与否，交由命名策略（服务定义名称产生器）决定。
	//服务容器中注册表是以ID为标识的，因为ID作为标识由命名策略决定是否重复。也就是一个类可以有多个服务定义
	//再由不同的ID产生不同的实例，即，在不同的服务定义下所注入的元素就有不同。无法断定在应用场景种不会遇到这种需求。
	//合并器仅用于将不同的解析器所解析的连续的服务定义和元数据合并在一起，它不是用来合并所有相同类型的服务定义的。
//	@Override
//	public boolean equals(Object obj) {
//		boolean eq=false;
//		if(obj instanceof ServiceDefinition){
//			ServiceDefinition def=(ServiceDefinition)obj;
//			if(def.getServiceDescriber()!=null){
//				eq=def.getServiceDescriber().equals(this.serviceDescriber);
//			}
//		}
//		return eq;
//	}

	@Override
	public byte getServiceDescribeForm() {
		return serviceForm;
	}

	@Override
	public void setServiceDescribeForm(byte form) {
		// TODO Auto-generated method stub
		this.serviceForm = form;
	}

	@Override
	public List<ServiceProperty> getProperties() {
		// TODO Auto-generated method stub
		return propertyRefs;
	}

	@Override
	public List<TypeDescriber> getExtraDescribers() {
		// TODO Auto-generated method stub
		return extraDescriber;
	}

	@Override
	public List<ServiceMethod> getMethods() {
		// TODO Auto-generated method stub
		return serviceMethods;
	}

	@Override
	public ServiceDescriber getServiceDescriber() {
		// TODO Auto-generated method stub
		return serviceDescriber;
	}

	@Override
	public void setServiceDescriber(ServiceDescriber serviceDescriber) {
		this.serviceDescriber = serviceDescriber;
	}
	@Override
	public String toString() {
		if(this.getServiceDescriber()!=null)
			return this.getServiceDescriber()+"[form:"+this.serviceForm+"]";
		return super.toString();
	}
}
