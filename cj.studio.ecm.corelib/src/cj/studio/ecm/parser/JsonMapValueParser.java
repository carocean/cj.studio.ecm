package cj.studio.ecm.parser;

import java.util.HashMap;
import java.util.Map;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.ultimate.gson2.com.google.gson.Gson;

public class JsonMapValueParser extends ValueParser {
	@SuppressWarnings("unchecked")
	@Override
	public Object parse(String propName, String value, Class<?> targetType,
			IServiceProvider provider) {
		if (!Map.class.isAssignableFrom(targetType))
			throw new EcmException(String.format("属性%s的类型非Map", propName));
		Map<String,String> map = new HashMap<String,String>();
		Gson gson = new Gson();
		map = gson.fromJson(value, HashMap.class);
		Map<String,Object> newMap=new HashMap<String, Object>();
		for(String key:map.keySet()){
			String v=map.get(key);
			if(v.contains("$.")){
				String ref=v.substring(2,v.length());
				Object service=provider.getService(ref);
				if(service==null)
					throw new RuntimeException(String.format("引用的服务%s不存在",ref));
				newMap.put(key, service);
			}else{
				newMap.put(key, v);
			}
		}
		return newMap;
	}
}
