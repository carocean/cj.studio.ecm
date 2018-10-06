package cj.studio.ecm.net.web;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import cj.studio.ecm.IChipInfo;
import cj.studio.ecm.IServiceAfter;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.graph.Access;
import cj.studio.ecm.graph.Graph;
import cj.studio.ecm.graph.GraphCreator;
import cj.studio.ecm.graph.IGraphHandler;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.IResult;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.graph.GraphCreatorHelper;
import cj.studio.ecm.net.web.page.Page;
import cj.studio.ecm.util.IConsole;

/**
 * 表示一个web站点，用于处理http协议
 * 
 * <pre>
 * 可直接接受浏览器请求，也可接受转发侦请求（该侦的内容须是httpFrame侦)
 * 限制：
 * 1.仅接受httpFrame侦，或侦的内容包含了httpFrame的侦。
 * 2.该类在服务onAfter时被初始化。
 * 功能：
 * 1.接受http,udt,local,tcp的侦请求，后三个的侦内容须是httpFrame侦数据。
 * 2.开放诸多受保护方法，可在派生类中根据需要覆盖。
 * 3.会话的拦截：
 * protected GraphCreator newCreator() {
 * 		return new WebSiteGraphCreator();
 * 	}
 * class MySiteGraphCreator extends WebSiteGraphCreator {
 * 		
 * 		protected SessionManager createSessionManager() {
 * 			// TODO Auto-generated method stub
 * 			SessionManager sm= super.createSessionManager();
 * 			sm.addEvent(new SessionManagerEvent());
 * 			return sm;
 * 		}
 * 	}
 * 	class SessionManagerEvent implements ISessionEvent {
 * 
 * 		
 * 		public void doEvent(String eventType, Object... args) {
 * 			System.out.println("MyWebSiteGraph :"+eventType+" "+((ISession)args[0]).id());
 * 			
 * 		}
 * 		
 * 	}
 * </pre>
 * 
 * @author carocean
 *
 */
public abstract class WebSiteGraph extends Graph implements IServiceAfter {
	Logger log = Logger.getLogger(WebSiteGraph.class);

	@Override
	public void onAfter(IServiceSite site) {
//		if (!isInit())
//			initGraph();
		IChipInfo ci = (IChipInfo) site.getService(IChipInfo.class.getName());
		if (options(SiteConfig.WEBCONFIG_KEY) == null) {

			SiteConfig wc = new SiteConfig();
			wc.parse(ci);
			options(SiteConfig.WEBCONFIG_KEY, wc);
		}
		// site().addService(clazz, service);//可实现callback功能
		

	}

	@Override
	protected GraphCreator newCreator() {
		return new WebSiteGraphCreator();
	}

	

	protected void buildIt(GraphCreator creator) {
		IPin output = creator.newWirePin("output", Access.output);
//		IPin input = creator.newCablePin("input", Access.input,new IWireBuilder() {
//			
//			@Override
//			public void build(String pinName, IWirePin wire) {
//				wire.plugLast("", creator.newSink("")).plugin("", creator.newSink(""));
//				
//			}
//		});
		IPin input = creator.newWirePin("input", Access.input);
		GraphCreatorHelper.attachSessionNetLayer(creator, input);
		ISink website = creator.newSink("websitesink");
		ISink wssink = creator.newSink("wssink");
		ISink httpsink = creator.newSink("httpsink");
		ISink widgetSink = creator.newSink("widgetsink");

		IPlug sitePlug = input.plugLast("website", website);
		IPin wsPin = creator.newWirePin("ws", sitePlug);
		IPin httpPin = creator.newWirePin("http", sitePlug);
		IPlug wsPlug = wsPin.plugLast("ws", wssink);
		IPlug httpPlug = httpPin.plugLast("http", httpsink);

		wsPlug.plugBranch("output", output);

		ServiceCollection<IHttpFilter> httpfilters = creator.site()
				.getServices(IHttpFilter.class);
		IPin hfilterPin = creator.newWirePin("filter");
		IHttpFilter hfarr[] = httpfilters.toArray(new IHttpFilter[0]);
		Arrays.sort(hfarr, new Comparator<IHttpFilter>() {
			public int compare(IHttpFilter o1, IHttpFilter o2) {
				if (o1.sort() > o2.sort()) {
					return 1;
				} else if (o1.sort() < o2.sort()) {
					return -1;
				} else {
					return 0;
				}
			};
		});
		for (IHttpFilter f : hfarr) {
			hfilterPin.plugLast(f.toString(), f);
		}
		ISink pageSink = creator.newSink("pagesink");
		IPlug pageSinkPlug = hfilterPin.plugLast("pageSink", pageSink);
		wsPlug.plugBranch("http", httpPin);
		httpPlug.plugBranch("filters", hfilterPin);

		Map<String,IPin> indexJavaPages=new HashMap<>();
		ServiceCollection<Page> pages = creator.site().getServices(Page.class);
		for (IPage p : pages) {
			CjService ann = p.getClass().getAnnotation(CjService.class);
			String name = ann.name();
			if (name.length() > 1 && name.endsWith("/")) {
				name = name.substring(0, name.length() - 1);
			}
			IPin pagePin = creator.newWirePin(name);
			pagePin.plugFirst("page", p);
			indexJavaPages.put(name, pagePin);
		}
		pageSinkPlug.option("javaPagePins",indexJavaPages);


		ServiceCollection<IWebsocketFilter> wsfilters = creator.site()
				.getServices(IWebsocketFilter.class);
		IPin wfilterPin = creator.newWirePin("filter");
		IWebsocketFilter wsfarr[] = wsfilters.toArray(new IWebsocketFilter[0]);
		Arrays.sort(wsfarr, new Comparator<IWebsocketFilter>() {
			public int compare(IWebsocketFilter o1, IWebsocketFilter o2) {
				if (o1.sort() > o2.sort()) {
					return 1;
				} else if (o1.sort() < o2.sort()) {
					return -1;
				} else {
					return 0;
				}
			};
		});
		for (IWebsocketFilter f : wsfarr) {
			wfilterPin.plugLast(f.toString(), f);
		}
		IPlug wsWidgetsPlug = wfilterPin.plugLast("widgetSink", widgetSink);
		wsPlug.plugBranch("filters", wfilterPin);

		ServiceCollection<IWidget> services = creator.site().getServices(
				IWidget.class);
		for (IWidget s : services) {
			CjService ann = s.getClass().getAnnotation(CjService.class);
			String name = ann.name();
			if (name.length() > 1 && name.endsWith("/")) {
				name = name.substring(0, name.length() - 1);
			}
			IPin sPin = creator.newWirePin(name);
			sPin.plugFirst("widget", s);
			wsWidgetsPlug.plugBranch(name, sPin);
		}
		
	}

	@Override
	protected final void build(GraphCreator creator) {
		buildIt(creator);
		site().addService("$.graph.handler", new GraphHandler());
		IChipInfo ci = (IChipInfo) site().getService(IChipInfo.class.getName());
		log.info(String.format("webSiteGraph[chipName:%s]已初始化...", ci.getName()));
	}

	class GraphHandler implements IGraphHandler {
		Object enteredNetSite(String cmd, Object... args) {
			Object ret = site().getService("$.cmdline(console)>");
			IResult result = (IResult) ret;
			IConsole console = (IConsole) result.value();
			// console.color("\033[0;31m");
			IChipInfo info = (IChipInfo) site().getService(
					IChipInfo.class.getName());
			console.println(String.format(
					"\r\nWebSiteGraph(%s.%s{%s#%s})使用说明书：", info.getName(),
					processId(), name(), protocolFactory().getProtocol()));
			console.println("\t1.含有input,output两个固定端子。output用于ws通讯,向远端输出信息");
			console.println("\t2.仅接受httpServer连接。");
			console.println(String.format("\t3.当前的进程号是:%s", processId()));
			console.println(String.format("\t4.当前的协议是:%s", protocolFactory()
					.getProtocol()));
			console.println(String
					.format("\t5.支持netsite向其要求新端子，但每次均返回固有的input,output同一实例"));
			console.println("连接信息:");

			ret = site().getService("$.cmdline(gc)>is -n websiteServer -o input");
			result = (IResult) ret;
			if (result.state()!=200) {
//				if (ret != null) {
//					String tips = String
//							.format("当前进程：%s 的输入端子:input 推荐与net名（websiteServer)连接，回撤跳过.\r\n请输入netName >",
//									processId());
//					String netname = console.waitInput(tips);
//					if (!StringUtil.isEmpty(netname)) {
//						String line = String.format(
//								"$.cmdline(gc)>plug -p input -o input -n %s",
//								netname);
//						ret = site().getService(line);
//						result = (IResult) ret;
//						if (result.state()==200) {
//							log.info("建立成功。");
//						} else if (result.state()!=200) {
//							System.out.println(result.message());
//						}
//					}
//					if (result.state()!=200) {
//						// console.colorBlack();
//						console.println(result.message());
//					}
//					tips = "输入output要插入的目标NET >";
//					netname = console.waitInput(tips);
//					if (!StringUtil.isEmpty(netname)) {
//						String line = String.format(
//								"$.cmdline(gc)>plug -p output -o output -n %s",
//								netname);
//						ret = site().getService(line);
//						result = (IResult) ret;
//						if (result.state()==200) {
//							log.info("建立成功。");
//						} else if (result.state()!=200) {
//							System.out.println(result.message());
//						}
//					}
//					// console.colorBlack();
//				}

			}
			if (result.state()!=200) {
				console.println(result.message());
			}
			return null;
		}

		@Override
		public Object event(String cmd,  Object... args) {
			if (IGraphHandler.EVENT_ENTERING_NETSITE.equals(cmd)) {
				return enteredNetSite(cmd, args);
			} else if (IGraphHandler.EVENT_DEMAND_PIN.equals(cmd)) {
				return demandPin(cmd, args);
			}

			return null;
		}

		// websitegraph暂不支持多pin，所以不论请求什么pin，均返回相同的输出和输出端子实例
		private Object demandPin(String cmd, Object[] args) {
			switch ((String) args[2]) {
			case "input":
				return in("input");
			case "output":
				return out("output");
			}
			return null;
		}

	}
}
