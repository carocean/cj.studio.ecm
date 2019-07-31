package cj.studio.ecm;
/**
 * 运行时服务回调器
 * @author caroceanjofers
 *
 */
public interface IRuntimeServiceCreator {
	/**
	 * 如果create返回空则默认返回请求的服务原型
	 * @return
	 */
	Object create();
}
