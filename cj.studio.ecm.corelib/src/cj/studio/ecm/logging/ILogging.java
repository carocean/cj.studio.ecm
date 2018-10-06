package cj.studio.ecm.logging;
//拥有一输出器工厂
//如果同一芯片或驱动启用多个实例，则其日志文件仅记录在同名的同一份日志中
public interface ILogging {
	//按顺序为严重级别，级别低的输出该级别以上所有消息
	void fatal(Object message);
	void warn(Object message);
	void info(Object message); 
	void info(Class<?> source,Object message); 
	void debug(Object message);
	void debug(Class<?> source,Object message); 
	void trace(Object message);
	
	void fatal(Object message,Throwable t);
	void error(Object message,Throwable t);
	void error(Class<?> source,Object message);
	void error(Class<?> source,Throwable t);
	void error(Object message);
	
	void warn(Object message,Throwable t);
	void warn(Class<?> source,Object message);
	void warn(Class<?> source,Throwable t);
	void info(Object message,Throwable t);
	void debug(Object message,Throwable t);
	void trace(Object message, Throwable t);
	
	boolean isDebugEnabled() ;
	boolean isErrorEnabled() ;
	boolean isFatalEnabled()  ;
	boolean isInfoEnabled() ;
	boolean isWarnEnabled() ;
	boolean isTraceEnabled();
	
}
