package cj.studio.ecm.graph;
/**
 * 将芯片图插入到netsite的线板事件
 * <pre>
 * 用于自定义与线板的连接。
 * </pre>
 * @author carocean
 *
 */
public interface IGraphPlugNetboardEvent {

	void plugNetboard(String netName, IPin pin, String method,
			String flow, String rewrite, String to);

}
