package cj.studio.ecm.parser;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.ultimate.util.StringUtil;

public class ValueParserFactory implements IValueParserFactory {
	private IServiceProvider provider;

	public ValueParserFactory(IServiceProvider provider) {
		this.provider = provider;
	}

	@Override
	public IValueParser getValueParser(String simpleName) {
		if (StringUtil.isEmpty(simpleName))
			throw new RuntimeException("parser can not null");
		IValueParser vp = null;
		if ("cj.basic".equals(simpleName))
			vp = new BasicValueParser();
		if ("cj.propfile".equals(simpleName))
			vp = new AssemblyPropertiesParser();
		if (vp == null)
			if ("cj.jsonList".equals(simpleName))
				vp = new JsonListValueParser();
		if (vp == null)
			if ("cj.jsonMap".equals(simpleName))
				vp = new JsonMapValueParser();
		if (vp == null)
			vp = (IValueParser) provider.getService(simpleName);
		if (vp == null)
			throw new EcmException(String.format("不支持指定的parser:%s",
					simpleName));
		return vp;
	}

	@Override
	public void dispose() {
		this.provider = null;
	}
}
