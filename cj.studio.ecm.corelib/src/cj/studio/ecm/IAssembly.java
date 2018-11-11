package cj.studio.ecm;

//由于重新加载和反复调用开始和停止，会导成内部组件的反复的初始化，不必为此担心。
public interface IAssembly {
	String fileName();
	String home();
	AssemblyState state();
	IAssemblyInfo info();
	/**
	 * 加载
	 */
	void load();
	/**
	 * 重新加载
	 */
	void reload();
	/**
	 * 刷新服务。先停止再启动服务,刷新并不导致类型的加载
	 */
	void refresh();
	/**
	 * 程序集开始服务。start是指刷新服务容器并启动程序集入口点，即：读取服务并装载服务器容，start并不重新加载类型
	 */
	void start();
	/**
	 * 程序集停止服务。仅停止服务，并不卸载类型。
	 */
	void stop();
	/**
	 * 卸载程序集，销毁服务容器并将类加载器置空。卸载后不可再运行
	 */
	void unload();
	/**
	 * 获取零件箱
	 * @return
	 */
	IWorkbin workbin();
	/**
	 * 将指定的程序集设为当前程序集的父
	 * <pre>
	 * 1.子程序集先在本容器搜索服务，如果不存在将在父程序集中搜索服务
	 * 2.如果依赖的程序集未启动，则会被启动，而当前程序集的状态不变。
	 * 约束：
	 * 1.只能设置一个父程序集
	 * 
	 * 注意：对于依赖于外部芯片cj服务Ｔ的芯片内cj服务（Ａ.x），
	 * 在A构造函数中x所指向的T可能还未被在此时注入到x,
	 * 因此构造时x可能还是为空。如果一定要得到x，可以在IServiceAfter方法事件中，
	 * 或非构造的任意方法在被执行时，或在系统运行后
	 * </pre>
	 * @param assembly
	 */
	void parent(Assembly assembly);
	/**
	 * 为程序集提供服务
	 * <pre>
	 * - 所提供的服务将作为程序集的运行时服务被使用
	 * - 此种机制是种懒加载模式，它能在程序集启动时才去获取这些服务
	 * </pre>
	 * @param parent
	 */
	void parent(IServiceProvider parent);
	/**
	 * 依赖程序集，并刷新依赖的程序集和本程序集
	 * <pre>
	 * 被依的程序集必须声明为开放类型才可在子程序集中使用。
	 * 声明方式：
	 * 1.在assembly.json的scans中为package声明为：exoterical:"true"
	 * 2.在java包目录下采用java的package.info文件中声明
	 * 3.声明单独的开放类型，可采用注解、json,xml方式为一个类型声明为开放
	 * 
	 * 优先级：
	 * 1.assembly.json声明为开放，则指定的包开放，如果没声明或声明为false，则采用默认情况
	 * 2.默认情况下，如果在package.info中声明了开放指定包，则开放，否则采用默认情况
	 * 3.默认情况下，如果为指定的类型声明了开放类型，则该指定类型开放
	 * 4.默认情况下类型不开放
	 * 
	 * 
	 * <b>注意：如果依赖的程序集未启动，则会被启动，而当前程序集的状态不变</b>
	 * </pre>
	 * @param assembly
	 */
	void dependency(IAssembly assembly);
	//由于依赖后类型会加载到本地资源中，可以用返射ClassLoader基类和JarClassLoader取出资源的集合，并从中仅移除依赖的资源，解除依赖可留待之后在此添加
	void undependency(Assembly assembly);
	void load(ClassLoader parent);
}
