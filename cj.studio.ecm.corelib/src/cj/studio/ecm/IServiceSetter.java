package cj.studio.ecm;

import cj.studio.ecm.annotation.CjServiceInvertInjection;

/**
 * 服务注入器，与CjServiceInjectionInvert注解联用
 * <pre></pre>
 * <h3>解释</h3>
 * <ul>
 * <li>功能点</li>
 * </ul>
 * @author C.J 赵向彬 <br>
 *   2012-1-28<br>
 * @see
 * <li>{@link CjServiceInvertInjection CjServiceInjectionInvert}
 * <li>{@link type2 label}
 */
public interface IServiceSetter {
	void setService(String serviceId,Object service);
}
