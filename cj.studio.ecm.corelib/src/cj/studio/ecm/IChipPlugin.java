package cj.studio.ecm;

import cj.studio.ecm.context.IElement;

/**
 * 芯片插件<br>
 * <pre>
 * 插件是在程序集启动前为程序集插入一些服务。可以用在：
 * - 像对接mybatis的映射文件，让芯片容器直接调用mybatis的获取映射文件接口
 * </pre>
 * @author caroceanjofers
 *
 */
public interface IChipPlugin extends INamedProvider{
	/**
	 * 加载插件
	 * @param ctx
	 * @param args
	 */
	void load(IAssemblyContext ctx, IElement args);

	void unload();
	/**
	 * 插件的服务请求格式是：插件名.服务名。故不能直接以服务名获取到
	 */
	@Override
	Object getService(String serviceId);
}
