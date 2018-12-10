package cj.studio.ecm.net.graph;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.graph.GraphCreator;
import cj.studio.ecm.graph.IGraphCreator;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.layer.INetLayerGraphCreator;
import cj.studio.ecm.net.layer.ISessionManager;

public class GraphCreatorHelper {
	/**
	 * 为指定的端子添加net层
	 * 
	 * <pre>
	 * net层有：
	 * 1.提供会话支持。
	 * 2.如果哪个输入端子需要。
	 *	
	 * </pre>
	 * 
	 * @param creator
	 * @param in
	 */
	public static void attachSessionNetLayer(GraphCreator creator, IPin in) {
		IGraphCreator gc = creator;
		if (!(gc instanceof INetLayerGraphCreator)) {
			throw new EcmException("附加netlayer必须使用INetLayerGraphCreator");
		}
		INetLayerGraphCreator nlgc = (INetLayerGraphCreator) gc;
		ISessionManager sm = nlgc.getSessionManager();
		if (sm == null) {
			throw new EcmException("会话管理器为空。即然使用了netlayer，必须为之分配会话管理器");
		}
		ISink sessionCheck = gc.newSink(INetLayerGraphCreator.KEY_SESSION_SINK);

		// 注意插入顺序：路由层在会话层之上，即：路由层靠过chipgraph,而会话层靠近netgraph
		if (sessionCheck != null)
			in.plugFirst(INetLayerGraphCreator.KEY_SESSION_SINK, sessionCheck);

	}
	/**
	 * 为输出端子附加cookie
	 * <pre>
	 * 如果该输出端子请求远程或其它具有会话功能的graph时，如需要像浏览器那样，自动管理cookie，则需要添加会话容器。
	 * 一个输出端子一个容器，当然也可多输出端子共享一个实例。
	 * 原理是：
	 * 如果发现回路中有Set-Cookie则记录到容器，每次以侦请求地址的域查询有没有对应的cookie，如果有则附加到frame
	 * </pre>
	 * @param creator
	 * @param out
	 */
	public static void attachCookieContainerNetLayer(GraphCreator creator, IPin out) {
		ISink sink = null;
		try {
			sink = creator.newSink(INetLayerGraphCreator.KEY_COOKIE_CONTAINER_SINK);
		} catch (Exception e) {

		}
		if(sink==null){
			sink=new CookieContainer();
		}
		out.plugLast(INetLayerGraphCreator.KEY_COOKIE_CONTAINER_SINK, sink);
	}
}
