package cj.studio.ecm.container.describer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import cj.studio.ecm.annotation.MethodMode;
import cj.ultimate.org.objectweb.asm.Type;

public class ServiceMethod {
	// 有多少种方法的注解或XML/JSON的配置注入方式，则定义各个常量枚举
	public static final byte EYEABLE_DESCRIBEFORM = 1;
	public static final byte PARAMETERS_METHOD_DESCRIBEFORM = 2;
	public static final byte RETURN_METHOD_DESCRIBEFORM = 4;
	private byte methodDescribeForm;
	// 方法别名，程序使用此别名注入
	private String alias;
	// 绑定的方法名
	private String bind;
	private String[] parameterTypeNames;
	private MethodMode callMode;
	/**
	 * @uml.property name="propertyDescribers"
	 */
	private List<MethodDescriber> methodDescribers;

	public ServiceMethod() {
		this.methodDescribers = new ArrayList<MethodDescriber>();
	}
	public MethodMode getCallMode() {
		return callMode;
	}
	public void setCallMode(MethodMode callMode) {
		this.callMode = callMode;
	}
	public byte getMethodDescribeForm() {
		return methodDescribeForm;
	}

	public List<MethodDescriber> getMethodDescribers() {
		return methodDescribers;
	}

	public String getAlias() {
		return alias;
	}

	public void setMethodDescribeForm(byte methodDescribeForm) {
		this.methodDescribeForm = methodDescribeForm;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String[] getParameterTypeNames() {
		return parameterTypeNames==null?new String[0]:parameterTypeNames;
	}

	public void setParameterTypeNames(String[] parameterTypeNames) {
		this.parameterTypeNames=parameterTypeNames;
	}

	// 如何判断两个方法是相等的，同名，同参
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ServiceMethod) {
			ServiceMethod method = (ServiceMethod) obj;
			String[] objParms = method.getParameterTypeNames();
			String[] thisParams = this.parameterTypeNames;
			boolean isArgEquals = true;
			if (objParms != thisParams)
				isArgEquals = false;
			if (isArgEquals && ((objParms != null) && (thisParams != null))) {
				for (int i = 0; i < objParms.length; i++) {
					if (objParms[i] != thisParams[i]) {
						isArgEquals = false;
						break;
					}
				}
			}
			if (method.bind.equals(this.bind) && isArgEquals)
				return true;
		}
		return false;
	}

	public String getBind() {
		return bind;
	}

	public void setBind(String bind) {
		this.bind = bind;
	}


	public boolean matched(Method m) {
		if (m.getName().equals(this.alias)) {
			Class<?>[] params = m.getParameterTypes();
			boolean match = true;
			for (int i = 0; i < params.length; i++) {
				Class<?> c = params[i];
				if (!c.getName().equals(parameterTypeNames[i])) {
					match = false;
					break;
				}
			}
			return match;
		}
		return false;
	}

	public boolean matched(String methodName, String desc) {
		if (!methodName.equals(bind))
			return false;
		Type[] argTypes = Type.getArgumentTypes(desc);
		if(argTypes.length!=0&&parameterTypeNames==null)
			return false;
		for (int i = 0; i < argTypes.length; i++) {
			Type t = argTypes[i];
			if (!t.getClassName().equals(parameterTypeNames[i])) {
				return false;
			}
		}
		return true;
	}
@Override
public String toString() {
	String str=this.alias+"(";
	if(this.parameterTypeNames==null||this.parameterTypeNames.length==0)
		return this.alias;
	for(String arg:this.parameterTypeNames){
		str+=arg+",";
	}
	str=str.substring(0,str.length()-1);
	str=str+")";
	return str;
}
}
