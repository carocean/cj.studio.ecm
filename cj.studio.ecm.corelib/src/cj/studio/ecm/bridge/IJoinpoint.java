package cj.studio.ecm.bridge;

import java.lang.reflect.Method;

import cj.studio.ecm.IServiceProvider;
import cj.ultimate.net.sf.cglib.proxy.MethodProxy;
/**
 * 连接点是在桥（服务）和切面间进行连接的装置
 * <pre>
 * －一个桥拥有一个连接点
 * －一个连接点通过切入点（ICutpoint）连接多个方面，切入点的方面集合具有先后次序，支持计算符：+,-
 * －方面是一种用于拦截服务的代理实现
 * </pre>
 * @author carocean
 *
 */
//连接点是在桥、服务和切面间进行连接的装置
public interface IJoinpoint {
	Object cut(Object bridge, Method method, MethodProxy proxy, Object[] args);

	void init(String aspects, IServiceProvider provider);

	// 复制当前连接点、增加新的方面并生成新的连接点
	IJoinpoint builtNewJoinpoint(String aspects);
	Object getService();
	// 返回连接点包含的所有切入接口
	Class<?>[] getCutInterfaces();

	
}
