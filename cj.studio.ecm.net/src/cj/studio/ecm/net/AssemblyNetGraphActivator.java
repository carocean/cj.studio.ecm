package cj.studio.ecm.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cj.studio.ecm.IEntryPointActivator;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.context.IElement;
import cj.studio.ecm.graph.CircuitException;
/**
 * 用于组装netGraph事件
 * <pre>
 * 该方法在初始化服务器和客户端后开始
 * </pre>
 * @author carocean
 *
 */
public class AssemblyNetGraphActivator implements IEntryPointActivator{
	Logger logger=LoggerFactory.getLogger(AssemblyNetGraphActivator.class);
	ServiceCollection<INetGraphBuilder> ang;
	@Override
	public void activate(IServiceSite site,IElement args) {
		ang=site.getServices(INetGraphBuilder.class);
		if(ang.isEmpty())return;
		try{
		for(INetGraphBuilder g:ang){
			g.connectNetGraphs(site);
		}
		}catch(CircuitException e){
			CircuitException.throwExceptionIt(e, logger);
		}
	}

	@Override
	public void inactivate(IServiceSite site) {
		if(ang.isEmpty())return;
		
		for(INetGraphBuilder g:ang){
			g.disconnectNetGraphs(site);
		}
	}

}
