package cj.studio.ecm.net.layer;

import cj.studio.ecm.IChipInfo;
import cj.studio.ecm.graph.GraphCreator;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.web.SiteConfig;

/**
 * 网络层构建器，提供会话层和路由层的功能。
 * 
 * <pre>
 *
 * </pre>
 * 
 * @author carocean
 *
 */
public abstract class NetLayerGraphCreator extends GraphCreator implements
		INetLayerGraphCreator {

	private ISessionManager sessionManager;

	public NetLayerGraphCreator() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public final ISessionManager getSessionManager() {
		if (sessionManager == null) {
			sessionManager = createSessionManager();
			IChipInfo ci = (IChipInfo) site().getService(IChipInfo.class.getName());
			SiteConfig wc =(SiteConfig)option(SiteConfig.WEBCONFIG_KEY) ;
			if (wc== null) {
				 wc = new SiteConfig();
				wc.parse(ci);
				option(SiteConfig.WEBCONFIG_KEY, wc);
			}
//			SiteConfig wc = (SiteConfig) option(SiteConfig.WEBCONFIG_KEY);
			sessionManager.siteConfig(wc);
			sessionManager.setName(site().getProperty("$.graph.name"));
		}
		return sessionManager;
	}

	protected ISessionManager createSessionManager() {
		return new DefaultSessionManager();
	}
	@Override
	protected final ISink createSink(String name) {
		ISink sink = null;
		switch (name) {
		case KEY_SESSION_SINK:
			sink = createSessionLayerCheckSink(getSessionManager());
			break;
		default:
			sink = createYourSink(name);
			break;
		}
		return sink;
	}



	protected SessionLayerCheckSink createSessionLayerCheckSink(
			ISessionManager sm) {
		return new SessionLayerCheckSink(sm);
	}

	/**
	 * 开发者可重载此方法以自定sink
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param name
	 * @return
	 */
	protected abstract ISink createYourSink(String name);

	class DefaultSessionManager extends SessionManager {

	}
}
