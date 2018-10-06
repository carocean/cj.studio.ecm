package cj.studio.ecm.container;

import java.util.ArrayList;

import cj.ultimate.net.sf.cglib.proxy.Enhancer;

/**
 * 在搜索服务时用于临时存放服务<br>
 * 为什么派生此类？答：
 * 
 * <pre>
 * 这是因为：
 * －当服务为桥时，使用了cglib代理，当调用代理对象的equals比较时，该方法也被代理类代理，为了说明，此时假设：
 * －－a为原始类型实例，b为派生于a的cglib代理类型实例，则：
 * －－b.equals(a)为false，实验，在a类型中重载equals方法，设断点，发现传入的参数是b类型，而拿a对象比b对象，肯定不同，因为a,b是不同的实例：
 * 
 * 解决办法：
 * －在此类中重载contains方法而此方法实际调用indexOf方法，故重载indexOf，如果是代理类，则进行对象实例比较，从而避免集合调用元素的equals方法比较
 * 
 * 
 * 注意：此方案来处理cglib的问题是否会引发其它问题，待日后观查
 * </pre>
 * 
 * @author carocean
 *
 * @param <E>
 */
class ServiceArrayList<E> extends ArrayList<E> {

	/**
	 * <pre>
	 *
	 * </pre>
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public int indexOf(Object o) {
		int size = size();
		if (o == null) {
			for (int i = 0; i < size; i++)
				if (get(i) == null)
					return i;
		} else {
			boolean isEnhance = Enhancer.isEnhanced(o.getClass());
			for (int i = 0; i < size; i++) {
				Object e = get(i);
				if (isEnhance) {
					if (e == o)
						return i;
				} else {
					if (o.equals(e))
						return i;
				}
			}
		}
		return -1;
	}

}
