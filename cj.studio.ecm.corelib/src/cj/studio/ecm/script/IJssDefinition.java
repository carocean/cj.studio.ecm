package cj.studio.ecm.script;

import javax.script.Bindings;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * 表示一个jss服务定义。
 * 
 * <pre>
 *
 * </pre>
 * 
 * @author carocean
 *
 */
public interface IJssDefinition {
	Bindings importsDomain();
	public void importsDomain(Bindings b);
	ScriptObjectMirror getHead();
	String relateName();
	public void setHead(ScriptObjectMirror head) ;
	/**
	 * 如：$.cj.jss.service1.test.dept
	 * <pre>
	 *
	 * </pre>
	 * @return
	 */
	String selectName();

	/**
	 * head中jss关键字对象。
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @return
	 */
	JssDecriber getDecriber();
	/**
	 * 相对于模块的位置，即相对文件路径名，带文件扩展名
	 * <pre>
	 *
	 * </pre>
	 * @return
	 */
	String location(String extName);

	String ownerModule();

	/**
	 * 服务的来源，与searchMode对应，即在哪里发现的并装载成服务的。inner,link
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @return
	 */
	String source();

	/** 请求id格式：$.cj.jss.模块名.相对jss服务名（如：$.cj.jss.service1.test.dept，test.dept即是相对于模块service1的相对服务名)
	// 返回值0是模块名，1是请求服务相对于模块的包名,如上例，即test.dept。
	 * 
	 */
	static String[] parseReqServiceId(String reqsid) {
		String str = reqsid.replace(IJssModule.$_CJ_JSS_DOT, "");
		String moduleName = "";
		String relateObjName = "";
		if (str.contains(".")) {
			moduleName = str.substring(0, str.indexOf("."));
			relateObjName = str.replaceFirst("^"+moduleName + "\\.", "");
		} else {// 说明查找模块名，则返回模块对象镜象。
			moduleName = str;
		}
		return new String[] { moduleName, relateObjName };
	}
	/**
	 * 在脚本中的查询名：
	 * <pre>
	 * $.cj.jss.service1['test.dept']
	 * </pre>
	 * @return
	 */
	String selectScriptName();
}
