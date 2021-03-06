package cj.studio.ecm.bridge;

/**
 * 方面用于让调用者拦截服务对象的方法
 * 
 * <pre>
 * －－一个服务对象是一个桥，服务对象拥有连接点，连接点收集了所在服务对象定义的方面，方面是服务对象不同的使用方式，如同一个人多张脸。
 *    
 * －方面，表示拦截的方法的一个处理逻辑，比如方法前方法后逻辑及下一方面如何处理等
 * －方面是中立的，它只关注于切入的接口类型进行处理，并不关注于某个特定服务。 
 * －方面首先关注于接口的处理，其次决定如何剪切接口的某些方法
 * －方面还可以指定自动作用于哪些服务
 * －方面切入服务的所有方法(包括Object的方法），开发者自行拦截和过滤
 * 
 * 注意：方面在桥中声明的顺序，它以职责链模式模拟一个操作方法的执行序，每个方面没有直接执行方法的权限，
 *      系统为每条切入点线的末尾插入了执行方面，只有执行到链的末尾被代理的方法才会被执行,这是因为：
 *      －如果方面中设置了执行真实方法的api，假设此时方面a执行了方法，然后传给其后方面b，而其后方面并不知道该方法是否执行过（不同的开发者）
 *        b也执行了这个方法，则此时一个方法被代理，却被执行莫名的次数，这会造成方法执行序的混乱，因此：
 *        采用最尾执行原则，要想执行方法必须向后个方面传递。
 *        举个实体的例子：
 *        欲为服务s加上事务和日志，由于日志也要记录事务作用下的异常情况，则：
 *        1.日志方面必须放在事务方面前面
 *        2.日志－》事务－》执行器方面
 *      －系统方法不被拦截，有：getAdapter,getBridge
 * </pre>
 * 
 * @author carocean
 *
 */
public interface IAspect {
	/**
	 * 切入被拦截的服务的方法，
	 * 
	 * <pre>
	 * －调用point.cut方法以执行桥接出来的方法
	 * </pre>
	 * 
	 * @param bridge
	 * @param args
	 * @param point
	 * @return
	 */
	Object cut(Object bridge, Object[] args, ICutpoint point) throws Throwable;

	/**
	 * 服务对象的面子
	 * 
	 * <pre>
	 * 作用：
	 * －如果此接口集中包含某接口a，且服务对象在定义时无论是否实现了a接口，均可强制转换成此a接口使用。
	 * －如果该集合为非空集合，在其它服务通过属性引用使用此服务对象（桥接到此方面的服务）时，建议使用接口集合中的接口，这是因为：
	 *   引导开发者关注于方面编程，属性引用类型是桥的方面的可转换接口
	 * －所有声明为桥的服务均是适配器类型，因此被切入的桥服务可通过适配器方法转换为任意接口。
	 * 
	 * 方面支持的接口，
	 * 1.由此生成桥对象，
	 * 2.非方面所支持的类型则不被访方面处理
	 * 3.可以返回null值或空值，它表示桥服务可转换为原类类型，其实空不空桥服务的基类都是其自身，
	 *   此处设返回接口的好处就是能为桥服务添加任意接口，然后方面可判断是否是源自该接口的方法请求，即可处置，这就类似于为桥服务实现了新的接口及方法实现。
	 * 
	 * 注：接口仅用于桥服务在使用者面前可以被转换的类型，它并不作为拦截方法的过滤条件，
	 *  在一个切入点icutpoint中，第一个方面接受所有桥服务的调用方法切入，
	 *  方面中可自行过滤方法的命中条件，比如根据接口、名称等等。
	 * </pre>
	 * 
	 * @return
	 */
	// 此句话是前片本写的，不知有何作用，暂记于此：如果返回null或空则以Iadaptable,IBridgeable及Object的方法作为方面的切入接口
	Class<?>[] getCutInterfaces();
	// 是否能应用到指定的服务，在元数据编译前被调用以修改桥。
	// boolean canApplyTo(String serviceDefId);
	// boolean canApplyTo(Class<?> serviceClass);
	/**
	 * 观察对象
	 * @param service
	 */
	void observe(Object service);

}
