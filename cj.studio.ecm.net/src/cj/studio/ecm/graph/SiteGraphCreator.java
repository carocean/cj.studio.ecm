package cj.studio.ecm.graph;


/**
 * 提供服务sink
 * <pre>
 *
 * </pre>
 * @author carocean
 *
 */
public class SiteGraphCreator extends GraphCreator {
	IProtocolFactory protocolFactory;
	public SiteGraphCreator(IProtocolFactory protocolFactory) {
		this.protocolFactory=protocolFactory;
	}

	@Override
	public ISink createSink(String sink) {
		return (ISink)site.getService(sink);
	}
	@Override
	protected IProtocolFactory newProtocol() {
		return protocolFactory;
	}
}
