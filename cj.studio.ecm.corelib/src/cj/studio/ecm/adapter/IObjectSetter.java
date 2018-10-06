package cj.studio.ecm.adapter;


/**
 * 
 * @author Administrator
 *
 */
//由于为代理的属性赋值无法关联到代理目标，所以通过属性设置器为代理的属性赋值
public interface IObjectSetter {
	void set(String fieldName,Object value);
	Object get(String fieldName);
}
