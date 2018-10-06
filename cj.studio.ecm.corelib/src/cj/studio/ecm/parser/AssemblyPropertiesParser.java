package cj.studio.ecm.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IChip;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.resource.IResource;

public class AssemblyPropertiesParser extends BasicValueParser {
	@Override
	public Object parse(String propName, String value, Class<?> targetType,
			IServiceProvider provider) {
		IResource res = provider.getServices(IResource.class).get(0);
		IChip chip = (IChip) provider.getService(IChip.class.getName());
		String home = chip.site().getProperty("home.dir");
		File h = new File(String.format("%s/properties/Assembly.json", home));
		InputStream in = null;
		if (h.exists()) {
			try {
				in = new FileInputStream(h);
			} catch (FileNotFoundException e) {
			}
		}

		try {
			if (in != null) {
				in = res.getResourceAsStream("cj/properties/Assembly.properties");
				if (in == null) {
					CJSystem.current()
							.environment()
							.logging()
							.info("没有为程序集指定属性文件：cj/properties/Assembly.properties");
					return null;
				}
			}
			Properties p = new Properties();

			p.load(in);
			String v = p.getProperty(value);
			return super.parse(propName, v, targetType, provider);
		} catch (IOException e) {
			CJSystem.current().environment().logging()
					.info("错误：" + e.getMessage());
			throw new RuntimeException(e);
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

	}
}
