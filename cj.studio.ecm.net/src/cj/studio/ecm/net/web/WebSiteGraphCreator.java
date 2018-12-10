package cj.studio.ecm.net.web;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.graph.AnnotationProtocolFactory;
import cj.studio.ecm.graph.IProtocolFactory;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.graph.Pin;
import cj.studio.ecm.graph.PinType;
import cj.studio.ecm.net.layer.NetLayerGraphCreator;
import cj.studio.ecm.net.web.sink.HttpSink;
import cj.studio.ecm.net.web.sink.PageSink;
import cj.studio.ecm.net.web.sink.WebSiteSink;
import cj.studio.ecm.net.web.sink.WidgetSink;
import cj.studio.ecm.net.web.sink.WsSink;

public class WebSiteGraphCreator extends NetLayerGraphCreator {

	public WebSiteGraphCreator() {
		// TODO Auto-generated constructor stub
	}
	@Override
	protected IProtocolFactory newProtocol() {
		IProtocolFactory factory = AnnotationProtocolFactory
				.factory(IWebSiteConstans.class);
		return factory;
	}
	@Override
	protected Pin createPin(String pinName,PinType t) {
		if(t==PinType.wire&&"output".equals(pinName)){
		return new HttpPin();
		}
		return null;
	}
	@Override
	public ISink createYourSink(String name) {
		ISink sink=null;
		switch(name){
		case "websitesink":
			sink=createWebSink();
			break;
		case "wssink":
			sink=createWsSink();
			break;
		case "httpsink":
			sink=createHttpSink();
			break;
		case "widgetsink":
			sink=createWidgetSink();
			break;
		case "pagesink":
			sink=createPageSink();
			break;
		}
		if(sink==null){
			throw new EcmException(String.format("请求的sink:%s为空", name));
		}
		return sink;
	}


	protected WsSink createWsSink(){
		return  new WsSink();
	}
	protected HttpSink createHttpSink(){
		return  new HttpSink();
	}
	protected WebSiteSink createWebSink(){
		return  new WebSiteSink();
	}
	protected PageSink createPageSink(){
		return  new PageSink();
	}
	
	protected WidgetSink createWidgetSink() {
		return new WidgetSink();
	}
	
}
