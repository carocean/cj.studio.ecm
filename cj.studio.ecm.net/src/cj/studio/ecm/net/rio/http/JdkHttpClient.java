package cj.studio.ecm.net.rio.http;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.graph.ICablePin;
import cj.studio.ecm.net.IClient;
import cj.studio.ecm.net.IConnectCallback;
import cj.studio.ecm.net.graph.INetGraph;
import cj.ultimate.util.StringUtil;

public class JdkHttpClient implements IClient {
	private Map<String, String> props;
	private JdkNetGraph netGraph;
	private IHttpChannel ch;

	public JdkHttpClient() {
		props = new HashMap<String, String>();
		props.put("status", "inited");
	}

	@Override
	public String netName() {
		CjService s = JdkHttpClient.this.getClass().getAnnotation(
				CjService.class);
		if (s == null) {
			String name = this.getProperty("clientName");
			if (StringUtil.isEmpty(name))// 如果为空则以端口号作为名字
				name = this.simple() + "$" + this.getProperty("host") + "#"
						+ this.getProperty("port");
			return name;
		}

		return s.name();
	}

	@Override
	public String status() {
		return props.get("status");
	}

	@Override
	public void setProperty(String key, String value) {
		props.put(key, value);
	}

	@Override
	public String getPort() {
		// TODO Auto-generated method stub
		return props.get("port");
	}

	@Override
	public String getHost() {
		return props.get("host");
	}

	@Override
	public void close() {
		props.clear();
		buildNetGraph().dispose();
		props.put("status", "stoped");
		
	}

	@Override
	public String getProperty(String name) {
		// TODO Auto-generated method stub
		return props.get(name);
	}

	@Override
	public String[] enumProp() {
		return props.keySet().toArray(new String[0]);
	}
	@Override
	public Object connect(String host, String contentPath)
			throws InterruptedException {
		return connect(host, contentPath, null);
	}
	/**
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param host
	 *            网站主机：不带端口
	 * @param contentPath
	 *            网站主机上的端口及站点上下文地址:8080/website/
	 */
	@Override
	public Object connect(String host, String contentPath, IConnectCallback callback)
			throws InterruptedException {
		int port = 80;

		Pattern p = Pattern.compile("^(\\d*).*");
		Matcher m = p.matcher(contentPath);
		if (!m.matches()) {
			throw new EcmException("上下文格式错误：" + contentPath);
		}

		if (m.groupCount() > 0) {
			String str = m.group(1);
			if (!StringUtil.isEmpty(str)) {
				port = Integer.valueOf(str);
			}
		}
		String portStr = String.valueOf(port);
		contentPath = contentPath.startsWith(portStr) ? contentPath.substring(
				portStr.length(), contentPath.length() )
				: contentPath;
		if(StringUtil.isEmpty(contentPath)){
			contentPath="/";
		}
		props.put("host", host);
		props.put("port", portStr);
		props.put("contentPath", contentPath.replace("//", "/"));
		ch = new JdkHttpChannel();
		INetGraph ng = buildNetGraph();
		if (!ng.isInit()) {
			ng.initGraph();
		}
		ICablePin in = (ICablePin) ng.netInput();
		in.options("http-channel", ch);
		in.newWire(Integer.toHexString(ch.hashCode()));
		in.options("cable-net","client");
//		in.newWire(netName());
		if(callback!=null){
			callback.buildGraph(this,ng);
		}
		ch.open(host, port, contentPath);
		props.put("status", "listening");
		return null;
	}
	
	@Override
	public INetGraph buildNetGraph() {
		if (netGraph == null) {
			this.netGraph = new JdkNetGraph(netName(),props);
		}
		return this.netGraph;
	}

	@Override
	public String simple() {
		return "rio-http";
	}

}
