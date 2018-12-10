package cj.studio.ecm.net.web.page.context;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.nio.NetConstans;
import cj.ultimate.IDisposable;

public abstract class PageContext extends HttpContext implements IDisposable {
	ISink lookup;

	public PageContext(ISink lookup, String httpRoot) {
		super(httpRoot);
		this.lookup = lookup;
	}

	@Override
	public void dispose() {
		lookup = null;
		super.dispose();
	}

	/**
	 * 请求并执行其它页面，并返回处理后的文档。
	 * 
	 * <pre>
	 * 可用此方法进页面进行组合。
	 * </pre>
	 * 
	 * @param frame
	 * @return
	 */
	public Document page(String relativedUrl, Frame frame,Circuit circuit, IPlug plug)
			throws CircuitException {
		String redirectUrl = String.format("%s/%s", frame.rootPath(),
				relativedUrl).replace("//", "/");
		if (frame.url().equals(redirectUrl)) {
			throw new CircuitException(NetConstans.STATUS_571, String.format("重定向到同一地址.%s",
					redirectUrl));
		}
		frame.parameter("routeMode", "redirect");
		frame.parameter("originalUrl", frame.url());
		frame.head("url", redirectUrl);
		lookup.flow(frame,circuit, plug);
		String html = "";
		if (circuit.content().readableBytes() > 0) {
			byte[] b = circuit.content().readFully();
			html = new String(b);
		}
		Document doc = Jsoup.parse(html);
		return doc;
	}
}
