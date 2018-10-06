package cj.studio.ecm.parser;

import java.util.ArrayList;
import java.util.List;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.ultimate.gson2.com.google.gson.Gson;

public class JsonListValueParser extends ValueParser {
	@SuppressWarnings("unchecked")
	@Override
	public Object parse(String propName, String value,
			Class<?> targetType, IServiceProvider provider) {
		if(!List.class.isAssignableFrom(targetType))
			throw new EcmException(String.format("属性%s的类型非List",propName));
		List<String> list=new ArrayList<String>();
		Gson gson=new Gson();
		list=gson.fromJson(value, ArrayList.class);
		List<Object> newList=new ArrayList<Object>();
		for(String v:list){
			if(v.contains("$.")){
				String ref=v.substring(2,v.length());
				Object service=provider.getService(ref);
				if(service==null)
					throw new RuntimeException(String.format("引用的服务%s不存在",ref));
				newList.add(service);
			}else{
				newList.add(v);
			}
		}
		return newList;
	}
}
