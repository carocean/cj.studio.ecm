package cj.studio.ecm.container.resolver;

import java.lang.reflect.Type;

import cj.studio.ecm.json.JsonObjectDeserializer;
import cj.ultimate.gson2.com.google.gson.JsonDeserializationContext;
import cj.ultimate.gson2.com.google.gson.JsonElement;
import cj.ultimate.gson2.com.google.gson.JsonParseException;

public class JsonServiceDeserializer extends JsonObjectDeserializer {
	@Override
	public JsonElement deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		// TODO Auto-generated method stub
		return super.deserialize(json, typeOfT, context);
	}
	
}
