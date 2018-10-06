package cj.studio.ecm.script;

import cj.studio.ecm.Scope;
import cj.ultimate.util.StringUtil;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * jss头中定义的jss对象
 * <pre>
 *
 * </pre>
 * @author carocean
 *
 */
public class JssDecriber {
	public final static String JSS_DECRIBER_LITTER="jss";
	Scope scope;
	boolean filterInvalid;
	String filterMatchId;
	String interrupter;//拦截函数名
	String extendsType;
	JssDecriber() {
		
	}
	public static JssDecriber parse(ScriptObjectMirror head){
		JssDecriber d=new JssDecriber();
		d.init(head);
		return d;
	}
	private void init(ScriptObjectMirror head) {
		ScriptObjectMirror  jss=(ScriptObjectMirror)head.get("jss");
		if(jss==null){
			scope=Scope.runtime;
			filterInvalid=true;
			filterMatchId="";
			interrupter="";
			return ;
		}
		String scopestr=StringUtil.isEmpty((String)jss.get("scope"))?"runtime":(String)jss.get("scope");
		scope= Scope.valueOf(scopestr);
		extendsType=StringUtil.isEmpty((String)jss.get("extends"))?"":(String)jss.get("extends");
		ScriptObjectMirror filterm=(ScriptObjectMirror)jss.get("filter");
		if(filterm==null){
			filterInvalid=true;
			filterMatchId="";
			interrupter="";
			return;
		}
		String invalidStr=StringUtil.isEmpty((String)filterm.get("invalid"))?"true":(String)filterm.get("invalid");
		this.filterInvalid=Boolean.valueOf(invalidStr);
		this.filterMatchId=StringUtil.isEmpty((String)filterm.get("pattern"))?"":(String)filterm.get("pattern");
		this.interrupter=StringUtil.isEmpty((String)filterm.get("interrupter"))?"":(String)filterm.get("interrupter");
		
	}
	public String getExtendsType() {
		return extendsType;
	}
	public ScriptObjectMirror getJss(ScriptObjectMirror head) {
		return (ScriptObjectMirror)head.get("jss");
	}
	public Scope scope(){
		return scope;
	}
	
}
