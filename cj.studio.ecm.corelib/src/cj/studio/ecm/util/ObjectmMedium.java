package cj.studio.ecm.util;

import java.util.HashMap;
import java.util.Map;
//对象中介者，用于从一个封闭对象引出不可见的属性，从而使创建它的对象能够改变其属性
public class ObjectmMedium {
	
	private Map<String, Object> map;
	public ObjectmMedium() {
		map=new HashMap<String, Object>();
	}
	public void set(String propName,Object value){
		if(contains(propName))
			throw new RuntimeException("已存在属性："+propName);
		map.put(propName, value);
	}
	public Object get(String propName){
		return map.get(propName);
	}
	public boolean contains(String propName){
		return map.containsKey(propName);
	}
	public void clear(){
		map.clear();
	}
}
