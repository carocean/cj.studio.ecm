package cj.studio.ecm.adapter;
/**
 * 命令：它用于代理一个对象的所有方法，委托给适配器的IActuator调用
 * <pre>
 * 用法：
 * · 声明一个适配器服务，实现icommand接口
 * · 在调用该适配器时必须通过  {@link IActuator} 方可被icommand拦截
 * </pre>
 * @author carocean
 *
 */
public interface ICommand {
	Object exeCommand(Object adapter, String method,Class<?>[] argtypes, Object[] args);
}
