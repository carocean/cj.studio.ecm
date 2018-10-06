package cj.studio.ecm.graph;
/**
 * 服务提供器返回的一类结果
 * <pre>
 * 
 * </pre>
 * @author carocean
 *
 */
public interface IResult {
	/**
	 * 200 表示成功，非200表示不成功，具体值代表某种错误。
	 * <pre>
	 *
	 * </pre>
	 * @return
	 */
	int state();
	/**
	 * 状态信息。
	 * <pre>
	 *
	 * </pre>
	 * @return
	 */
	String message();
	/**
	 * 服务
	 * <pre>
	 *
	 * </pre>
	 * @return
	 */
	Object value();
}
