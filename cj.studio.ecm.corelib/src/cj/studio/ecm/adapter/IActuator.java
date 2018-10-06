package cj.studio.ecm.adapter;

/**
 * 执行器
 * 
 * <pre></pre>
 * 
 * <h3>用于替代镜子接口的强类型访问方式，可以通过文本命令访问</h3>
 * <ul>
 * <li>功能点</li>
 * </ul>
 * 
 * <pre>
 * //IAdaptable enAdapter = AdaptableFactory.getAdaptableObject(en);
 * // IActuator act = enAdapter.getAdapter(IActuator.class);
 * </pre>
 * 
 * @author C.J 赵向彬 <br>
 *         2012-1-30<br>
 * @see <li>{@link type1 label} <li>{@link type2 label}
 */
// 执行器由适配器对象返回。如：
// IAdaptable enAdapter = AdaptableFactory.getAdaptableObject(en);
// IActuator act = enAdapter.getAdapter(IActuator.class);
public interface IActuator {
	/**
	 * 按参数类型自动匹配到方法，如果匹配不上请用该接口的exactCommand方法以精确匹配到指定方法上。
	 * <pre>
	 *
	 * </pre>
	 * @param actionName
	 * @param args
	 * @return
	 */
	Object exeCommand(String actionName, Object... args);
	/**
	 * 准确匹配方法并执行
	 * <pre>
	 *
	 * </pre>
	 * @param actionName
	 * @param matchArgTypes 准确匹配方法的参数
	 * @param args
	 * @return
	 */
	Object exactCommand(String actionName,Class<?>[] matchArgTypes, Object... args);
}
