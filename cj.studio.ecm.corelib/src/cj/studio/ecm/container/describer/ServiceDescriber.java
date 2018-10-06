package cj.studio.ecm.container.describer;

import cj.studio.ecm.Scope;


public class ServiceDescriber extends TypeDescriber {
	/**
	 * @uml.property name="scope"
	 */
	private Scope scope;
	/**
	 * ddd
	 * 
	 * @uml.property name="className"
	 */
	private String className;
	private String constructor;
	private String description;
	private boolean isExoteric;
	//什么叫唯一？想想一下在同一个包吧，你能定义两个类全名相同的类吗？肯定不能，所以是否相等，不是取决于ID，而是取决于类型。而ID的重复与否，交由命名策略（服务定义名称产生器）决定。
//	@Override
//	public boolean equals(Object obj) {
//		if(obj instanceof ServiceDescriber){
//			ServiceDescriber des=(ServiceDescriber)obj;
//			return des.className.equals(this.className);
//		}
//		return super.equals(obj);
//	}
	/**
	 * @uml.property name="scope"
	 */
	public Scope getScope() {
		return scope;
	}

	/**
	 * @uml.property name="scope"
	 */
	public void setScope(Scope scope) {
		this.scope = scope;
	}

	/**
	 * @uml.property name="className"
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * @uml.property name="className"
	 */
	public void setClassName(String className) {
		this.className = className;
	}

	/**
	 * @uml.property name="serviceId"
	 */
	private String serviceId;

	/**
	 * @uml.property name="serviceId"
	 */
	public String getServiceId() {
		return serviceId;
	}

	/**
	 * @uml.property name="serviceId"
	 */
	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public boolean isExoteric() {
		return isExoteric;
	}
	public void setExoteric(boolean isExoteric) {
		this.isExoteric = isExoteric;
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "serviceId:"+serviceId+",className:"+className+",scope:"+scope+"isExoteric:"+isExoteric+",description:"+description;
	}
	public String getConstructor() {
		return constructor;
	}
	public void setConstructor(String constructor) {
		this.constructor = constructor;
	}
}
