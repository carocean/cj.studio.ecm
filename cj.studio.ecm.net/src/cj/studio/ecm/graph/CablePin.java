package cj.studio.ecm.graph;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.net.nio.NetConstans;
import cj.ultimate.util.StringUtil;
import io.netty.util.internal.ConcurrentSet;

/**
 * 电缆线
 * 
 * <pre>
 * 是导线的集线束
 * </pre>
 * 
 * @author carocean
 *
 */
public class CablePin extends Pin implements IPin, ICablePin, NetConstans {
	Map<String, IWirePin> wires;
	private String defaultWireName;
	private Set<ICablePinEvent> events;

	public CablePin(String pinName) {
		wires = new ConcurrentHashMap<String, IWirePin>();
		events = new ConcurrentSet<ICablePinEvent>();
	}

	@Override
	public void onEvent(ICablePinEvent e) {
		this.events.add(e);
	}

	@Override
	public void wireEvent(String wireName, IWirePinChangedEvent changedEvent) {
		if (!wires.containsKey(wireName))
			return;
		if (changedEvent == null)
			return;
		wires.get(wireName).setChangedEvent(changedEvent);
	}

	@Override
	public boolean containsOption(String key) {
		return options.containsKey(key);
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return wires.isEmpty();
	}

	@Override
	public void dispose() {
		String[] keys = wires.keySet().toArray(new String[0]);
		for (String k : keys) {
			removeWire(k);
		}
		wires.clear();
		events.clear();
		super.dispose();
	}

	@Override
	public String defaultWireName() {
		return defaultWireName;
	}

	@Override
	public Object wireOptions(String wireName, String key) {
		if (wires.containsKey(wireName)) {
			return wires.get(wireName).options(key);
		}
		return null;
	}

	@Override
	public void removeWireOptions(String wireName, String key) {
		if (!wires.containsKey(wireName)) {
			return;
		}
		wires.get(wireName).removeOptions(key);
	}

	@Override
	public void wireOptions(String wireName, String key, Object value) {
		if (!wires.containsKey(wireName)) {
			return;
		}
		wires.get(wireName).options(key, value);
	}

	@Override
	public void flow(Frame frame, Circuit circuit) throws CircuitException {
		if (wires.isEmpty())
			return;
		if (wires.size() == 1) {
			Set<Entry<String, IWirePin>> it = wires.entrySet();
			for (Entry<String, IWirePin> en : it) {
				en.getValue().flow(frame, circuit);
			}
			return;
		} else {
			Set<Entry<String, IWirePin>> it = wires.entrySet();
			IPin idle = null;
			for (Entry<String, IWirePin> en : it) {
				IPin p = en.getValue();
				if (!"busy".equals(p.options(PIN_WORK_STATUS_KEY))) {
					idle = p;
					break;
				}
			}
			if (idle == null) {
				flowRandomSelect(frame, circuit);
			} else {
				idle.flow(frame, circuit);
			}
		}
	}

	@Override
	public void flowAll(Frame frame, Circuit circuit) throws CircuitException {
		if (wires.size() == 1) {
			Set<Entry<String, IWirePin>> it = wires.entrySet();
			for (Entry<String, IWirePin> en : it) {
				en.getValue().flow(frame, circuit);
			}
			return;
		}
		Set<Entry<String, IWirePin>> it = wires.entrySet();
		for (Entry<String, IWirePin> en : it) {
			en.getValue().flow(frame.copy(), circuit);
		}
	}

	@Override
	public void flow(String selectWireName, Frame frame, Circuit circuit)
			throws CircuitException {
		if (StringUtil.isEmpty(selectWireName)) {
			if (wires.isEmpty()) {
				throw NetConstans.newCircuitException(NetConstans.STATUS_605);
			}
			IWirePin w = wires.get(defaultWireName);
			w.flow(frame, circuit);
			return;
		}
		IWirePin p = wires.get(selectWireName);
		if (p == null) {
			throw NetConstans.newCircuitException(NetConstans.STATUS_604,
					selectWireName);
		}
		p.flow(frame, circuit);
	}

	@Override
	public void flowRandomSelect(Frame frame, Circuit circuit)
			throws CircuitException {
		if (wires.isEmpty()) {
			throw NetConstans.newCircuitException(NetConstans.STATUS_605);
		}
		String[] keys = wires.keySet().toArray(new String[0]);
		String key = keys[Math.abs(frame.hashCode()) % keys.length];
		IWirePin w = wires.get(key);
		if (w == null) {
			throw NetConstans.newCircuitException(NetConstans.STATUS_605);
		}
		w.flow(frame, circuit);

	}

	@Override
	public void link(String theWireName, ICablePin other, String otherWireName,
			ISink sink) {
		IWirePin the = wires.get(theWireName);
		IWirePin o = ((CablePin) other).wires.get(otherWireName);
		the.plugLast(String.format("%s_link", otherWireName), sink)
				.plugBranch(otherWireName, o);

	}

	@Override
	public int wireCount() {
		return wires.size();
	}

	@Override
	public void emptyWire() {
		for (String k : wires.keySet()) {
			if (!k.equals(name())) {
				wires.get(k).dispose();
				wires.remove(k);
			}
		}
	}

	@Override
	protected void site(IServiceSite site) {
		super.site(new CableSite(site));
	}

	public boolean containsWire(String name) {
		return wires.containsKey(name);
	}

	// @Override
	// public void options(String key, Object value) {
	// super.options(key, value);
	// Set<String> keys=wires.keySet();
	// for(String k:keys){
	// IPin p=wires.get(k);
	// p.options(key, value);
	// }
	// }
	@Override
	public boolean newWire(String name) {// 电线内的导线都是内部pin,而且仅在电览线内可见。
		return newWire(name, null);
	}

	@Override
	public boolean newWire(String name, ICableWireCreateCallback cb) {// 电线内的导线都是内部pin,而且仅在电览线内可见。
		if (wires.containsKey(name)) {
			return false;
		}
		try {
			CableWirePin wire = new CableWirePin();
			// wire.options.putAll(super.options);
			wire.options.put("$pinName", name);
			wire.site(site());
			Plug p = first;
			boolean isWireEmpty = wires.isEmpty();
			if (isWireEmpty)
				defaultWireName = name;
			while (p != null) {
				ISink sink = null;
				if (p.plugin instanceof SinkCreateBy) {
					SinkCreateBy by = (SinkCreateBy) p.plugin;
					sink = by.newSink(p.name, wire);
				} else if (p.plugin instanceof ISinkCreateBy) {
					ISinkCreateBy by = (ISinkCreateBy) p.plugin;
					sink = by.newSink(p.name, wire);
				} else {
					sink = p.plugin;
				}
				ChildrenPlug cp = (ChildrenPlug) wire.plugLast(p.name, sink);
				cp.setParent(p);
				p = p.next;
			}
			wires.put(name, wire);
			if (cb != null) {
				cb.createdWire(name, wire);
			}
			for (ICablePinEvent e : events) {
				e.onNewWired(this, name, wire);
			}
			return true;
		} catch (Exception e) {
			CJSystem.current().environment().logging().error(getClass(), e);
			// e.printStackTrace();
			return false;
		}
	}

	@Override
	public void removeWire(String name) {
		IWirePin p = wires.get(name);
		if (p == null)
			return;

		for (ICablePinEvent e : events) {
			e.onDestoringWired(this, name, p);
		}

		wires.remove(name);

		for (ICablePinEvent e : events) {
			e.onDestoryWired(this, name, p);
		}

		if (!p.isDisposed()) {
			p.dispose();
		}
	}

	@Override
	public String[] enumWire() {
		return wires.keySet().toArray(new String[0]);
	}

	@Override
	public IPlug plugLast(String sinkName, ISink sink) {

		if (wires.isEmpty()) {
			return super.plugLast(sinkName, sink);
		}
		IPlug plug = super.plugLast(sinkName, sink);
		Set<String> set = wires.keySet();
		for (String key : set) {
			IPin p = wires.get(key);
			if (sink instanceof SinkCreateBy) {
				sink = ((SinkCreateBy) sink).newSink(sinkName, p);
			} else if (sink instanceof ISinkCreateBy) {
				sink = ((ISinkCreateBy) sink).newSink(sinkName, p);
			}
			ChildrenPlug cp = (ChildrenPlug) p.plugLast(sinkName, sink);
			cp.setParent(plug);
		}
		return plug;
	}

	@Override
	public IPlug plugAfter(String posSinkName, String sinkName, ISink sink) {
		if (wires.isEmpty()) {
			return super.plugAfter(posSinkName, sinkName, sink);
		}
		IPlug plug = super.plugAfter(posSinkName, sinkName, sink);
		Set<String> set = wires.keySet();
		for (String key : set) {
			IPin p = wires.get(key);
			if (sink instanceof SinkCreateBy) {
				sink = ((SinkCreateBy) sink).newSink(sinkName, p);
			} else if (sink instanceof ISinkCreateBy) {
				sink = ((ISinkCreateBy) sink).newSink(sinkName, p);
			}
			ChildrenPlug cp = (ChildrenPlug) p.plugAfter(posSinkName, sinkName,
					sink);
			cp.setParent(plug);
		}
		return plug;
	}

	@Override
	public IPlug plugBefore(String posSinkName, String sinkName, ISink sink) {
		if (wires.isEmpty()) {
			return super.plugBefore(posSinkName, sinkName, sink);
		}
		IPlug plug = super.plugBefore(posSinkName, sinkName, sink);
		Set<String> set = wires.keySet();
		for (String key : set) {
			IPin p = wires.get(key);
			if (sink instanceof SinkCreateBy) {
				sink = ((SinkCreateBy) sink).newSink(sinkName, p);
			} else if (sink instanceof ISinkCreateBy) {
				sink = ((ISinkCreateBy) sink).newSink(sinkName, p);
			}
			ChildrenPlug cp = (ChildrenPlug) p.plugBefore(posSinkName, sinkName,
					sink);
			cp.setParent(plug);
		}
		return plug;
	}

	@Override
	public IPlug plugFirst(String sinkName, ISink sink) {
		if (wires.isEmpty()) {
			return super.plugFirst(sinkName, sink);
		}
		IPlug plug = super.plugFirst(sinkName, sink);
		Set<String> set = wires.keySet();
		for (String key : set) {
			IPin p = wires.get(key);
			if (sink instanceof SinkCreateBy) {
				sink = ((SinkCreateBy) sink).newSink(sinkName, p);
			} else if (sink instanceof ISinkCreateBy) {
				sink = ((ISinkCreateBy) sink).newSink(sinkName, p);
			}
			ChildrenPlug cp = (ChildrenPlug) p.plugFirst(sinkName, sink);
			cp.setParent(plug);
		}
		return plug;
	}

	@Override
	public synchronized void unPlug(String sinkName) {
		// if (wires.isEmpty()) {//注掉原因：当解除时电览线自身也应该解除该sink
		super.unPlug(sinkName);
		// return;
		// }
		Set<String> set = wires.keySet();
		for (String key : set) {
			IWirePin p = wires.get(key);
			p.unPlug(sinkName);
		}
	}

	@Override
	public IPlug plugReplace(String sinkName, ISink sink) {
		if (wires.isEmpty()) {
			return super.plugReplace(sinkName, sink);
		}
		IPlug plug = super.plugReplace(sinkName, sink);
		Set<String> set = wires.keySet();
		for (String key : set) {
			IPin p = wires.get(key);
			if (sink instanceof SinkCreateBy) {
				sink = ((SinkCreateBy) sink).newSink(sinkName, p);
			} else if (sink instanceof ISinkCreateBy) {
				sink = ((ISinkCreateBy) sink).newSink(sinkName, p);
			}
			ChildrenPlug cp = (ChildrenPlug) p.plugReplace(sinkName, sink);
			cp.setParent(plug);
		}
		return plug;
	}

	@Override
	public IPlug getPlug(String sinkName) {
		return super.getPlug(sinkName);
	}

	class CableSite implements IServiceSite {

		private IServiceSite site;

		public CableSite(IServiceSite site) {
			this.site = site;
		}

		@Override
		public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
			return site.getServices(serviceClazz);
		}

		@Override
		public Object getService(String serviceId) {
			if ("cable-options".equals(serviceId)) {
				return options;
			}
			return site.getService(serviceId);
		}

		@Override
		public void addService(Class<?> clazz, Object service) {
			site.addService(clazz, service);

		}

		@Override
		public void removeService(Class<?> clazz) {
			site.removeService(clazz);
		}

		@Override
		public void addService(String serviceName, Object service) {
			// TODO Auto-generated method stub
			site.addService(serviceName, service);
		}

		@Override
		public void removeService(String serviceName) {
			// TODO Auto-generated method stub
			site.removeService(serviceName);
		}

		@Override
		public String getProperty(String key) {
			// TODO Auto-generated method stub
			return site.getProperty(key);
		}

		@Override
		public String[] enumProperty() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	class CableWirePin extends WirePin {
		@Override
		protected Plug newPlug(String sinkName, Map<String, Object> options,
				ISink sink) {
			if (sink instanceof SinkCreateBy) {// added by cj,time 2015.06.24
				((SinkCreateBy) sink).newSink(sinkName, this);
			}
			Plug plug = new ChildrenPlug(sinkName, options, site(), sink);
			if (changedEvent != null) {
				changedEvent.changed(this, sinkName, sink);
			}
			return plug;
		}
	}
}
