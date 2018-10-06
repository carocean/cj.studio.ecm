package cj.studio.ecm.script;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptException;

import cj.studio.ecm.EcmException;
import cj.ultimate.util.StringUtil;

public class JssCodeCompiler implements IJssCodeCompiler {
	Pattern commentsPat;// 解析出解释窜
	Pattern headPat;// 解析位于jjs代码中的头串
	Pattern repleacePat;// 用于替换头串中注释代码前含有*号的行，将被去掉

	public JssCodeCompiler() {
		commentsPat = Pattern.compile("/\\*+(.*?)\\*+/", Pattern.DOTALL
				| Pattern.MULTILINE);// 解析出解释窜
		headPat = Pattern.compile(
				"/\\*.*?\\*/|<!\\[\\s*(\\w+)\\s*:\\{(.*?)\\}(\\s*|\\**)\\]>",
				Pattern.DOTALL);// 解析位于jjs代码中的头串
		repleacePat = Pattern.compile(
				"^\\s*\\*+((?!<\\!\\[)|(?!\\}\\]>)|(?!\\]\\>).)*",
				Pattern.MULTILINE);// 用于替换头串中注释代码前含有*号的行，将被去掉
	}

	public static void main(String... strings) throws ScriptException {
		JssCodeCompiler c = new JssCodeCompiler();
		StringBuffer sb = new StringBuffer();
		sb.append("/****** ");
		sb.append("* 作家：\r\n");
		sb.append("* 日期:\r\n");
		sb.append("*");
		sb.append("*   <![jss:{\r\n");
		sb.append("scope:'runtime',");
		sb.append("\r\n");
		sb.append("\r\n");
		sb.append("filter:{");
		sb.append("\r\n");
		sb.append("	invalid:'true',");
		sb.append("\r\n");
		sb.append("	matchId:'$.cj.test.deptment.*',");
		sb.append("\r\n");
		sb.append("	matchType:'clazz'");
		sb.append("\r\n");
		sb.append("}");
		sb.append("\r\n");
		sb.append("*},test:{name:'zhao'}]>");

		sb.append("*   <![define:{\r\n");
		sb.append("scope:'runtime',");
		sb.append("\r\n");
		sb.append("\r\n");
		sb.append("filter:{");
		sb.append("\r\n");
		sb.append("	invalid:'true',");
		sb.append("\r\n");
		sb.append("	matchId:'$.cj.test.deptment.*',");
		sb.append("\r\n");
		sb.append("	matchType:'clazz'");
		sb.append("\r\n");
		sb.append("}");
		sb.append("\r\n");
		sb.append("*}]>");

		sb.append("*****/");
		sb.append("\r\n");
		sb.append("print('sss');dd={}");
		sb.append("\r\n");
		sb.append("/** 吃 饭了吧**/");
		sb.append("exports.ddd=function(){\r\nprint('sss');\r\n}");
		JssCode code = c.compile("pages", "cj.studio.test.Test1",
				sb.toString(), new HashMap<String, String>());
		// UserJssCodeSegment code=c.parseCode("",sb.toString());
		System.out.println("--------正文代码");
		System.out.println(code.getBodyCode());
		System.out.println("----------头代码");
		System.out.println(code.getHeadCode());
	}

	protected JssCode parseCode(String relateName, String userJssCode) {
		// String[] ret = new String[2];// 0为正文，1为定义
		Matcher cm = commentsPat.matcher(userJssCode);
		StringBuffer sb = new StringBuffer();
		// String headCode = "{";
		JssCode jssCode = new JssCode();
		int index = 0;
		while (cm.find()) {
			String t = cm.group(1);

			if (StringUtil.isEmpty(t) || !t.contains("<![")) {
				continue;
			}
			cm.appendReplacement(sb, "");
			String s = repleacePat.matcher(t).replaceAll("");
			Matcher hm = headPat.matcher(s);
			boolean found = false;
			while (hm.find()) {
				if (hm.groupCount() < 1) {
					throw new EcmException(String.format(
							"编译jss失败，请检查：<![xxx{是否指定。%s", relateName));
				}
				if (hm.groupCount() < 2) {
					throw new EcmException(
							String.format(
									"编译jss失败，请检查：<![xxx{代码}]>中的代码部分是否正确。%s",
									relateName));
				}
				if (StringUtil.isEmpty(hm.group(1))) {
					throw new EcmException(String.format(
							"编译jss失败，请检查：<![xxx{代码}]>中的代码部分xxx处是否指定了名。%s",
							relateName));
				}
				String f = hm.group(2);
				f = StringUtil.isEmpty(f) ? "" : f;
				// headCode += f;
				jssCode.addRawHead(hm.group(1), "{" + f + "}");
				found = true;
			}
			if (found)
				index++;
		}
		if (index > 1) {
			throw new EcmException(
					String.format(
							"编译jss失败，请检查：在多处注释中使用了jss语法体，正确格式为：\r\n/*<![xxx{代码},yyy{代码}]><![zzz{代码},ttt{代码}]>*/只能在一个注释中使用。\r\n%s",
							relateName));
		}
		// headCode += "}";
		cm.appendTail(sb);// 正文
		jssCode.setRawBody(sb.toString());
		// ret[0] = sb.toString();
		// ret[1] = headCode;
		return jssCode;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cj.studio.ecm.script.IUserJssCodeCompiler#compile(java.lang.String,
	 * java.lang.String, java.lang.String, java.util.Map)
	 */
	@Override
	public JssCode compile(String moduleName, String relateName,
			String userJssCode, Map<String, String> objectProps) {
		JssCode code = parseCode(relateName, userJssCode);
		String bodyCode = compileJssBody(moduleName, code.getRawBody(),
				relateName, objectProps);
		String headCode = compileJssDefine(moduleName, code.getRawHeadCode(),
				relateName, objectProps);
		code.setBodyCode(bodyCode);
		code.setHeadCode(headCode);
		return code;
	}

	/**
	 * 
	 * <pre>
	 *  if(typeof $ !=='object'){
	 * 	$={};
	 * };
	 * if(typeof $.cj !=='object'){
	 * 	$.cj={};
	 * };
	 * if(typeof $.cj.jss !=='object'){
	 * 	$.cj.jss={};
	 * };
	 * (function(global,userFun){
	 * 	var module=global.$.cj.jss.pages;//moduleName
	 * 	if(typeof module !=='object'){
	 * 		module={};
	 * 		global.$.cj.jss.pages=module;
	 * 	};
	 * var defines=module.defines;
	 * if(typeof defines!=='object'){
	 * defines={};
	 * global.$.cj.jss.pages.defines=defines;
	 * }
	 * defines['cj.studio.test.Test1']={define:{
	 * scope:'runtime',
	 * 
	 * filter:{
	 * 	invalid:'true',
	 * 	matchId:'$.cj.test.deptment.*',
	 * 	matchType:'clazz'
	 * }
	 * }jss:{
	 * scope:'runtime',
	 * 
	 * filter:{
	 * 	invalid:'true',
	 * 	matchId:'$.cj.test.deptment.*',
	 * 	matchType:'clazz'
	 * }
	 * }};
	 * userFun(defines['cj.studio.test.Test1']);
	 * })(this,userFun(head){
	 * });
	 * </pre>
	 * 
	 * @param moduleName
	 * @param defineCode
	 * @param relateName
	 * @param objectProps
	 * @return
	 */
	protected String compileJssDefine(String moduleName, String defineCode,
			String relateName, Map<String, String> objectProps) {
		StringBuffer sb = new StringBuffer();
		sb.append("if(typeof $ !=='object'){");
		sb.append("\r\n");
		sb.append("$={};");
		sb.append("\r\n");
		sb.append(" };");
		sb.append("\r\n");
		sb.append("if(typeof $.cj !=='object'){");
		sb.append("\r\n");
		sb.append("	$.cj={};");
		sb.append("\r\n");
		sb.append("};");
		sb.append("\r\n");
		sb.append("if(typeof $.cj.jss !=='object'){");
		sb.append("\r\n");
		sb.append("	$.cj.jss={};");
		sb.append("\r\n");
		sb.append("};");
		sb.append("\r\n");
		sb.append("(function(global,userFun){");
		sb.append("\r\n");
		sb.append(String.format("	var module=global.$.cj.jss.%s;", moduleName));
		sb.append("\r\n");
		sb.append("	if(typeof module !=='object'){");
		sb.append("\r\n");
		sb.append("module={};");
		sb.append("\r\n");
		sb.append(String.format("global.$.cj.jss.%s=module;", moduleName));
		sb.append("\r\n");
		sb.append("};");
		sb.append("\r\n");
		sb.append(String.format("var defines=module.defines;"));
		sb.append("\r\n");
		sb.append("if(typeof defines!=='object'){");
		sb.append("\r\n");
		sb.append("defines={};");
		sb.append("\r\n");
		sb.append(String.format("module.defines=defines;",
				moduleName));
		sb.append("\r\n");
		sb.append("}");
		sb.append("\r\n");
		sb.append(String.format("defines['%s']=%s;", relateName, defineCode));
		sb.append("\r\n");
		sb.append(String.format("userFun(defines['%s']);", relateName));

		// sb.append(String.format("var objDef=defines['%s'];", relateName));
		// sb.append("\r\n");
		// sb.append("if(typeof objDef!=='object'){");
		// sb.append("\r\n");
		// sb.append(String.format("objDef=%s;", defineCode));
		// sb.append("\r\n");
		// sb.append(String.format("defines['%s']=objDef;", relateName));
		// sb.append("\r\n");
		// sb.append("}");
		sb.append("\r\n");
		sb.append("})(this,function(head){");
		sb.append("\r\n");
		sb.append("});");
		return sb.toString();
	}

	/**
	 * <pre>
	 * <code>
	 * if(typeof $ !=='object'){
	 * 	$={};
	 * };
	 * if(typeof $.cj !=='object'){
	 * 	$.cj={};
	 * };
	 * if(typeof $.cj.jss !=='object'){
	 * 	$.cj.jss={};
	 * };
	 * //用户代码外壳
	 * (function (global,userJssFun) {
	 * 	var module=global.$.cj.jss.moduleName;//moduleName
	 * 	if(typeof module !=='object'){
	 * 		module={};
	 * 		global.$.cj.jss.moduleName=module;
	 * 	};
	 * 	var userJssObj=module['relateName'];
	 * 	if(typeof userJssObj !=='object'){//缓冲就不执行用户代码段，程序可根据$.cj.studio.pages[pagePath]得到该对象
	 * 		userJssObj={name:'',location:'',cache:'false'};//属性objectProps
	 * 		module['relateName']=userJssObj;//relateName
	 * 		userJssFun(userJssObj);
	 * 	};	
	 * }(this,function(exports){
	 * 	//用户代码段userJssCode
	 *   })
	 * );
	 * </code>
	 * </pre>
	 */
	protected String compileJssBody(String moduleName, String userBodyCode,
			String relateName, Map<String, String> objectProps) {
		StringBuffer sb = new StringBuffer();
		sb.append("if(typeof $ !=='object'){");
		sb.append("\r\n");
		sb.append("$={};");
		sb.append("\r\n");
		sb.append(" };");
		sb.append("\r\n");
		sb.append("if(typeof $.cj !=='object'){");
		sb.append("\r\n");
		sb.append("	$.cj={};");
		sb.append("\r\n");
		sb.append("};");
		sb.append("\r\n");
		sb.append("if(typeof $.cj.jss !=='object'){");
		sb.append("\r\n");
		sb.append("	$.cj.jss={};");
		sb.append("\r\n");
		sb.append("};");
		sb.append("\r\n");
		sb.append("(function (global,userJssFun) {");
		sb.append("\r\n");
		sb.append(String.format("	var module=global.$.cj.jss.%s;", moduleName));
		sb.append("\r\n");
		sb.append("	if(typeof module !=='object'){");
		sb.append("\r\n");
		sb.append("module={};");
		sb.append("\r\n");
		sb.append(String.format("global.$.cj.jss.%s=module;", moduleName));
		sb.append("\r\n");
		sb.append("};");
		sb.append("\r\n");
		sb.append(String.format("var userJssObj=module['%s'];", relateName));
		sb.append("\r\n");
		sb.append("if(typeof userJssObj !=='object'){");
		sb.append("\r\n");
		sb.append(String.format("userJssObj=%s;", map(objectProps.entrySet()
				.iterator())));
		sb.append("\r\n");
		sb.append(String.format("module['%s']=userJssObj;", relateName));
		sb.append("\r\n");
		sb.append("userJssFun(userJssObj);");
		sb.append("\r\n");
		sb.append("};");
		sb.append("\r\n");
		sb.append("})(this,function(exports){");
		sb.append("\r\n");
		// 用户代码段userJssCode
		sb.append(userBodyCode);
		sb.append("\r\n");
		sb.append(" });");
		sb.append("\r\n");
		//sb.append(");");
		//sb.append("\r\n");
		return sb.toString();
	}

	public static String map(Iterator<Entry<String, String>> i) {
		if (!i.hasNext())
			return "{}";

		StringBuilder sb = new StringBuilder();
		sb.append('{');
		for (;;) {
			Entry<String, String> e = i.next();
			String key = e.getKey();
			String value = e.getValue();
			sb.append(key);
			sb.append(':');
			if ((value.startsWith("{") && value.endsWith("}")))
				sb.append(value);
			else
				sb.append(String.format("\"%s\"", value));
			if (!i.hasNext())
				return sb.append('}').toString();
			sb.append(',').append(' ');
		}
	}
}
