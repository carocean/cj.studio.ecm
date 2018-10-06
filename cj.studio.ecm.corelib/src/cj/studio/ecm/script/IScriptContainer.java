package cj.studio.ecm.script;

import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import cj.ultimate.IDisposable;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
/**
 * 脚本容器，为芯片提供统一的脚本引擎
 * <pre>
 *
 * </pre>
 * @author carocean
 *
 */
public interface IScriptContainer extends IDisposable{
	String USER_JSS_KEYWORDS="exports";
	void init();
	Bindings global();
	ClassLoader classloader();
	<T>T getInterface(T obj,Class<T> clazz);
	void classloader(ClassLoader cj);
	/**
	 * 包装用户自定义的脚本，此为jss做外壳，使之类似于nodejs
	 * <pre>
	 * 该方法一次性执行头和正文，如需要在了解jss服务的特性之后再决定执行正文的话，调用方法:loadJssHead
	 * </pre>
	 * @param moduleName 即：$.cj.studio.moduleName，默认情况下：<br>jss页面和widget是：$.cj.jss.views,jss 服务是：$.cj.jss.services
	 * @param userJssCode 用户自定义的脚本
	 * @param objectName 用户对象在容器中的名字。脚本容器中的对象，最终是通过$.jss.module.['objectName']来访问，objectName可以.分隔多级.
	 * 如果存在相同的对象名，则报错
	 * @param importsDomain 该域是jss服务的专属域，用于对jss服务输入
	 * @param objectProps 系统为用户对象设置的属性。如果要传递java对象，请序列化为js的json格式，即为此map元素添加子map.<br>
	 * 如需传引用，需要Bindings传。
	 */
	void loadJss(String moduleName,String userJssCode,String objectName,Bindings importsDomain,Map<String,String> objectProps)throws ScriptException ;
	/**
	 * 该方法执行头代码，并返回未执行的已编译正文代码。
	 * <pre>
	 *
	 * </pre>
	 * @param moduleName
	 * @param userJssCode
	 * @param relateName
	 * @param importsDomain
	 * @param objectProps
	 * @return 返回未执行的已编译正文代码。
	 * @throws ScriptException
	 */
	String loadJssHead(String moduleName,String userJssCode,String relateName,Bindings importsDomain,Map<String,String> objectProps)throws ScriptException ;
	public ScriptObjectMirror jssObject(String moduleName,String relateName,Bindings b) throws ScriptException;
	public void deleteJssObject(String moduleName,String relateName,Bindings b) throws ScriptException;
	public boolean containsJssObject(String moduleName,String relateName,Bindings b) throws ScriptException;
	public boolean containsJssObjectDefine(String moduleName,String relateName,Bindings b) throws ScriptException;
	public boolean containsJssModule(String moduleName,Bindings b) throws ScriptException;
	public ScriptObjectMirror JssObjectDefine(String moduleName,String relateName,Bindings b) throws ScriptException;
	public String[] enumJssObjectOfModule(String moduleName,Bindings b) throws ScriptException;
	public String[] enumJssObjectDefineOfModule(String moduleName,Bindings b) throws ScriptException;
	ScriptObjectMirror eval(String js,Bindings b) throws ScriptException;
	ScriptObjectMirror jssModule(String moduleName,Bindings b) throws ScriptException;
	ScriptEngine getEngine();
}
