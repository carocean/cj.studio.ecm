package cj.studio.ecm.container.describer;

public class ServicePropertyValueDescriber extends PropertyDescriber{
	private String value;
	//value的格式器，可以是JAVA简单值，可以是map,list等，系统提供一套方案，芯片开发者可扩展格式
	private String parser;
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getParser() {
		return parser;
	}
	public void setParser(String parser) {
		this.parser = parser;
	}
}
