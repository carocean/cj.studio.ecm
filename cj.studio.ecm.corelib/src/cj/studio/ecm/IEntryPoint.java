package cj.studio.ecm;
/**
 * 模块的入口点，它管理着激活器。<br>
 * 实现者为芯片和主板
 * @author Administrator
 *
 */
public interface IEntryPoint extends INamedProvider{
	/**
	 * 开始。<br>
	 * 1.如果已是激活状，则不做工作 <br>
	 * 2.使输入输出处于可用状态 <br>
	 * 3.启动Activator.activate方法，系统的activator用于注解端子的装配，自定义激活器可以实现端子的自装配 <br>
	 * 4.状态改为已激活ACTIVE
	 */
	public abstract void start(IAssemblyContext ctx);

	/**
	 * 停止。<br>
	 * 1.如果是停止则不工作 <br>
	 * 2.停止输入输出 <br>
	 * 3.调用Activators的inactivate方法 <br>
	 * 3.状态改为inactive状态
	 */
	public abstract void stop(IAssemblyContext ctx);

	public abstract void load(IAssemblyContext assemblyContext);

}