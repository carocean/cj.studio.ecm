package cj.studio.ecm.logging;

import java.util.HashMap;
import java.util.Map;

public class LoggingOutterFactory {
	private Map<String, IOutter> outterMap;
	public LoggingOutterFactory() {

	}

	public void parse(LoggingContext ctx) {
		outterMap = new HashMap<String, IOutter>(0);
		Map<String, OutterDefinition> map = ctx.getOutterMap();
		try {
			for (String key : map.keySet()) {
				OutterDefinition od = map.get(key);
				Class<?> clazz = Class.forName(od.getClassName(), true,
						ctx.getLoader());
				IOutter out=(IOutter)clazz.newInstance();
				out.setOwner(ctx.getOwner());
				out.loadProperties(od.getPropMap());
				outterMap.put(od.getOutterName(), out);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	public boolean contains(String name) {
		return outterMap.containsKey(name);
	}

	public String[] enumOutter() {
		return outterMap.keySet().toArray(new String[0]);
	}

	public IOutter getOutter(String name) {
		return outterMap.get(name);
	}

	public void addOutter(String name, IOutter outter) {
		outterMap.put(name, outter);
	}

	
}
