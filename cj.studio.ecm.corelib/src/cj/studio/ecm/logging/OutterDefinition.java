package cj.studio.ecm.logging;

import java.util.HashMap;
import java.util.Map;

public class OutterDefinition {
	private String outterName;
	private String className;
	private Map<String, String> propMap;
	public OutterDefinition(String outterName,String className) {
		this.outterName=outterName;
		this.className=className;
		propMap=new HashMap<String, String>(0);
	}
	public String getClassName() {
		return className;
	}
	public String getOutterName() {
		return outterName;
	}
	public Map<String, String> getPropMap() {
		return propMap;
	}
}
