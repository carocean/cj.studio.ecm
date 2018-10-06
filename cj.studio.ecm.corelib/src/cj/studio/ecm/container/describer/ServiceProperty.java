package cj.studio.ecm.container.describer;

import java.util.ArrayList;
import java.util.List;

public class ServiceProperty {
	public static final byte SERVICEREF_DESCRIBEFORM = 1;
	public static final byte SERVICEII_DESCRIBEFORM = 2;
	public static final byte SERVICESITE_DESCRIBEFORM = 4;
	//直接注入值，结合${xx}还可以从属性中取值或使用JAVA代码片段，如：
	//${name}+100,$${new MyObject(${age}+1);}超级注入
	public static final byte VALUE_DESCRIBEFORM = 4;
	private byte propDescribeForm;
	private String propName;
	/**
	 * @uml.property name="propertyDescribers"
	 */
	private List<PropertyDescriber> propertyDescribers;
	
	public ServiceProperty() {
		this.propertyDescribers=new ArrayList<PropertyDescriber>();
	}
	public String getPropName() {
		return propName;
	}

	public void setPropName(String propName) {
		this.propName = propName;
	}

	public byte getPropDescribeForm() {
		return propDescribeForm;
	}

	public void setPropDescribeForm(byte propDescribeForm) {
		this.propDescribeForm = propDescribeForm;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ServiceProperty) {
			ServiceProperty prop = (ServiceProperty) obj;
			if (prop.propName.equals(this.propName))
				return true;
		}
		return false;
	}

	
	/**
	 * @uml.property name="propertyDescribers"
	 */
	public List<PropertyDescriber> getPropertyDescribers() {
		return propertyDescribers;
	}

}
