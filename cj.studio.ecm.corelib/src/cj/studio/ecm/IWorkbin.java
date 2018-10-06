package cj.studio.ecm;

import java.util.List;


/**
 * 零件盒用于向程序集外提供元件，这些元件包括：<br>
 * 主板、芯片、端子、总线描述器。
 * 
 * @author Administrator
 * 
 */
public interface IWorkbin {
	/**
	 * 获取部件，一般是开放服务
	 * @param name
	 * @return
	 */
	Object part(String name);
	<T> ServiceCollection<T> part(Class<T> type);
	/**
	 * 获取外部类型
	 * <pre>
	 *－如果参数为空表示获取全部外部类型。
	 * </pre>
	 * @param typeName
	 * @return
	 */
	List<Class<?>> exotericalType(String typeName);
	IChipInfo chipInfo();
	/**
	 * 列出在net.properties配置的属性
	 * <pre>
	 *
	 * </pre>
	 * @return
	 */
	public String[] enumProperty();
	public String getProperty(String key);
}
