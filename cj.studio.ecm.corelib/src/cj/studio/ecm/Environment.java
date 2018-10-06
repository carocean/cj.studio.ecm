package cj.studio.ecm;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cj.studio.ecm.global.ILanguage;
import cj.studio.ecm.global.Language;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.logging.LoggingConfigurator;
import cj.ultimate.util.StringUtil;

//为运行环境提供参数配置，它用于解析CjSystem.xml文件
//运行时环境，提供语言、日志等
public class Environment {
	private ILanguage language;
	private Map<String, String> properties;
	private ILogging systemLogging;

	public Environment(InputStream in) {
		try {
			this.parse(in);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void parse(InputStream in) throws ParserConfigurationException,
			SAXException, IOException, TransformerException,
			XPathExpressionException {
		properties = new HashMap<String, String>();
		TransformerFactory transFact = TransformerFactory.newInstance();
		Transformer transFormer = transFact.newTransformer();
		DOMResult dom = new DOMResult();
		transFormer.transform(new StreamSource(in), dom);
		XPath xpath = XPathFactory.newInstance().newXPath();
		parseGlobal(xpath, dom);
		parseLogging(xpath, dom);
	}

	private void parseLogging(XPath xpath, DOMResult dom)
			throws ParserConfigurationException, SAXException, IOException,
			TransformerException, XPathExpressionException {
		XPathExpression expression = xpath.compile("/environment/logging");
		Node loggin = (Node) expression.evaluate(dom.getNode(),
				XPathConstants.NODE);
		Node sysConf = (Node) xpath.evaluate("//system-config", loggin,
				XPathConstants.NODE);
		if (sysConf != null) {
			String lc = xpath.evaluate("text()", sysConf);
			lc = StringUtil.isEmpty(lc) ? "" : lc.trim();
			String de = xpath.evaluate("@demand", sysConf);
			properties.put("system-config-file", lc);
			properties.put("system-config-demand", de);
			InputStream in= Environment.class.getClassLoader().getResourceAsStream(lc);
			LoggingConfigurator conf=new LoggingConfigurator();
			conf.config(in,Environment.class.getClassLoader());
			systemLogging= conf.getLogging();
		}
		NodeList outs = (NodeList) xpath.evaluate("//chip-config", loggin,
				XPathConstants.NODESET);
		for (int i = 0; i < outs.getLength(); i++) {
			Node out = outs.item(i);
			String gr = xpath.evaluate("text()", out);
			gr = StringUtil.isEmpty(gr) ? "" : gr.trim();
			properties.put("chip-config-file", gr);
		}
	}

	private void parseGlobal(XPath xpath, DOMResult dom)
			throws ParserConfigurationException, SAXException, IOException,
			TransformerException, XPathExpressionException {
		XPathExpression expression = xpath.compile("/environment/global");
		Node globalN = (Node) expression.evaluate(dom.getNode(),
				XPathConstants.NODE);
		String lc = xpath.evaluate("//locale-code/text()", globalN);
		lc = StringUtil.isEmpty(lc) ? "" : lc.trim();
		String cr = xpath.evaluate("//global-repository/text()", globalN);
		String zr = xpath.evaluate("//chip-repository/text()", globalN);
		cr = StringUtil.isEmpty(cr) ? "" : cr.trim();
		zr = StringUtil.isEmpty(zr) ? "" : zr.trim();
		properties.put("locale-code", lc);
		properties.put("global-repository", cr);
		properties.put("chip-repository", zr);
		language = new Language(cr,lc);
		language.scan(lc,null);
	}

	public String getProperty(String prop) {
		return properties.get(prop);
	}


	public ILanguage language() {
		return language;
	}
	public ILogging logging(){
		return systemLogging;
	}
	public void demandLanguage(String code) {
		properties.put("demand-language-code", code);
		language.scan(code,null);
	}

}
