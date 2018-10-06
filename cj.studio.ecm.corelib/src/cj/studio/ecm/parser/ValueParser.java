package cj.studio.ecm.parser;

import cj.studio.ecm.IServiceProvider;

public abstract class ValueParser implements IValueParser {


	@Override
	public Object parse(String propName, String value, Class<?> targetType, IServiceProvider provider) {
		// TODO Auto-generated method stub
		return value;
	}

}
