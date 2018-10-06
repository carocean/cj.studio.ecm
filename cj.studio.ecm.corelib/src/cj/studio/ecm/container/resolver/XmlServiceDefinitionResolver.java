package cj.studio.ecm.container.resolver;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cj.studio.ecm.EcmException;
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
import cj.studio.ecm.container.registry.XmlServiceDefinition;
import cj.studio.ecm.resource.IResource;
import cj.ultimate.util.StringUtil;

public class XmlServiceDefinitionResolver
		extends AbstractServiceDefinitionResolver {
	protected void validate(Element e, String nodeName) {
		if (!"service".equals(nodeName)) {
			throw new RuntimeException("xml服务配置不正确，根的配置必须为service");
		}
	}

	@Override
	public IServiceDefinition resolve(String resItem, IResource resource) {
		if (!resItem.endsWith(".xml"))
			return null;
		InputStream stream = resource.getResourceAsStream(resItem);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(stream);
			Element root = doc.getDocumentElement();
			IServiceDefinition def = new XmlServiceDefinition();
			this.resolveService(root, def, resItem);
			NodeList bridges = root.getElementsByTagName("bridge");
			if ((bridges != null) && (bridges.getLength() > 1))
				throw new RuntimeException("bridge must be one.");
			if (bridges != null)
				this.resolveBridge(bridges, def);
			NodeList props = root.getElementsByTagName("property");
			if (props.getLength() > 0)
				this.resolveFields(props, def);
			NodeList methods = root.getElementsByTagName("method");
			if (methods.getLength() > 0)
				this.resolveMethods(methods, def);
			return def;
			// return null;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void resolveBridge(NodeList bridges, IServiceDefinition def) {
		if (bridges == null || bridges.getLength() < 1)
			return;
		Node node = bridges.item(0);
		Element e = (Element) node;
		String aspects = e.getAttribute("aspects");
		String valid = e.getAttribute("isValid");
		boolean isValid = StringUtil.isEmpty(valid) ? true
				: Boolean.valueOf(valid);
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

	private void resolveMethods(NodeList methods, IServiceDefinition def) {
		for (int i = 0; i < methods.getLength(); i++) {
			Element methodE = (Element) methods.item(i);
			String alias = methodE.getAttribute("alias");
			String bind = methodE.getAttribute("bind");
			String cModeE = methodE.getAttribute("callMode");
			if (StringUtil.isEmpty(bind))
				bind = "<init>";
			String argTypes = methodE.getAttribute("argTypes");
			ServiceMethod sm = new ServiceMethod();
			ParametersMehtodDescriber md = new ParametersMehtodDescriber();
			sm.getMethodDescribers().add(md);
			int form = sm.getMethodDescribeForm()
					| ServiceMethod.PARAMETERS_METHOD_DESCRIBEFORM;
			sm.setMethodDescribeForm((byte) form);
			def.getMethods().add(sm);
			sm.setAlias(alias);
			sm.setBind(bind);
			sm.setCallMode(MethodMode
					.valueOf(StringUtil.isEmpty(cModeE) ? "both" : cModeE));
			if (!StringUtil.isEmpty(argTypes)) {
				String[] arr = argTypes.split(",");
				sm.setParameterTypeNames(arr);
			}
			NodeList argList = methodE.getElementsByTagName("arg");
			for (int j = 0; j < argList.getLength(); j++) {
				Element arg = (Element) argList.item(j);
				String ref = arg.getAttribute("ref");
				String value = arg.getAttribute("value");
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
			NodeList retList = methodE.getElementsByTagName("result");
			ReturnMehtodDescriber rmd = new ReturnMehtodDescriber();
			int f = sm.getMethodDescribeForm()
					| ServiceMethod.RETURN_METHOD_DESCRIBEFORM;
			sm.setMethodDescribeForm((byte) f);
			sm.getMethodDescribers().add(rmd);
			if (retList != null && !"<init>".equals(sm.getBind())) {
				if (retList.getLength() > 1) {
					throw new EcmException(
							String.format("配置了多个返回结果，在服务的方法：%s.%s",
									def.getServiceDescriber().getServiceId(),
									sm.getAlias()));
				}
				if (retList.getLength() == 1) {
					Element result = (Element) retList.item(0);
					String id = result.getAttribute("byDefinitionId");
					String type = result.getAttribute("byDefinitionType");
					rmd.setByDefinitionId(id);
					rmd.setByDefinitionType(type);
				}
			}
		}
	}

	private void resolveFields(NodeList props, IServiceDefinition def) {
		for (int i = 0; i < props.getLength(); i++) {
			Node node = props.item(i);
			Element propE = (Element) node;
			String name = propE.getAttribute("name");
			String value = propE.getAttribute("value");
			ServiceProperty sp = new ServiceProperty();
			def.getProperties().add(sp);
			sp.setPropName(name);
			NodeList valueList = propE.getElementsByTagName("value");
			if (!StringUtil.isEmpty(value)
					|| (valueList != null && valueList.getLength() > 0)) {
				int form = sp.getPropDescribeForm()
						| ServiceProperty.VALUE_DESCRIBEFORM;
				sp.setPropDescribeForm((byte) form);
				Node valueN = valueList.item(0);
				Element valueE = (Element) valueN;
				String parser = (valueE.getAttribute("parser"));
				parser = StringUtil.isEmpty(parser) ? "cj.basic" : parser;
				value = valueE.getTextContent();
				ServicePropertyValueDescriber pvd = new ServicePropertyValueDescriber();
				pvd.setValue(value);
				pvd.setParser(parser);
				sp.getPropertyDescribers().add(pvd);
			}
			NodeList refList = propE.getElementsByTagName("ref");
			if (refList.getLength() > 0) {
				Element refE = (Element) refList.item(0);
				String byName = refE.getAttribute("byName");
				String byType = refE.getAttribute("byType");
				String byMethod = refE.getAttribute("byMethod");
				Attr useBridgeA = refE.getAttributeNode("useBridge");
				UseBridgeMode useBridge = useBridgeA == null
						? UseBridgeMode.normal
						: UseBridgeMode.valueOf(useBridgeA.getValue());
				int form = sp.getPropDescribeForm()
						| ServiceProperty.SERVICEREF_DESCRIBEFORM;
				sp.setPropDescribeForm((byte) form);
				ServiceRefDescriber srd = new ServiceRefDescriber();
				srd.setRefByName(byName);
				srd.setRefByType(byType);
				srd.setRefByMethod(byMethod);
				srd.setUseBridge(useBridge);
				if (useBridge != UseBridgeMode.forbidden) {
					NodeList jpList = refE.getElementsByTagName("joinpoint");
					if (jpList != null && jpList.getLength() > 0) {
						Element jpE = (Element) jpList.item(0);
						String aspects = jpE.getAttribute("aspects");
						BridgeJoinpoint bjp = new BridgeJoinpoint();
						bjp.setAspects(aspects);
						srd.setBridgeJoinpoint(bjp);
					}

				}
				sp.getPropertyDescribers().add(srd);
			}
			NodeList invertList = propE.getElementsByTagName("invertInjection");
			if (invertList.getLength() > 0) {
				int form = sp.getPropDescribeForm()
						| ServiceProperty.SERVICEII_DESCRIBEFORM;
				sp.setPropDescribeForm((byte) form);
				Element invertE = (Element) invertList.item(0);
				String isForceStr = invertE.getAttribute("isForce");
				boolean isForce = StringUtil.isEmpty(isForceStr) ? false
						: Boolean.valueOf(isForceStr);
				ServiceInvertInjectionDescriber ssd = new ServiceInvertInjectionDescriber();
				sp.getPropertyDescribers().add(ssd);
				ssd.setForce(isForce);
			}
			NodeList serviceSiteList = propE
					.getElementsByTagName("serviceSite");
			if (serviceSiteList.getLength() > 0) {
				ServiceSiteDescriber ssd = new ServiceSiteDescriber();
				sp.getPropertyDescribers().add(ssd);
				int form = sp.getPropDescribeForm()
						| ServiceProperty.SERVICESITE_DESCRIBEFORM;
				sp.setPropDescribeForm((byte) form);
			}
		}

	}

	private void resolveService(Element root, IServiceDefinition def,
			String resItem) {
		this.validate(root, "service");
		String id = root.getAttribute("id");
		String className = root.getAttribute("class");
		String scope = root.getAttribute("scope");
		String constructor = root.getAttribute("constructor");
		String isExotericE = root.getAttribute("isExoteric");
		ServiceDescriber sd = new ServiceDescriber();
		def.setServiceDescriber(sd);
		int form = def.getServiceDescribeForm()
				| IServiceDefinition.SERVICE_DESCRIBEFORM;
		def.setServiceDescribeForm((byte) form);

		if (StringUtil.isEmpty(className)) {
			// 如果没有指定类名，则使用XML文件的路径和文件名作为类名.
			// className = resItem.replace("/", ".").replace(".xml", "");
			className = resItem.replace("/", ".");
			className = className.substring(0, className.lastIndexOf(".xml"));
		}
		String serviceId = id;
		if (StringUtil.isEmpty(serviceId)) {
			serviceId = "$." + sd.getClassName();
		}
		sd.setServiceId(serviceId);
		if (StringUtil.isEmpty(scope))
			scope = "singleon";
		sd.setScope(Scope.valueOf(scope));
		sd.setClassName(className);
		sd.setConstructor(constructor);
		boolean isExoteric = false;
		if (!StringUtil.isEmpty(isExotericE)) {
			isExoteric = Boolean.valueOf(isExoteric);
		}
		sd.setExoteric(isExoteric);
	}

}
