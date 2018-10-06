package cj.studio.ecm;

import java.io.InputStream;

import cj.studio.ecm.logging.ILogging;

//管理芯片实例，系统运行，退出，系统的芯片
//管理驱动类装载器
//该类是静态类，在同一进程内收集芯片
//或许可以返回运行时接口，以期实现对系统的监控信息
//系统事件，如芯片加载、退出、驱动的加载退出等事件
//芯片共用组件、芯片名唯一性校验等功能
public final class CJSystem {
	private Environment environment;
	private static CJSystem cjsystem;
	private static String environmentFile="cj/studio/ecm/environment.xml";
	static {
		InputStream in= CJSystem.class.getClassLoader().getResourceAsStream(environmentFile);
		loadEnvironment(in);
	}
	public static CJSystem current() {
		return cjsystem;
	}
	public static ILogging logging() {
		return cjsystem.environment.logging();
	}
	//加载运行时环境上下文
	private static void loadEnvironment(InputStream in){
		cjsystem = new CJSystem();
		Environment en=new Environment(in);
		cjsystem.environment=en;
	}

	public Environment environment() {
		return environment;
	}
	public static int getChipCount() {
		return 0;
	}

	//如果为空则在cj/studio/ecm/environment.xml中查找，以默认环境运行
	public static void run(String environmentFile) {
		
	}

}
