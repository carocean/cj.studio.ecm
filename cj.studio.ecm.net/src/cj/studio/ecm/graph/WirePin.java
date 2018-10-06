package cj.studio.ecm.graph;

import java.util.Map;

/**
 * 导线
 * 
 * <pre>
 *
 * </pre>
 * 
 * @author carocean
 *
 */
public class WirePin extends Pin implements IWirePin {
	protected IWirePinChangedEvent changedEvent;
	public WirePin() {
		// TODO Auto-generated constructor stub
	}
	public WirePin(String name) {
		options.put("$pinName", name);
	}
	public void setChangedEvent(IWirePinChangedEvent changedEvent) {
		this.changedEvent = changedEvent;
	}
	@SuppressWarnings("unchecked")
	@Override
	public Object options(String key) {
		// TODO Auto-generated method stub
		Object obj = super.options(key);
		if (obj != null) {
			return obj;
		}
		if (site() == null) {
			return null;
		}
		Map<String, Object> map = (Map<String, Object>) site()
				.getService("cable-options");
		if (map == null)
			return null;
		return map.get(key);
	}
	@Override
	protected Plug newPlug(String sinkName, Map<String, Object> options,
			ISink sink) {
		// TODO Auto-generated method stub
		Plug plug= super.newPlug(sinkName, options, sink);
		if(changedEvent!=null){
			changedEvent.changed(this,sinkName,sink);
		}
		return plug;
	}
}