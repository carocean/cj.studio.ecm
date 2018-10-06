package cj.studio.ecm.parser;

import cj.ultimate.IDisposable;

//值解析器工厂,收集系统和芯片中的值解析器
public interface IValueParserFactory extends IDisposable{
	IValueParser getValueParser(String simpleName);
}
