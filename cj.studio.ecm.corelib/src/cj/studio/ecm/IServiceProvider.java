package cj.studio.ecm;



/**
 * 服务提供者。
 * @author Administrator
 *
 */
//服务容器和站点实现此接口，向组件提供服务支持。
public interface IServiceProvider {
	String $="$";
	String $_cj="$.cj";
	String $_cj_studio="$.cj.studio";
	/**
	 * 网站的jss服务在服务容器中的索引前缀
	 */
	String $_cj_jss_site="$.cj.jss.site";
	/**
	 * 芯片的业务逻辑jss服务在容器中的索引前缀。
	 */
	String $_cj_jss_services="$.cj.jss.services";
	/**
	 * 获取指定类型的服务实现类
	 * <pre>
	 * 返回本程序集内符合类型的服务。
	 * 
	 * 该方法仅用于获取已在容器的类型，未加载入容器的不会被加载
	 * </pre>
	 * @param serviceClazz
	 * @return
	 */
	//此方法只查询服务，而不是申请服务。区别是：查询服务是到容器中查找已存在的服务集合，
	//该方法不会导致新建服务
	public <T> ServiceCollection<T> getServices(Class<T> serviceClazz);
	/**
	 * 获取服务
	 * <pre>
	 * 如果设备了父程序集，且当前程序集中没有服务，则会搜索父程序集中的服务
	 * 
	 * 注意：
	 *   在不同的SCOPE域中可存在同名服务，在singleon域中不能存在同名服务，它具搜索优先级：
	 *   1.优先jss服务搜索，jss服务优先提供了另外一个功能，
	 *   	让jss拦截其它jss服务和java服务，
	 *   	jss服务如果定义为proxy，只需要将要拦截的java对象的方法声明为函数，且第一个参数是proxy。
	 *   	因此jss服务名支持通配符*?
	 *   2.其次singleon
	 *   3.次mutipul
	 *   4.次runtime
	 * </pre>
	 * @param serviceClazz 传入服务类名。
	 * @return 返回服务的实例。
	 */
	//如果不存在请求的服务返回空。对于多例或者request\session等工厂，该方法对服务的请求会导致多例工厂生成新的服务
	//serviceId格式：没有$前缀的请求，会在工厂中搜索是否有包含此ID的服务，如果没有则会在注册表中查找是否存在此服务ID的服务定义，如果有就生成新服务
	//serviceId格式：有$前缀的请求，视为按类型请求服务，与getServices的区别是，$前缀的请求可能会导致服务的生成。
	Object getService(String serviceId);
}

