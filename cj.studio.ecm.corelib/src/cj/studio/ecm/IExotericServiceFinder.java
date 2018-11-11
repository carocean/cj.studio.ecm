package cj.studio.ecm;
/**
 * 搜索外部服务实现
 * <pre>
 * 设有两个程序集a,b,在a中有开放类型ITest,b中Test类实现了ITest;且b.dependency(a)，那么：
 * 在a中可使用IExotericServiceFinder服务搜索b中的服务实现Test
 * </pre>
 * @author caroceanjofers
 *
 */
public interface IExotericServiceFinder {
	/**
	 * 获取程序集b中的外部服务实现
	 * @param clazz
	 * @return
	 */
	<T> ServiceCollection<T> getExotericServices(Class<T> clazz);
	/**
	 * 获取本程序集a中的外部服务实现
	 * @param clazz
	 * @return
	 */
	<T> ServiceCollection<T> getLocalServices(Class<T> clazz);
}
