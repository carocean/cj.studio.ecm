package cj.studio.ecm.net.web.sink;

import java.io.File;

import cj.studio.ecm.IChipInfo;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.layer.ISession;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.web.HttpCircuit;
import cj.studio.ecm.net.web.HttpFrame;
import cj.studio.ecm.script.IJssModule;
import cj.ultimate.util.StringUtil;

public class WebSiteSink implements ISink {

	@Override
	public void flow(Frame frame, Circuit circuit2,IPlug plug) throws CircuitException {
		if (!(frame instanceof HttpFrame)) {
			doOtherNewProtocol(frame,circuit2, plug);
			return;
		}
		
		HttpCircuit circuit = (HttpCircuit) circuit2;
		IChipInfo info = plug.chipInfo();
		String http_root = String
				.format("%s/%s/%s", info.getProperty("home.dir"),
						IJssModule.RUNTIME_SITE_DIR,
						info.getResourceProp("http.root")).replace("///", "/")
				.replace("//", "/").replace("/", File.separator);
		circuit.attribute("http.root", http_root);
		IPin p = null;
		if (!"ws".equals(frame.head("sub-protocol"))/*"http".equals(circuit.attribute("select-simple"))*//*!isWebSocketReq(frame)*/) {
			p = plug.branch("http");
		} else {
			p = plug.branch("ws");
		}
		if (p == null) {
			throw new CircuitException(NetConstans.STATUS_503, "webSiteGraph已损坏。");
		}
		p.flow(frame, circuit);
	}
//	private boolean isWebSocketReq(Frame frame) {
//		return ((frame.head("Connection").contains("Upgrade")) && "websocket"
//				.equalsIgnoreCase(frame.head("Upgrade")));
//	}
	/**
	 * 如果是其它传输协议，则尝试检查其内容是否含有http请求侦，如有则解侦，并发起http请求
	 * 
	 * <pre>
	 * http转发原理：开始于一个httpServer接受到浏览器请求(必须见附注1），得到一个httpFrame侦，和httpCircuit回路,
	 * 而后可以使用其它net传输它，而最终的接受端是websiteGraph，因此需要将net来的侦解包为httpFrame
	 * </pre>
	 * 
	 * 1.为什么必须由浏览器发起请求，因为无论netty的http客户端或者开源的网页抓取程序，均不可能像浏览器那样对网页内的资源链接发起再次请求，
	 * 因此编程抓取模式，只有文本而无整个页面，因此必须由浏览器发起
	 * 
	 * @param frame
	 * @param plug
	 * @throws CircuitException
	 */
	// 这段仍未测试
	protected void doOtherNewProtocol(Frame frame, Circuit circuit,IPlug plug)
			throws CircuitException {
		byte[] b = frame.content().readFully();
		HttpFrame f = null;
		boolean isHttpFrame = false;
		try {
			f = new HttpFrame(b);
			isHttpFrame = StringUtil.isEmpty(f.protocol()) ? false : true;
		} catch (Exception e) {
			isHttpFrame = false;
		}
		if (!isHttpFrame) {
			String err = String
					.format("程序集%s不支持的侦，webgraph:%s,%s仅支持httpFrame.当前侦的content不是httpFrame类型的侦数据.",
							plug.chipInfo().getName(),
							plug.optionsGraph("$graphName"), plug.protocol().getProtocol());
			throw new CircuitException(NetConstans.STATUS_801, err);
		}
		Object session =circuit.attribute("session");
		if (session == null) {
			String err = String.format(
					"%s,%s未使用会话层，因此websitegraph不能处理该服务器发来的请求。", circuit
							.attribute("select-name"), circuit
							.attribute("select-simple"));
			throw new CircuitException(NetConstans.STATUS_803, err);
		}
		f.setSession((ISession) session);
		HttpCircuit c = new HttpCircuit("http/1.1 200 ok");
		IChipInfo info = plug.chipInfo();
		String http_root = String
				.format("%s/%s/%s", info.getProperty("home.dir"),
						IJssModule.RUNTIME_SITE_DIR,
						info.getResourceProp("http.root")).replace("///", "/")
				.replace("//", "/").replace("/", File.separator);
		c.attribute("http.root", http_root);
		IPin p = null;
		if ("http".equals(frame.parameter("select-simple"))) {
			p = plug.branch("http");
		} else if ("ws".equals(frame.parameter("select-simple"))) {
			p = plug.branch("ws");
		} else {
			String err = String
					.format("请求程序集%s的webgraph:%s,%s,未在frame.parameter(select-simple)中指定通路：http还是ws.",
							plug.chipInfo().getName(),
							plug.optionsGraph("$graphName"), plug.protocol().getProtocol());
			throw new CircuitException(NetConstans.STATUS_802, err);
		}
		if (p == null) {
			throw new CircuitException(NetConstans.STATUS_503, "webSiteGraph已损坏。");
		}
		try {
			plug.flow(f, c);
			circuit.copyFrom(c, true);
			f.dispose();
			c.dispose();
		} catch (Exception e) {
			throw new CircuitException(NetConstans.STATUS_805, e);
		}
	}
}
