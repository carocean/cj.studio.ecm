package cj.studio.ecm.parser;

import cj.studio.ecm.IServiceProvider;
/**
//用于解析配置的属性值,工厂实现负责收集和管理ValueParser,充许客户扩展valueParser，像定义普通服务一样
 * 
 * @author carocean
 *
 */
public interface IValueParser {
	/**
	//cj提供的解析器,如cj.basic,cj.jsonList,cj.jsonMap,cj.codeScript等
	//配置的值，待要转换的目标类型，数据提供器
	//如果两个服务互相引用对方属性而使用解析器去获取另一服务时会导致死循环
	 * 
	 * @param propName 注解的属性
	 * @param value 注入的值，此前传入原始值
	 * @param targetType 接受值注入的目标类型
	 * @param provider 服务容器
	 * @return
	 */
	public Object parse(String propName,String value, Class<?> targetType, IServiceProvider provider);
}
