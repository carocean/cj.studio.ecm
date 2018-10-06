package cj.studio.ecm.json;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import cj.ultimate.gson2.com.google.gson.Gson;
import cj.ultimate.gson2.com.google.gson.GsonBuilder;
import cj.ultimate.gson2.com.google.gson.JsonDeserializer;
import cj.ultimate.gson2.com.google.gson.JsonElement;

public class JsonReflector {
	private InputStream stream;

	public JsonReflector(InputStream stream) {
		this.stream = stream;

	}
	public JsonReflector(String file) {
		try {
			stream=new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	protected JsonDeserializer<?> createJsonDeserializer() {
		return new JsonObjectDeserializer();
	}

	public JsonElement getContext() {
		if(stream==null)throw new RuntimeException("程序集未定义上下文，或已损坏");
		JsonDeserializer<?> deser = this.createJsonDeserializer();
		GsonBuilder gb = new GsonBuilder();
		gb = gb.registerTypeAdapter(JsonElement.class, deser);
		Gson gson = gb.create();
		String json;
		try {
			json = this.readJson(stream);
			return gson.fromJson(json, JsonElement.class);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private String readJson(InputStream stream) throws IOException {
		byte[] buffer = new byte[stream.available()];
		stream.read(buffer);
		return new String(buffer);
	}
}
