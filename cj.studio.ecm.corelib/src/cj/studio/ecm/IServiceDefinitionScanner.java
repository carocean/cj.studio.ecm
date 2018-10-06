package cj.studio.ecm;

import java.util.Map;


/**
 * 服务定义扫描器。<br>
 * 得到bean定义，并解析为服务元数据<br>
 * 它绑定到服务容器（注册表）
 * @author Administrator
 *
 */
public interface IServiceDefinitionScanner {
	/**
	 * 增加解析器，解析器成对出现
	 * @param definitionResolver
	 * @param metaDataResolver
	 */
	void addResolver(IServiceDefinitionResolver definitionResolver);
	/**
	 * 扫描的模式路径<br>
	 * 该方法可以多次调用以扫描多个路径
	 * <br>通配符*代表扫描所有路径下的所有文件<br>
	 * *.json,*.class代表扫描的类型
	 * @param patternPath
	 */
	void scan(String patternPath);
	void setServiceNameGenerator(IServiceNameGenerator generator);
	void setServiceScopeGenerator(IServiceScopeGenerator generator);
	void setCombinServiceDefinitionStrategy(ICombinServiceDefinitionStrategy strategy);
	//获取扫描出来的开放类型名,也可能是包，如果是包其value=true
	Map<String,Boolean> getExotericalTypeNames();
	
}
