package cj.studio.ecm.net.web.sink;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.web.HttpCircuit;
import cj.studio.ecm.net.web.SiteConfig;
import cj.studio.ecm.net.web.widget.context.JavaWidgetContext;
import cj.studio.ecm.net.web.widget.context.JssWidgetContext;
import cj.studio.ecm.net.web.widget.context.WidgetContext;
import cj.studio.ecm.script.IJssModule;
import cj.ultimate.util.FileHelper;
import cj.ultimate.util.StringUtil;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class WidgetSink implements ISink {

	@Override
	public void flow(Frame frame, Circuit circuit, IPlug plug)
			throws CircuitException {

		String rpath = frame.relativePath();
		String url = "";
		if (rpath.equals("/")) {
			SiteConfig wc = (SiteConfig) plug
					.optionsGraph(SiteConfig.WEBCONFIG_KEY);
			String qstr = frame.queryString();
			rpath = wc.getWsWelcome();
			if (StringUtil.isEmpty(qstr)) {
				url = String.format("%s/%s", frame.path(), rpath);
			} else {
				url = String.format("%s/%s?%s", frame.path(), rpath, qstr);
			}
			frame.head("url", url.replace("///", "/").replace("//", "/"));
		}
		// if(frame.isDirectory()){
		// url=String.format("%s/%s", frame.url(),wc.getWsInDirDefaultDoc());
		// frame.head("url",url.replace("///", "/").replace("//", "/"));
		// }

		if (rpath.length() > 1 && rpath.endsWith("/")) {
			rpath = rpath.substring(0, rpath.length() - 1);
		}
		IPin branchPin = plug.branch(rpath);
		if (branchPin != null) {
			IPlug pagePlug = branchPin.getPlug("widget");
			HttpCircuit c = (HttpCircuit) circuit;
			WidgetContext ctx = new JavaWidgetContext(c.httpSiteRoot());// 页面上下文应由pageSink创建
			pagePlug.option("widgetContext", ctx);
			branchPin.flow(frame, circuit);
			return;
		}
		//下面是jss方式
		String filePath = FileHelper.getFilePathNoEx(rpath);
		filePath = filePath.replace("/", ".");
		String jssSelectName = "";
		if (filePath.startsWith(".")) {
			jssSelectName = String.format("$.cj.jss.%s%s",
					IJssModule.FIXED_MODULENAME_WS_JSS, filePath);
		} else {
			jssSelectName = String.format("$.cj.jss.%s.%s",
					IJssModule.FIXED_MODULENAME_WS_JSS, filePath);
		}
		Object jss = null;
		jss = plug.site().getService(jssSelectName);
		if (jss != null) {
			ScriptObjectMirror m = (ScriptObjectMirror) jss;
			if (!m.hasMember("doWidget")) {
				throw new CircuitException(NetConstans.STATUS_503,
						String.format("在jssWidget页面%s中没有发现doWidget方法。",
								jssSelectName));
			}
			JssWidgetContext ctx = new JssWidgetContext(
					((HttpCircuit) circuit).httpSiteRoot());
			try {
				m.callMember("doWidget", frame, circuit, plug, ctx);
			} catch (Exception e) {
				CJSystem.current().environment().logging().error(String
						.format("jssWidget页面脚本错误:%s,信息：%s", jssSelectName, e));
				throw new CircuitException(NetConstans.STATUS_503, e);
			}
			return;
		}
		throw new CircuitException(NetConstans.STATUS_404,
				String.format(
						"not found the widget,please check value of url:%s",
						frame.url()));

	}

	
}
