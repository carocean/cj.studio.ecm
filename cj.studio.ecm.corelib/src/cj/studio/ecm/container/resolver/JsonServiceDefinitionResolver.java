package cj.studio.ecm.container.resolver;

import java.io.InputStream;

import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.MethodMode;
import cj.studio.ecm.bridge.UseBridgeMode;
import cj.studio.ecm.container.describer.BridgeDescriber;
import cj.studio.ecm.container.describer.BridgeJoinpoint;
import cj.studio.ecm.container.describer.ParametersMehtodDescriber;
import cj.studio.ecm.container.describer.ReturnMehtodDescriber;
import cj.studio.ecm.container.describer.ServiceDescriber;
import cj.studio.ecm.container.describer.ServiceInvertInjectionDescriber;
import cj.studio.ecm.container.describer.ServiceMethod;
import cj.studio.ecm.container.describer.ServiceProperty;
import cj.studio.ecm.container.describer.ServicePropertyValueDescriber;
import cj.studio.ecm.container.describer.ServiceRefDescriber;
import cj.studio.ecm.container.describer.ServiceSiteDescriber;
import cj.studio.ecm.container.registry.JsonServiceDefinition;
import cj.studio.ecm.json.JsonReflector;
import cj.studio.ecm.resource.IResource;
import cj.ultimate.gson2.com.google.gson.JsonArray;
import cj.ultimate.gson2.com.google.gson.JsonElement;
import cj.ultimate.gson2.com.google.gson.JsonObject;
import cj.ultimate.util.StringUtil;

public class JsonServiceDefinitionResolver extends
		AbstractServiceDefinitionResolver {
	protected JsonReflector createJsonReflector(InputStream stream) {
		return new JsonServiceReflector(stream);
	}

	protected void validate(JsonElement e, String nodeName) {
		if ("root".equals(nodeName)) {
			if (!e.isJsonObject())
				throw new RuntimeException("JSON服务配置不正确，根的配置必须为对象");
		}
		if ("properties".equals(nodeName)) {
			if (!e.isJsonArray())
				throw new RuntimeException("属性必须为数组");
		}
		if ("methods".equals(nodeName)) {
			if (!e.isJsonArray())
				throw new RuntimeException("方法必须为数组");
		}
		if ("adapter".equals(nodeName)) {
			if (!e.isJsonObject())
				throw new RuntimeException("适配器必须为对象");
		}
		if ("method".equals(nodeName)) {
			if (!e.isJsonObject()) {
				throw new RuntimeException("方法必须配成对象格式");
			}
		}
		if ("property".equals(nodeName)) {
			if (!e.isJsonObject()) {
				throw new RuntimeException("属性必须配成对象格式");
			}
		}
		if ("args".equals(nodeName)) {
			if (!e.isJsonArray()) {
				throw new RuntimeException("参数必须配成数组格式");
			}
		}
		if ("arg".equals(nodeName)) {
			if (!e.isJsonObject()) {
				throw new RuntimeException("参数必须配成对象格式");
			}
		}

		if ("invertInjection".equals(nodeName)) {
			if (!e.isJsonObject()) {
				throw new RuntimeException("反转注入必须配成对象格式");
			}
		}
	}

	// 对于镜的解析放到元数据解析类中实现，因为镜子是注解方式的接口类，JSON/XML方式不考虑支持镜子的注解，所以json/xml的解析类都不实现镜子的解析，
	// 由于注解/json/xml等方式可以对同一个类合并解析，所以镜子的解析完成由注解解析器完成。
	@Override
	public IServiceDefinition resolve(String resItem, IResource resource) {
		if (!resItem.endsWith(".json"))
			return null;
		InputStream stream = resource.getResourceAsStream(resItem);
		JsonReflector refl = createJsonReflector(stream);
		JsonElement e = refl.getContext();
		this.validate(e, "root");
		JsonObject obj = e.getAsJsonObject();
		IServiceDefinition def = new JsonServiceDefinition();
		this.resolveService(obj, def, resItem);
		JsonElement bridge = obj.get("bridge");
		if (bridge != null)
			this.resolveBridge(bridge, def);
		JsonElement props = obj.get("properties");
		if (props != null)
			this.resolveFields(props, def);
		JsonElement mets = obj.get("methods");
		if (mets != null)
			this.resolveMethods(mets, def);
		return def;
	}

	private void resolveBridge(JsonElement bridge, IServiceDefinition def) {
		JsonObject node = bridge.getAsJsonObject();
		String aspects = node.get("aspects") == null ? "" : node.get("aspects")
				.getAsString();
		String valid = node.get("isValid") == null ? "" : node.get("isValid")
				.getAsString();
		boolean isValid = StringUtil.isEmpty(valid) ? true : Boolean
				.valueOf(valid);
		if (isValid) {
			BridgeDescriber bd = new BridgeDescriber();
			bd.setAspects(aspects);
			bd.setValid(isValid);
			def.getExtraDescribers().add(bd);
			int form = def.getServiceDescribeForm()
					| IServiceDefinition.BRIDGE_DESCRIBEFORM;
			def.setServiceDescribeForm((byte) form);
		}
	}

	private void resolveService(JsonObject obj, IServiceDefinition def,
			String resItem) {
		ServiceDescriber sd = new ServiceDescriber();
		def.setServiceDescriber(sd);
		int form = def.getServiceDescribeForm()
				| IServiceDefinition.SERVICE_DESCRIBEFORM;
		def.setServiceDescribeForm((byte) form);

		JsonElement des = obj.get("description");
		sd.setDescription(des.getAsString());
		JsonElement ser = obj.get("serviceId");
		JsonElement cla = obj.get("class");
		JsonElement isExotericE = obj.get("isExoteric");
		JsonElement constructor = obj.get("constructor");
		String className = ((cla != null) ? cla.getAsString() : resItem
				.replace("/", ".").replace(".class", ""));
		if (StringUtil.isEmpty(className)) {
			// 如果没有指定类名，则使用JSON文件的路径和文件名作为类名.
//			className = resItem.replace("/", ".").replace(".json", "");
			className = resItem.replace("/", ".");
			className=className.substring(0,className.lastIndexOf(".json"));
		}
		String serviceId = (ser == null ? "" : ser.getAsString());
		if (StringUtil.isEmpty(serviceId)) {
			serviceId = "$." + sd.getClassName();
		}
		sd.setServiceId(serviceId);
		sd.setClassName(className);
		JsonElement sco = obj.get("scope");
		String strScope = sco.getAsString();
		if (StringUtil.isEmpty(strScope))
			strScope = "singleon";
		sd.setScope(Scope.valueOf(strScope));
		sd.setConstructor(constructor==null?"":constructor.getAsString());
		boolean isExoteric=(isExotericE==null)?false:isExotericE.getAsBoolean();
		sd.setExoteric(isExoteric);
	}

	private void resolveMethods(JsonElement methods, IServiceDefinition def) {
		if (methods == null)
			return;
		this.validate(methods, "methods");
		JsonArray array = methods.getAsJsonArray();
		for (JsonElement e : array) {
			this.validate(e, "method");
			JsonObject obj = e.getAsJsonObject();
			JsonElement nameE = obj.get("alias");
			JsonElement fmnE = obj.get("bind");
			JsonElement cmE = obj.get("callMode");
			JsonElement argtypesE = obj.get("argTypes");
			JsonElement argsE = obj.get("args");
			String methodName = "";
			if (nameE != null) {
				methodName = nameE.getAsString();
				if (StringUtil.isEmpty(methodName)) {
					continue;
				}
			}
			ServiceMethod sm = new ServiceMethod();
			ParametersMehtodDescriber md = new ParametersMehtodDescriber();
			sm.getMethodDescribers().add(md);
			int form = sm.getMethodDescribeForm()
					| ServiceMethod.PARAMETERS_METHOD_DESCRIBEFORM;
			sm.setMethodDescribeForm((byte) form);
			def.getMethods().add(sm);
			sm.setAlias(methodName);
			String fmnStr = (fmnE==null||fmnE.getAsString()=="")?"<init>":fmnE.getAsString();
			sm.setBind(fmnStr);
			String cmode=cmE==null?"both":cmE.getAsString();
			sm.setCallMode(MethodMode.valueOf(StringUtil.isEmpty(cmode)?"both":cmode));
			if (argtypesE != null) {
				String str = argtypesE.getAsString();
				if (!StringUtil.isEmpty(str)) {
					String[] arr = str.split(",");
					sm.setParameterTypeNames(arr);
				}
			}
			if (argsE != null){
				this.resolveMethodArgs(argsE, md);
			}
			
			ReturnMehtodDescriber rmd = new ReturnMehtodDescriber();
			int f = sm.getMethodDescribeForm()
					| ServiceMethod.RETURN_METHOD_DESCRIBEFORM;
			sm.setMethodDescribeForm((byte) f);
			sm.getMethodDescribers().add(rmd);
			JsonElement resultE=obj.get("result");
			if (resultE != null && !"<init>".equals(sm.getBind())) {
				JsonObject resultjo=resultE.getAsJsonObject();
				String id = resultjo.get("byDefinitionId")==null?"":resultjo.get("byDefinitionId").getAsString();
				String type = resultjo.get("byDefinitionType")==null?"":resultjo.get("byDefinitionType").getAsString();
				rmd.setByDefinitionId(id);
				rmd.setByDefinitionType(type);
			}
		}
	}

	private void resolveMethodArgs(JsonElement args,
			ParametersMehtodDescriber md) {
		this.validate(args, "args");
		JsonArray arr = args.getAsJsonArray();
		for (JsonElement e : arr) {
			this.validate(e, "arg");
			JsonObject obj = e.getAsJsonObject();
			JsonElement vE = obj.get("value");
			JsonElement rE = obj.get("ref");
			String value = (vE == null ? "" : vE.getAsString());
			String ref = (rE == null ? "" : rE.getAsString());
			String injectMode = "";
			String argConf = "";
			if (!StringUtil.isEmpty(value)) {
				argConf = value;
				injectMode = "value";
			}
			if (!StringUtil.isEmpty(ref)) {
				injectMode = "ref";
				argConf = ref;
			}
			md.put(new String[] { argConf }, new String[] { injectMode });
		}

	}

	private void resolveFields(JsonElement props, IServiceDefinition def) {
		if (props == null)
			return;
		this.validate(props, "properties");
		JsonArray array = props.getAsJsonArray();
		for (JsonElement e : array) {
			this.validate(e, "property");
			JsonObject obj = e.getAsJsonObject();
			JsonElement nameE = obj.get("name");
			JsonElement rE = obj.get("refByName");
			JsonElement tE = obj.get("refByType");
			JsonElement fE = obj.get("refByMethod");
			JsonElement bE = obj.get("refByBridge");
			JsonElement vP = obj.get("parser");
			JsonElement vE = obj.get("value");

			JsonElement iE = obj.get("invertInjection");
			JsonElement sE = obj.get("serviceSite");
			String nameStr = "";
			if (nameE != null) {
				nameStr = nameE.getAsString();
				if (StringUtil.isEmpty(nameStr)) {
					continue;
				}
			}
			ServiceProperty sp = new ServiceProperty();
			def.getProperties().add(sp);
			sp.setPropName(nameStr);
			if (vE != null) {
				String vStr = vE.toString();
				if (!StringUtil.isEmpty(vStr)) {
					int form = sp.getPropDescribeForm()
							| ServiceProperty.VALUE_DESCRIBEFORM;
					sp.setPropDescribeForm((byte) form);
					ServicePropertyValueDescriber pvd = new ServicePropertyValueDescriber();
					String parser = (vP == null) ? "" : vP.getAsString();
					pvd.setParser(parser);
					pvd.setValue(vStr);
					sp.getPropertyDescribers().add(pvd);

				}
			}
			String rStr = (rE == null ? "" : rE.getAsString());
			String tStr = (tE == null ? "" : tE.getAsString());
			String fStr = (fE == null ? "" : fE.getAsString());
			if (!StringUtil.isEmpty(rStr) || !StringUtil.isEmpty(tStr)
					|| !StringUtil.isEmpty(fStr)) {
				int form = sp.getPropDescribeForm()
						| ServiceProperty.SERVICEREF_DESCRIBEFORM;
				sp.setPropDescribeForm((byte) form);
				ServiceRefDescriber srd = new ServiceRefDescriber();
				srd.setRefByName(rStr);
				srd.setRefByType(tStr);
				srd.setRefByMethod(fStr);
				if (bE != null) {
					JsonObject bridgeJo = bE.getAsJsonObject();
					JsonElement bUE= bridgeJo.get("useBridge");
					UseBridgeMode useBridge = bUE == null ? UseBridgeMode.normal :UseBridgeMode.valueOf(bUE
							.getAsString());
					srd.setUseBridge(useBridge);
					if (useBridge!=UseBridgeMode.forbidden) {
						JsonElement jpE = bridgeJo.get("joinpoint");
						if (jpE != null) {
							JsonObject jpO = jpE.getAsJsonObject();
							JsonElement aspectsE = jpO.get("aspects");
							BridgeJoinpoint bjp = new BridgeJoinpoint();
							bjp.setAspects(aspectsE.getAsString());
							srd.setBridgeJoinpoint(bjp);
						}
					}
				}
				sp.getPropertyDescribers().add(srd);
			}
			if (iE != null) {
				int form = sp.getPropDescribeForm()
						| ServiceProperty.SERVICEII_DESCRIBEFORM;
				sp.setPropDescribeForm((byte) form);
				boolean isForce = iE.getAsBoolean();
				ServiceInvertInjectionDescriber ssd = new ServiceInvertInjectionDescriber();
				sp.getPropertyDescribers().add(ssd);
				ssd.setForce(isForce);
			}
			if (sE != null) {
				String siteStr = (sE.getAsString() == null ? "false" : sE
						.getAsString());
				boolean isSite = Boolean.valueOf(siteStr);
				if (isSite) {
					ServiceSiteDescriber ssd = new ServiceSiteDescriber();
					sp.getPropertyDescribers().add(ssd);
					int form = sp.getPropDescribeForm()
							| ServiceProperty.SERVICESITE_DESCRIBEFORM;
					sp.setPropDescribeForm((byte) form);
				}
			}
		}
	}

}
