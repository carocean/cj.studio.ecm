package cj.studio.ecm.container.resolver;

import java.io.InputStream;

import cj.studio.ecm.json.JsonReflector;
import cj.ultimate.gson2.com.google.gson.JsonDeserializer;

public class JsonServiceReflector extends JsonReflector {

	public JsonServiceReflector(InputStream stream) {
		super(stream);
		// TODO Auto-generated constructor stub
	}

	public JsonServiceReflector(String file) {
		super(file);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected JsonDeserializer createJsonDeserializer() {
		// TODO Auto-generated method stub
		return new JsonServiceDeserializer();
	}
}
