package cj.studio.ecm.script;

import java.util.Map;

public interface IJssCodeCompiler {
	/**
	 * 将用户jss代码编译为可执行代码
	 * <pre>
	 *
	 * </pre>
	 * @param moduleName
	 * @param objectName
	 * @param userJssCode
	 * @param objectProps
	 * @return 
	 */
	public abstract JssCode compile(String moduleName, String objectName,
			String userJssCode, Map<String, String> objectProps);

}