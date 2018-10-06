package cj.studio.ecm.logging;

import java.util.Map;

//输出器，执行具体的输出实现
public interface IOutter {
	// 输出器所在的单元，系统为system,芯片名，驱动名. 它用于产生文件日志名
	// 用于标明日志记录的身份，是谁的日志
	void setOwner(String owner);

	void print(Object message);

	void print(Object message, Throwable t);

	// 配置的输出器的属性,__owner是芯片名、驱动名或系统名，可用之作为日志名
	void loadProperties(Map<String, String> propMap);

	// 输出器为什么可拷被？比如芯片中想使用系统提供的输出器，比如文件输出器，它的日志名需要被重写。由于可能别的芯片也在使用系统提供的这个输出器，便导致并发混乱，所以各芯片或驱动只能使用系统提供的拷贝
	//一般拷贝属性即可
	IOutter copy();

}
