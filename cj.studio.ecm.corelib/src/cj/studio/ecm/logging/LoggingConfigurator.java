package cj.studio.ecm.logging;

import java.io.InputStream;
import java.util.Map;

import org.apache.log4j.PropertyConfigurator;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.resource.JarClassLoader;

public class LoggingConfigurator {

	// 系统日志输出器工厂
	private static LoggingOutterFactory systemOutterFactory;
	private static Map<String, String> systemLogMap;
	// 芯片或驱动日志输出器工厂
	private LoggingOutterFactory chipOrDriverOutterFactory;
	private ILogging logging;
	// 说明是谁的日志。如系统，芯片名，驱动名，一般用于生成文件日志时作为其日志文件名
	private String owner;

	public LoggingConfigurator() {
		this("cjsystem");
	}

	public LoggingConfigurator(String owner) {
		this.owner = owner;
	}

	private void configSystem(InputStream in, ClassLoader loader) {
		// 以下是cj日志
		LoggingOutterFactory factory = new LoggingOutterFactory();
		LoggingContext ctx = new LoggingContext(owner);
		try {
			ctx.parse(in, loader);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		factory.parse(ctx);
		logging = new Logging(ctx.getLogMap(), factory);
		systemLogMap = ctx.getLogMap();
		systemOutterFactory = factory;
	}

	private void configChipOrDriver(InputStream in, ClassLoader loader) {

		LoggingOutterFactory factory = new LoggingOutterFactory();
		String demand = CJSystem.current().environment()
				.getProperty("system-config-demand");
		LoggingContext ctx = new LoggingContext(this.owner);
		try {
			ctx.parse(in, loader);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		factory.parse(ctx);
		// 合并输出器，系统级别优先
		for (String oName : systemOutterFactory.enumOutter()) {
			// 如果芯片或驱动中没有而在系统中有的输出器，将添加到芯片或驱动中。如果都存在同名输出器，将采用芯片或驱动的配置
			if (!factory.contains(oName)) {
				// 如果有定义在芯片或驱动中，则应重新创建输出器加载一遍属性，因为可能属性有变化
				IOutter out = systemOutterFactory.getOutter(oName);
				IOutter newout = out.copy();
				newout.setOwner(owner);
				factory.addOutter(oName, newout);
			}
		}
		if ("true".equals(demand)) {
			logging = new Logging(systemLogMap, factory);
		} else {
			logging = new Logging(ctx.getLogMap(), factory);
		}
		chipOrDriverOutterFactory = factory;
	}

	public void config(InputStream in, ClassLoader loader) {
		// 使用log4j日志
		InputStream sf2j = loader
				.getResourceAsStream("cj/properties/log4j.properties");
		if(sf2j==null){
			sf2j = loader
					.getResourceAsStream("log4j.properties");
		}
		if (sf2j != null)
			PropertyConfigurator.configure(sf2j);
		if (loader instanceof JarClassLoader) {
			this.configChipOrDriver(in, loader);
		} else {
			this.configSystem(in, loader);
		}
	}

	public LoggingOutterFactory getLoggingOutterFactory() {
		if (this.chipOrDriverOutterFactory != null)
			return chipOrDriverOutterFactory;
		return systemOutterFactory;
	}

	public ILogging getLogging() {
		return logging;
	}
}
