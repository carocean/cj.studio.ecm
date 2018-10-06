package cj.studio.ecm.net.web.widget;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.web.IWidget;
import cj.studio.ecm.net.web.widget.context.WidgetContext;

public abstract class Widget implements IWidget{

	@Override
	public final void flow(Frame frame,Circuit circuit, IPlug plug) throws CircuitException {
		WidgetContext ctx=(WidgetContext)plug.option("widgetContext");
		if(ctx==null){
			throw new CircuitException(NetConstans.STATUS_503, "widget没有指定上下文。");
		}
		doWidget(frame,circuit, plug,ctx);
	}

	protected abstract void doWidget(Frame frame,Circuit circuit,IPlug plug, WidgetContext ctx)throws CircuitException;

}
