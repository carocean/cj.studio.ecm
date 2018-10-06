package cj.ultimate.util;

public class PrimitiveType {
	public static Object convert(Class<?> type,String value){
		Object ret=null;
		if(int.class.equals(type)){
			ret=Integer.valueOf(value);
		}else if(boolean.class.equals(type)){
			ret=Boolean.valueOf(value);
		}else if(byte.class.equals(type)){
			ret=Byte.valueOf(value);
		}else if(short.class.equals(type)){
			ret=Short.valueOf(value);
		}else if(long.class.equals(type)){
			ret=Long.valueOf(value);
		}else if(float.class.equals(type)){
			ret=Float.valueOf(value);
		}else if(double.class.equals(type)){
			ret=Double.valueOf(value);
		}else if(char.class.equals(type)){
			ret=Character.valueOf(value.charAt(0));
		}else{
			ret=type.cast(value);
		}
		return ret;
	}
	public static Class<?> convert(String type,ClassLoader loader) throws ClassNotFoundException{
		Class<?> clazz=null;
		if("int".equals(type)){
			clazz=int.class;
		}else if("boolean".equals(type)){
			clazz=boolean.class;
		}else if("byte".equals(type)){
			clazz=byte.class;
		}else if("short".equals(type)){
			clazz=short.class;
		}else if("long".equals(type)){
			clazz=long.class;
		}else if("float".equals(type)){
			clazz=float.class;
		}else if("double".equals(type)){
			clazz=double.class;
		}else if("char".equals(type)){
			clazz=char.class;
		}else{
			clazz=Class.forName(type,true,loader);
		}
		return clazz;
	}
}
