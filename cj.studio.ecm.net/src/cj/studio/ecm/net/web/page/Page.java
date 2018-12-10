package cj.studio.ecm.net.web.page;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.web.IPage;
import cj.studio.ecm.net.web.page.context.PageContext;

public abstract class Page implements IPage{

	@Override
	public final void flow(Frame frame,Circuit circuit, IPlug plug) throws CircuitException {
		PageContext ctx=(PageContext)plug.option("pageContext");
		if(ctx==null){
			throw new CircuitException(NetConstans.STATUS_503, "页面没有指定上下文。");
		}
		doPage(frame,circuit, plug,ctx);
	}

	protected abstract void doPage(Frame frame,Circuit circuit,IPlug plug, PageContext ctx)throws CircuitException;

}
