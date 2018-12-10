package cj.studio.ecm.graph;

import java.lang.reflect.Field;

import cj.ultimate.util.StringUtil;

/**
 * 派生类中定义协议
 * <pre>
 * 协议定义为字段
 * 使用cjstatusDef注解
 * </pre>
 * @author carocean
 *
 */
public final class AnnotationProtocolFactory implements IProtocolFactory {
	private Class<? extends IConstans> constans;
	private AnnotationProtocolFactory(Class<? extends IConstans> constans2) {
		constans=constans2;
	}
	public static IProtocolFactory factory(Class<? extends IConstans> constans){
		IProtocolFactory factory=new AnnotationProtocolFactory(constans);
		if(StringUtil.isEmpty(factory.get("protocol")))
			throw new RuntimeException("未指定协议，需指定protocol在"+constans.getName());
		return factory;
	}
	@Override
	public String getProtocol() {
		return get("protocol");
	}
	@Override
	public CircuitException throwAt(String status, Object... error) {
		return CircuitException.throwAt(this, status, error);
	}
	@Override
	public  final String get(String status) {
		if(constans==null)
			throw new RuntimeException("缺少常量表");
		return get(constans,status);
	}
	private  String get(Class<?> factory,String status) {
		Field[] arr= factory.getDeclaredFields();
		for(Field f:arr){
			CjStatusDef e=f.getAnnotation(CjStatusDef.class);
			if(e==null)continue;
			f.setAccessible(true);
			Object v;
			try {
				v = f.get(this);
				try{
				v=(String)v;
				}catch(Exception er){
					throw new RuntimeException("未将错误代码声明为字符串类型."+this.getClass().getName()+"."+f.getName());
				}
				if(v.equals(status)){
					return e.message();
				}
			} catch (Exception e1) {
				throw new RuntimeException(e1);
			} 
			
		}
		Class<?> sup=factory.getSuperclass();
		if(!sup.equals(Object.class)&&IConstans.class.isAssignableFrom(sup)){
			return get(sup,status);
		}
		return "";
	}
}
