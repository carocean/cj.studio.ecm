package cj.studio.ecm.json;

import java.lang.reflect.Type;

import cj.ultimate.gson2.com.google.gson.JsonDeserializationContext;
import cj.ultimate.gson2.com.google.gson.JsonDeserializer;
import cj.ultimate.gson2.com.google.gson.JsonElement;
import cj.ultimate.gson2.com.google.gson.JsonParseException;

public class JsonObjectDeserializer implements JsonDeserializer<JsonElement> {
//	protected JsonContext createJsonContext(Type type) {
//		if (type == JsonContext.class) {
//			return new JsonContext();
//		}
//		return null;
//	}

	@Override
	public JsonElement deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
//		JsonContext ctx = this.createJsonContext(typeOfT);
		// context.deserialize(json, typeOfT);
//		if (json.isJsonObject()) {
//			JsonObject obj = json.getAsJsonObject();
//			System.out.println(obj.get("prop"));
//		}
		return json;
	}

}
