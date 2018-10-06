package cj.studio.ecm.logging;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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

import cj.ultimate.util.StringUtil;

//上下文
public class LoggingContext {
	//<log>
	private Map<String, String> logMap;
	//<outter name class
	private Map<String, OutterDefinition> outterMap;
	private ClassLoader loader;
	private String owner;
	public LoggingContext(String owner) {
		this.owner=owner;
	}
	public String getOwner() {
		return owner;
	}
	public Map<String, String> getLogMap() {
		return logMap;
	}
	public Map<String, OutterDefinition> getOutterMap() {
		return outterMap;
	}
	public ClassLoader getLoader() {
		return loader;
	}
	public void parse(InputStream in,ClassLoader loader) throws TransformerException, XPathExpressionException {
		this.loader=loader;
		logMap =new HashMap<String, String>(2);
		outterMap=new HashMap<String, OutterDefinition>();
		TransformerFactory transFact = TransformerFactory.newInstance();
		Transformer transFormer = transFact.newTransformer();
		DOMResult dom = new DOMResult();
		transFormer.transform(new StreamSource(in), dom);
		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression expression = xpath
				.compile("/logging/log");
		NodeList nodeList = (NodeList) expression.evaluate(dom.getNode(),
				XPathConstants.NODESET);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node=nodeList.item(i);
			String outter = xpath.evaluate("@outter", node);
			String level = xpath.evaluate("@level", node);
			logMap.put(outter, level);
		}
		
		XPathExpression outters = xpath
				.compile("/logging/outter");
		NodeList outterList = (NodeList) outters.evaluate(dom.getNode(),
				XPathConstants.NODESET);
		for (int i = 0; i < outterList.getLength(); i++) {
			Node outter=outterList.item(i);
			String name = xpath.evaluate("@name", outter);
			String cn = xpath.evaluate("@class", outter);
			OutterDefinition od=new OutterDefinition(name, cn);
			if(outterMap.containsKey(name))
				throw new RuntimeException(String.format("The outter %s exists, please check  file of log configration",name));
			outterMap.put(name, od);
			NodeList propList = (NodeList) xpath.evaluate("prop", outter,XPathConstants.NODESET);
			for (int j = 0; j< propList.getLength(); j++) {
				Node prop=propList.item(j);
				String pname = xpath.evaluate("@name", prop);
				String pvalue = xpath.evaluate("@value", prop);
				if(StringUtil.isEmpty(pname))
					throw new RuntimeException("the property name cann`t be null");
				od.getPropMap().put(pname, pvalue);
			}
		}
	}
	
}
