package cj.studio.ecm.context;

import cj.ultimate.gson2.com.google.gson.JsonElement;

public class ElementGet {

	public static String getJsonProp(JsonElement e) {
		if(e==null||e.isJsonNull()){
			return "";
		}
		if(e.isJsonPrimitive()){
			return e.getAsString();
		}
		return null;
	}
	
}
