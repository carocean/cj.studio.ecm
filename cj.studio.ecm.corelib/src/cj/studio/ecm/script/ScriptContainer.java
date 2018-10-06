package cj.studio.ecm.script;

import java.util.Map;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import cj.studio.ecm.EcmException;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class ScriptContainer implements IScriptContainer {
	ScriptEngineManager engineManager;
	ScriptEngine engine;
	ClassLoader classloader;
	Bindings global;
	IJssCodeCompiler compiler;
	public ScriptContainer() {
		compiler=new JssCodeCompiler();
	}

	@Override
	public void dispose() {
		engine = null;
		engineManager = null;
		classloader = null;
		global.clear();
		global = null;
		compiler=null;
	}

	@Override
	public void init() {
		engine();
		try {
			engine.eval("$={};$.cj={};$.cj.jss={};");
		} catch (ScriptException e) {
			throw new EcmException(e);
		}
	}
	@Override
	public <T> T getInterface(T obj, Class<T> clazz) {
		Invocable inv=(Invocable)engine();
		return inv.getInterface(obj, clazz);
	}
	public ClassLoader classloader() {
		return classloader;
	}
	private ScriptEngine engine() {
		if (engine != null)
			return engine;
		ClassLoader cl = classloader;
		if (cl == null) {
			cl = Thread.currentThread().getContextClassLoader();
		}
		//脚本引擎scriptContainer虽然传入了自定义类加载器，但仍不起作用，可能是以当前线程上下文的加载器也有关系，因此导致在jss中获取不到自定义的java类型
		//注意：此处与assembly.createResource,assembly.stop中实现的类加载器相关，如有问题需看此三个方法。
//		Thread.currentThread().setContextClassLoader(cl);
		engineManager = new ScriptEngineManager(cl/* cl可指定classLoader */);
		global = new SimpleBindings();
		engineManager.setBindings(global);
		engine = engineManager.getEngineByName("nashorn");
		return engine;
	}
	@Override
	public ScriptEngine getEngine() {
		return engine;
	}
	@Override
	public Bindings global() {
		return global;
	}

	@Override
	public void classloader(ClassLoader cj) {
		classloader = cj;
	}
	public ScriptObjectMirror jssObject(String moduleName,String relateName,Bindings b) throws ScriptException{
		String sel=String.format("$.cj.jss.%s['%s']",moduleName,relateName);
		Object obj= engine().eval(sel,b);
		if(obj==null)return null;
		return (ScriptObjectMirror)obj;
	}
	public void deleteJssObject(String moduleName,String relateName,Bindings b) throws ScriptException{
		String del=String.format("delete $.cj.jss.%s['%s'];",moduleName,relateName);
		engine().eval(del,b);
	}
	public boolean containsJssObject(String moduleName,String relateName,Bindings b) throws ScriptException{
		String exists=String.format("$.cj.jss.%s",moduleName);
		Object o=engine().eval(exists,b);
		if(o==null)return false;
		ScriptObjectMirror m=(ScriptObjectMirror)o;
		return m.hasMember(relateName);
	}
	public boolean containsJssObjectDefine(String moduleName,String relateName,Bindings b) throws ScriptException{
		String exists=String.format("$.cj.jss.%s.defines",moduleName);
		Object o=engine().eval(exists,b);
		if(o==null)return false;
		ScriptObjectMirror m=(ScriptObjectMirror)o;
		return m.hasMember(relateName);
	}
	public boolean containsJssModule(String moduleName,Bindings b) throws ScriptException{
		String exists=String.format("$.cj.jss");
		Object o=engine().eval(exists,b);
		if(o==null)return false;
		ScriptObjectMirror m=(ScriptObjectMirror)o;
		return m.hasMember(moduleName);
	}
	public ScriptObjectMirror JssObjectDefine(String moduleName,String relateName,Bindings b) throws ScriptException{
		String sel=String.format("$.cj.jss.%s.defines['%s']",moduleName,relateName);
		Object obj= engine().eval(sel,b);
		if(obj==null)return null;
		return (ScriptObjectMirror)obj;
	}
	public String[] enumJssObjectOfModule(String moduleName,Bindings b) throws ScriptException{
		String sel=String.format("$.cj.jss.%s",moduleName);
		Object obj=null;
		if(b!=null)
		 obj= engine().eval(sel,b);
		else
			obj=engine().eval(sel);
		if(obj==null)return new String[0];
		ScriptObjectMirror m=(ScriptObjectMirror)obj;
		return m.keySet().toArray(new String[0]);
	}
	public ScriptObjectMirror jssModule(String moduleName,Bindings b) throws ScriptException{
		String sel=String.format("$.cj.jss.%s",moduleName);
		Object obj= engine().eval(sel,b);
		return (ScriptObjectMirror) obj;
	}
	public String[] enumJssObjectDefineOfModule(String moduleName,Bindings b) throws ScriptException{
		String sel=String.format("$.cj.jss.%s.defines",moduleName);
		Object obj= engine().eval(sel,b);
		if(obj==null)return new String[0];
		ScriptObjectMirror m=(ScriptObjectMirror)obj;
		return m.keySet().toArray(new String[0]);
	}
	public ScriptObjectMirror eval(String js,Bindings b) throws ScriptException{
		return (ScriptObjectMirror) engine().eval(js, b);
	}
	
	@Override
	public void loadJss(String moduleName, String userJssCode,
			String objectName,Bindings importsDomain, Map<String, String> objectProps) throws ScriptException {
		JssCode code=compiler.compile(moduleName, objectName, userJssCode, objectProps);
		engine.eval(code.getBodyCode(),importsDomain);
		engine.eval(code.getHeadCode(),importsDomain);
	}

	@Override
	public String loadJssHead(String moduleName, String userJssCode,
			String objectName, Bindings importsDomain,
			Map<String, String> objectProps) throws ScriptException {
		JssCode code=compiler.compile(moduleName, objectName, userJssCode, objectProps);
		engine.eval(code.getHeadCode(),importsDomain);
		return code.getBodyCode();
	}


}
