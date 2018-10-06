package cj.studio.ecm.graph;
/**
 * 由SinkCreateBy创建的sink实现此类型的话可以得到持有它的端子，如果是导线类型，则返回导线端子，如果是电缆线类型，也则返回持有它的导线
 * <pre>
 *
 * </pre>
 * @author carocean
 *
 */
public interface ICreateCallbackSink extends ISink{
	/**
	 * 设置它的持有者端子
	 * <pre>
	 *
	 * </pre>
	 * @param pin
	 */
	void setOwnerPin(IPin pin);
}
