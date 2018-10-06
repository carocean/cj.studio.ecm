package cj.studio.ecm.logging;

import java.util.HashMap;
import java.util.Map;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;

public class Logging implements ILogging {
	private LoggingOutterFactory factory;
	// key=outterName,interge=level[fatal0,error1,warn2,info3,debug4,trace5]
	private Map<String, Integer> outlevelMap;
	public Logging(Map<String, String> logMap, LoggingOutterFactory factory) {
		this.factory = factory;
		outlevelMap = new HashMap<String, Integer>(6);
		for (String outName : logMap.keySet()) {
			String level = logMap.get(outName);
			if ("fatal".equalsIgnoreCase(level)) {
				outlevelMap.put(outName, 0);
			} else if ("error".equalsIgnoreCase(level)) {
				outlevelMap.put(outName, 1);
			} else if ("warn".equalsIgnoreCase(level)) {
				outlevelMap.put(outName, 2);
			} else if ("info".equalsIgnoreCase(level)) {
				outlevelMap.put(outName, 3);
			} else if ("debug".equalsIgnoreCase(level)) {
				outlevelMap.put(outName, 4);
			} else if ("trace".equalsIgnoreCase(level)) {
				outlevelMap.put(outName, 5);
			} else {
				throw new EcmException(
						String.format("不认识的级别%s，日志支持的级别有：fatal,error,warn,info,debug,trace"));
			}
		}
	}

	@Override
	public void fatal(Object message) {
		for (String outName : outlevelMap.keySet()) {
			int level = outlevelMap.get(outName);
			if (level >= 0) {
				IOutter out = factory.getOutter(outName);
				if (out == null)
					throw new EcmException(String.format("输出器%s不存在",
							outName));
				String prex = CJSystem.current().environment().language().lookup("Logging.fatal");
				out.print(prex + " : " + message);
			}
		}
	}

	@Override
	public void error(Class<?> clazz, Throwable t) {
		detail("Logging.error", clazz, "", t);

	}

	private void detail(String logLevel, Class<?> clazz, Object msg, Throwable t) {
		for (String outName : outlevelMap.keySet()) {
			int level = outlevelMap.get(outName);
			if (level >= 1) {
				IOutter out = factory.getOutter(outName);
				if (out == null)
					throw new EcmException(String.format("输出器%s不存在",
							outName));
				String prex = CJSystem.current().environment().language().lookup(logLevel);
				if (t != null) {
					out.print(String.format("%s: %s - %s 异常：%s", prex,
							clazz == null ? "cj.studio.ecm" : clazz.getName(),
							msg, t));
					printCurrent(out, t);
					if (t.getCause() != null) {
						out.print("\tCause by:");
						printCause(out, t.getCause());
					}
				} else {
					out.print(String.format("%s: %s - %s", prex,
							clazz == null ? "cj.studio.ecm" : clazz.getName(),
							msg));
					printCurrent(out, t);
				}
			}
		}
	}

	private void printCurrent(IOutter out, Throwable t) {
		if (t == null)
			return;
		StackTraceElement[] arr = t.getStackTrace();
		for (StackTraceElement se : arr) {
			out.print(String.format("\t\tat %s.%s(%s:%s)", se.getClassName(),
					se.getMethodName(), se.getFileName(), se.getLineNumber()));
		}
	}

	private void printCause(IOutter out, Throwable t) {
		StackTraceElement[] arr = t.getStackTrace();
		for (StackTraceElement se : arr) {
			out.print(String.format("\t\tat %s.%s(%s:%s)", se.getClassName(),
					se.getMethodName(), se.getFileName(), se.getLineNumber()));
		}
		if (t.getCause() != null) {
			printCause(out, t.getCause());
		}
	}

	public void error(Class<?> clazz, Object message) {
		detail("Logging.error", clazz, message, null);
	}

	@Override
	public void info(Class<?> clazz, Object message) {
		for (String outName : outlevelMap.keySet()) {
			int level = outlevelMap.get(outName);
			if (level >= 1) {
				IOutter out = factory.getOutter(outName);
				if (out == null)
					throw new EcmException(String.format("输出器%s不存在", outName));
				String prex = CJSystem.current().environment().language().lookup("Logging.info");
				out.print(String.format("%s: %s - %s", prex,
						clazz == null ? "cj.studio.ecm" : clazz.getName(),
						message));
			}
		}

	}

	@Override
	public void error(Object message) {
		error(null, message);
	}

	@Override
	public void warn(Object message) {
		for (String outName : outlevelMap.keySet()) {
			int level = outlevelMap.get(outName);
			if (level >= 2) {
				IOutter out = factory.getOutter(outName);
				if (out == null)
					throw new EcmException(String.format("输出器%s不存在",
							outName));
				String prex = CJSystem.current().environment().language().lookup("Logging.warn");
				out.print(prex + " : " + message);
			}
		}
	}
	@Override
	public void warn(Class<?> source, Object message) {
		detail("Logging.warn", source, message, null);
		
	}
	@Override
	public void warn(Class<?> source, Throwable t) {
		detail("Logging.warn", source, "", t);
	}
	@Override
	public void info(Object message) {
		for (String outName : outlevelMap.keySet()) {
			int level = outlevelMap.get(outName);
			if (level >= 3) {
				IOutter out = factory.getOutter(outName);
				if (out == null)
					throw new EcmException(String.format("输出器%s不存在",
							outName));
				String prex = CJSystem.current().environment().language().lookup("Logging.info");
				out.print(prex + " : " + message);
			}
		}
	}

	@Override
	public void debug(Object message) {
		for (String outName : outlevelMap.keySet()) {
			int level = outlevelMap.get(outName);
			if (level >= 4) {
				IOutter out = factory.getOutter(outName);
				if (out == null)
					throw new EcmException(String.format("输出器%s不存在",
							outName));
				String prex = CJSystem.current().environment().language().lookup("Logging.debug");
				out.print(prex + " : " + message);
			}
		}
	}

	@Override
	public void debug(Class<?> clazz, Object message) {
		for (String outName : outlevelMap.keySet()) {
			int level = outlevelMap.get(outName);
			if (level >= 4) {
				IOutter out = factory.getOutter(outName);
				if (out == null)
					throw new EcmException(String.format("输出器%s不存在",
							outName));
				String prex = CJSystem.current().environment().language().lookup("Logging.debug");
				out.print(String.format("%s: %s - %s", prex,
						clazz == null ? "cj.studio.ecm" : clazz.getName(),
						message));
			}
		}
	}

	@Override
	public void trace(Object message) {
		for (String outName : outlevelMap.keySet()) {
			int level = outlevelMap.get(outName);
			if (level >= 5) {
				IOutter out = factory.getOutter(outName);
				if (out == null)
					throw new EcmException(String.format("输出器%s不存在",
							outName));
				String prex = CJSystem.current().environment().language().lookup("Logging.trace");
				out.print(prex + " : " + message);
			}
		}
	}

	@Override
	public void fatal(Object message, Throwable t) {
		for (String outName : outlevelMap.keySet()) {
			int level = outlevelMap.get(outName);
			if (level >= 0) {
				IOutter out = factory.getOutter(outName);
				if (out == null)
					throw new EcmException(String.format("输出器%s不存在",
							outName));
				String prex = CJSystem.current().environment().language().lookup("Logging.fatal");
				out.print(prex + " : " + message, t);
			}
		}
	}

	@Override
	public void error(Object message, Throwable t) {
		detail("Logging.error", null, message, t);
	}

	@Override
	public void warn(Object message, Throwable t) {
		for (String outName : outlevelMap.keySet()) {
			int level = outlevelMap.get(outName);
			if (level >= 2) {
				IOutter out = factory.getOutter(outName);
				if (out == null)
					throw new EcmException(String.format("输出器%s不存在",
							outName));
				String prex = CJSystem.current().environment().language().lookup("Logging.warn");
				out.print(prex + " : " + message, t);
			}
		}
	}

	@Override
	public void info(Object message, Throwable t) {
		for (String outName : outlevelMap.keySet()) {
			int level = outlevelMap.get(outName);
			if (level >= 3) {
				IOutter out = factory.getOutter(outName);
				if (out == null)
					throw new EcmException(String.format("输出器%s不存在",
							outName));
				String prex = CJSystem.current().environment().language().lookup("Logging.info");
				out.print(prex + " : " + message, t);
			}
		}
	}

	@Override
	public void debug(Object message, Throwable t) {
		for (String outName : outlevelMap.keySet()) {
			int level = outlevelMap.get(outName);
			if (level >= 4) {
				IOutter out = factory.getOutter(outName);
				if (out == null)
					throw new EcmException(String.format("输出器%s不存在",
							outName));
				String prex = CJSystem.current().environment().language().lookup("Logging.debug");
				out.print(prex + " : " + message, t);
			}
		}
	}

	@Override
	public void trace(Object message, Throwable t) {
		for (String outName : outlevelMap.keySet()) {
			int level = outlevelMap.get(outName);
			if (level >= 5) {
				IOutter out = factory.getOutter(outName);
				if (out == null)
					throw new EcmException(String.format("输出器%s不存在",
							outName));
				String prex = CJSystem.current().environment().language().lookup("Logging.trace");
				out.print(prex + " : " + message, t);
			}
		}
	}

	@Override
	public boolean isDebugEnabled() {
		boolean result = false;
		for (String outName : outlevelMap.keySet()) {
			int level = outlevelMap.get(outName);
			if (level >= 4) {
				result = true;
				break;
			}
		}
		return result;
	}

	@Override
	public boolean isErrorEnabled() {
		boolean result = false;
		for (String outName : outlevelMap.keySet()) {
			int level = outlevelMap.get(outName);
			if (level >= 1) {
				result = true;
				break;
			}
		}
		return result;
	}

	@Override
	public boolean isFatalEnabled() {
		boolean result = false;
		for (String outName : outlevelMap.keySet()) {
			int level = outlevelMap.get(outName);
			if (level >= 0) {
				result = true;
				break;
			}
		}
		return result;
	}

	@Override
	public boolean isInfoEnabled() {
		boolean result = false;
		for (String outName : outlevelMap.keySet()) {
			int level = outlevelMap.get(outName);
			if (level >= 3) {
				result = true;
				break;
			}
		}
		return result;
	}

	@Override
	public boolean isWarnEnabled() {
		boolean result = false;
		for (String outName : outlevelMap.keySet()) {
			int level = outlevelMap.get(outName);
			if (level >= 2) {
				result = true;
				break;
			}
		}
		return result;
	}

	@Override
	public boolean isTraceEnabled() {
		boolean result = false;
		for (String outName : outlevelMap.keySet()) {
			int level = outlevelMap.get(outName);
			if (level >= 5) {
				result = true;
				break;
			}
		}
		return result;
	}

}
