package cj.studio.ecm.net.web.sink;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.frame.IFeedback;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.web.SiteConfig;
import cj.studio.ecm.net.web.WebUtil;
import cj.ultimate.util.StringUtil;

public class HttpSink implements ISink {
	ILogging logger;

	public HttpSink() {
		logger = CJSystem.current().environment().logging();
	}

	@Override
	public void flow(Frame frame, Circuit circuit, IPlug plug)
			throws CircuitException {
		if (!"HTTP/1.1".equals(frame.protocol())
				&& !"CHUNKED/1.0".equals(frame.protocol())) {
			return;
		}

		try {
			setMime(frame, plug, circuit);
			plug.branch("filters").flow(frame, circuit);
		} catch (Exception e) {
			boolean isMatchedError=false;
			boolean isDoc = WebUtil.documentMatch(frame, plug);// 出错页面只拦截doc，对于资源要抛出异常，因为浏览器对资源有特殊处理。
			if (isDoc && !frame.containsHead("X-Requested-With")) {
				circuit.content().clear();
				CircuitException ce = CircuitException.search(e);
				
				if (ce == null) {// 放行未定义异常，未定义异常将由netGraph直接写回或者重定向到错误页面
					// CJSystem.current().environment().logging().error(getClass(),e.getMessage());
					isMatchedError=doDefaultError(e, frame, circuit, plug);
				} else {
					// CJSystem.current().environment().logging().error(getClass(),String.format("%s
					// %s", ce.getStatus(),ce.getMessage()));
					// 自定义错误页面处理：
					isMatchedError=doCustomError(ce, frame, circuit, plug);
				}
				if(isMatchedError)
					circuit.attribute("error-custom", "true");// 告诉net已处理错误，net就不再处理。
			}
			if(!isMatchedError){
				if (e instanceof CircuitException) {
					throw e;
				} else {
					throw new CircuitException(NetConstans.STATUS_503, e);
				}
			}else{
				logger.error(getClass(), e);
			}
		}
	}

	private void setMime(Frame frame, IPlug plug, Circuit circuit) {
		SiteConfig wc = (SiteConfig) plug
				.optionsGraph(SiteConfig.WEBCONFIG_KEY);
		String extName = frame.extName();
		if (wc.containsMime(extName)) {
			String type = wc.mime(extName);
			circuit.head("Content-Type", type);
			IFeedback fb = circuit.feedback(IFeedback.KEY_OUTPUT_FEEDBACK);
			if (fb != null) {
				fb.plugSink("setMime", new SetMimeSink()).option("type", type);
			}
		}
	}

	class SetMimeSink implements ISink {

		@Override
		public void flow(Frame frame, Circuit circuit, IPlug plug)
				throws CircuitException {
			if (StringUtil.isEmpty(frame.contentType())) {
				frame.head("Content-Type", (String) plug.option("type"));
			}
			plug.flow(frame, circuit);
		}
	}

	private boolean doDefaultError(Exception e, Frame frame, Circuit circuit,
			IPlug plug) throws CircuitException {
		SiteConfig wc = (SiteConfig) plug
				.optionsGraph(SiteConfig.WEBCONFIG_KEY);
		if (wc.containsError("default")) {
			String errorUri = wc.error("default");
			String origiUri = frame.url();
			String rootPath = frame.rootPath();
			frame.parameter("routeMode", "error");
			frame.parameter("originalUrl", origiUri);
			String newUrl = String.format("%s/%s", rootPath, errorUri)
					.replace("//", "/").replace("///", "/");
			frame.head("url", newUrl);
			plug.branch("filters").flow(frame, circuit);
			return true;
		} else {
			String text = String.format("%s %s<br/>%s", circuit.status(),
					circuit.message(), circuit.cause());
			circuit.content().writeBytes(text.getBytes());
			return false;
		}

	}

	private boolean doCustomError(CircuitException ce, Frame frame,
			Circuit circuit, IPlug plug) throws CircuitException {
		SiteConfig wc = (SiteConfig) plug
				.optionsGraph(SiteConfig.WEBCONFIG_KEY);

		if (wc.containsError(ce.getStatus())) {
			String errorUri = wc.error(ce.getStatus());
			String origiUri = frame.url();
			String rootPath = frame.rootPath();
			frame.parameter("routeMode", "error");
			frame.parameter("originalUrl", origiUri);
			String newUrl = String.format("%s/%s", rootPath, errorUri)
					.replace("//", "/").replace("///", "/");
			frame.head("url", newUrl);
			plug.branch("filters").flow(frame, circuit);
			return true;
		}
		return doDefaultError(ce, frame, circuit, plug);
	}

}
