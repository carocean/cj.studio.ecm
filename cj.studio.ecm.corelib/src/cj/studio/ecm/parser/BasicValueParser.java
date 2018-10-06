package cj.studio.ecm.parser;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;

public class BasicValueParser extends ValueParser {
	@Override
	public Object parse(String propName, String value,
			Class<?> targetType, IServiceProvider provider) {
		if(!targetType.isPrimitive()&&!String.class.isAssignableFrom(targetType)){
			throw new EcmException(String.format("解释器%s只支持对JAVA基础类型的解析","cj.basic"));
		}
		if(value==null)return null;
		try{
		if(Integer.TYPE.isAssignableFrom(targetType))
			return Integer.valueOf(value);
		if(String.class.isAssignableFrom(targetType))
			return value;
		if(Byte.TYPE.isAssignableFrom(targetType))
			return Byte.valueOf(value);
		if(Double.TYPE.isAssignableFrom(targetType))
			return Double.valueOf(value);
		if(Float.TYPE.isAssignableFrom(targetType))
			return Float.valueOf(value);
		if(Long.TYPE.isAssignableFrom(targetType))
			return Long.valueOf(value);
		if(Boolean.TYPE.isAssignableFrom(targetType))
			return Boolean.valueOf(value);
		if(Character.TYPE.isAssignableFrom(targetType))
			return value.charAt(0);
		return super.parse(propName, value, targetType, provider);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
}
