package cj.studio.ecm.adapter;

import cj.ultimate.net.sf.cglib.proxy.Enhancer;

/**
 * 适配器工厂提供开发者生成自己的适配器对象，该适配器将保留原类型，并生成新的类型 服务的适配器都是更改原生类型，由新类型替代的。
 * 通过Enhancer实现适配模式有很多缺点
 */
public class AdapterFactory {
	// public static void main(String... method) throws CircuitException {
	// IAdaptable a = AdapterFactory.createAdaptable(new ICommand() {
	//
	// @Override
	// public Object exeCommand(Object adapter, String method, Object[] args) {
	// System.out.println(method);
	// return null;
	// }
	// });
	// Message obj = a.getAdapter(Message.class);
	// obj.toString();
	// }

	public static boolean isAdaptable(Object obj) {
		return (obj instanceof IAdaptable);
	}

	/**
	 * 
	 * 以Object.class作为超类生成代理
	 * <pre>
	 * 对指定的对象创建可适配对象，如果为icommand对象，将传出指令
	 * </pre>
	 * 
	 * @param original
	 * @return
	 */
	// 对指定的对象创建可适配对象，如果为icommand对象，将传出指令
	public static IAdaptable createAdaptable(Object original) {
		return createAdaptable(original,false);
	}
	/**
	 * 
	 * <pre>
	 *
	 * </pre>
	 * @param original
	 * @param isExtendsOriginalClass 是否从指定的对象类型派生,否则从Object.class派生
	 * @return
	 */
	public static IAdaptable createAdaptable(Object original,boolean isExtendsOriginalClass) {
		if (isAdaptable(original)) {
			return (IAdaptable) original;
		}
		AdapterInterrupter innerHandler = new AdapterInterrupter(original);
		Class<?> superClazz = null;
		if(isExtendsOriginalClass){
			superClazz=original.getClass();
		}else{
			superClazz=Object.class;
		}
		// 使用原型的超类可以相互转换，但会有许多属性庸余
		// Class<?> superClazz = original.getClass();
		Object adapter = Enhancer.create(superClazz,
				new Class<?>[] { IAdaptable.class }, innerHandler);
		return (IAdaptable) adapter;
	}
}
