package cj.studio.ecm.net.web.sink;

import java.util.Map;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.web.HttpCircuit;
import cj.studio.ecm.net.web.WebUtil;
import cj.studio.ecm.net.web.page.context.JavaPageContext;
import cj.studio.ecm.net.web.page.context.JssPageContext;
import cj.studio.ecm.net.web.page.context.ResourcePageContext;
import cj.studio.ecm.script.IJssModule;
import cj.ultimate.util.FileHelper;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * 分发处理。
 * 
 * <pre>
 * 1.静态资源
 * 2.文档
 * 3.java页和jss页
 * </pre>
 * 
 * @author carocean
 *
 */
public class PageSink implements ISink {
	private ISink redirect;

	public PageSink() {
		redirect = new RedirectPageSink();
	}

	@Override
	public void flow(Frame frame, Circuit circuit, IPlug plug)
			throws CircuitException {

		 // 注释掉的原因是资源越靠前返馈性能越好，因此放到了httpSink类中
		if (WebUtil.documentMatch(frame, plug)) {//
			// 目录(默认为index.html)、扩展名为html,htm(在assembly.properties中site.http.document=.html.htm)
			String rpath = frame.relativePath();
			if (rpath.length() > 1 && rpath.endsWith("/")) {
				rpath = rpath.substring(0, rpath.length() - 1);
			}
			@SuppressWarnings("unchecked")
			Map<String, IPin> javapagePins = (Map<String, IPin>) plug
					.option("javaPagePins");
			if (javapagePins.containsKey(rpath)) {// 有java页
				JavaPageContext ctx = new JavaPageContext(redirect,
						((HttpCircuit) circuit).httpSiteRoot(), rpath);
				IPin branchPin = javapagePins.get(rpath);
				IPlug theplug = branchPin.getPlug("page");
				theplug.option("pageContext", ctx);
				branchPin.flow(frame, circuit);
				return;
			}
			String filePath = FileHelper.getFilePathNoEx(rpath);
			filePath = filePath.replace("/", ".");
			String jssSelectName = "";
			if (filePath.startsWith(".")) {
				jssSelectName = String.format("$.cj.jss.%s%s",
						IJssModule.FIXED_MODULENAME_HTTP_JSS, filePath);
			} else {
				jssSelectName = String.format("$.cj.jss.%s.%s",
						IJssModule.FIXED_MODULENAME_HTTP_JSS, filePath);
			}
			Object jss = null;
			jss = plug.site().getService(jssSelectName);
			if (jss != null) {
				ScriptObjectMirror m = (ScriptObjectMirror) jss;
				if (!m.hasMember("doPage")) {
					throw new CircuitException(NetConstans.STATUS_503,
							String.format("在jssPage页面%s中没有发现doPage方法。",
									jssSelectName));
				}
				JssPageContext ctx = new JssPageContext(redirect,
						((HttpCircuit) circuit).httpSiteRoot());
				try {
					m.callMember("doPage", frame, circuit, plug, ctx);
				} catch (Exception e) {
					CJSystem.current().environment().logging()
							.error(String.format("jssPage页面脚本错误:%s,信息：%s",
									jssSelectName, e));
					throw new CircuitException(NetConstans.STATUS_503, e);
				}
				return;
			}
			// 不存在就放过去，后面将执行资源查询
		}
		//资源放到pageSink中回写性能没有放在httpSink中回写好，但是这是必须的，这是因为：
		//website内核支持模块架构，也就是支持模块对资源的拦载和处理，而如果资源在httpSink中即处理返回了
		//则跳过了httpsink后的过滤器对资源的拦截，因而模块中就得不到资源的处理请求了，故而仍放在pageSink中实现对资源的处理。
		//而在生产环境，多数资源是不经过应用神经元的，它在浏览器端即交给文件神经元处理，文件神经元不是website，它是直接缓存和回写资源文件用的。
		ResourcePageContext ctx = new ResourcePageContext(redirect,
				((HttpCircuit) circuit).httpSiteRoot());
		circuit.content().writeBytes(ctx.resource(frame.relativePath()));
	}

	class RedirectPageSink implements ISink {
		@Override
		public void flow(Frame frame, Circuit circuit, IPlug plug)
				throws CircuitException {

			PageSink.this.flow(frame, circuit, plug);
		}
	}
}
