package cj.studio.ecm.graph;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.net.nio.NetConstans;
import cj.ultimate.util.StringUtil;

public abstract class Pin implements IPin, IPrinter {

	protected Map<String, Object> options;
	Plug first;
	private ILogging log;
	private IServiceSite site;
	IPinOptionsEvent optionsEvent;
	private boolean isDisposed;
	public Pin() {

		init();
	}
	@Override
	public void setOptionsEvent(IPinOptionsEvent optionsEvent) {
		this.optionsEvent = optionsEvent;
	}
	@Override
	public int count() {
		int i = 0;
		if (first == null)
			return i;
		Plug p = first;
		while (p != null) {
			i++;
			p = p.next;
		}
		return i;
	}

	protected void site(IServiceSite site) {
		this.site = site;
	}

	// @Override
	// public boolean isForbiddenNetLayer() {
	// // TODO Auto-generated method stub
	// return "true".equals(options.get(KEY_FORBIDDEN_NETLAYER));
	// }
	// @Override
	// public void setForbiddenNetLayer(boolean forbidden) {
	// options.put(KEY_FORBIDDEN_NETLAYER, String.valueOf(forbidden));
	// }
	/**
	 * 支持内部初始化
	 * 
	 * <pre>
	 *
	 * </pre>
	 */
	protected void init() {
		options = new HashMap<String, Object>();
		log=CJSystem.current().environment().logging();
	}

	public IServiceSite site() {
		return site;
	}

	@Override
	public synchronized void dispose() {
		if(isDisposed){
			return;
		}
		if (site != null) {
			ISocketTable st = (ISocketTable) site
					.getService("$.graph.socketTable");
			if (st != null) {
				st.remove(name());
			}
		}
		for (String name : enumSinkName()) {
			IPlug p = getPlug(name);
			if (p != null) {
				IBranchKey[] arr = p.enumBranchKey();
				for (IBranchKey bname : arr) {
					IPin bp = p.branch(bname);
					if (bp == null)
						continue;
					if ("true".equals(bp.options("disposed"))) {
						bp.dispose();
						bp.options("disposed", "true");
					}
					p.removeBranch(bname);
				}
			}
			unPlug(name);
		}
		site = null;
		options.clear();
		isDisposed=true;
	}
	@Override
	public boolean isDisposed() {
		return isDisposed;
	}
	@Override
	public String name() {
		return (String) options.get("$pinName");
	}

	@Override
	public void flow(Frame frame, Circuit circuit) throws CircuitException {

//		if (first == null) {
//			if (!demandPlugSinks())
//				return;
//		}
		if (first != null && first.plugin != null) {
			try {

				first.plugin.flow(frame, circuit, first);

			} catch (Exception e) {
				Throwable t = e;
				if (e instanceof InvocationTargetException) {
					t = ((InvocationTargetException) e).getTargetException();
				}
				CircuitException ce = CircuitException.search(t);
				if (ce == null) {
					ce = new CircuitException(NetConstans.STATUS_603, t);
				}
				ce.update(getGraphSimpleName(), name(), first.name,
						first.plugin.getClass());
				circuit.status(ce.status);
				circuit.message(ce.getMessage());
				circuit.cause(ce.messageCause());
				boolean fst = ce.isFirstPinEle(name());
				if ("error".equals(this.options("log.level")) && fst) {
					log.error(ce.messageCause());
				}
				throw ce;
			}
		}

	}

	private String getGraphSimpleName() {
		if (site == null)
			return "graph";
		String gn = (String) site.getProperty("$.graph.name");
		gn = gn.substring(gn.lastIndexOf(".") + 1, gn.length());
		return gn;
	}

	/**
	 * 如果当前端子为空，该方法确认是否需要插入sink，如果该方法插入了sink请返回true
	 * 
	 * @return
	 * @see 该方法常用于采用ioc注入sink到端子的模式
	 */
	protected boolean demandPlugSinks() {
		return false;
	}

	@Override
	public Object optionsGraph(String key) {
		Map<?, ?> map = (Map<?, ?>) site.getService("$.graph.options");
		return map.get(key);
	}

	@Override
	public void optionsGraph(String key, Object value) {
		if (key.startsWith("$")) {
			throw new EcmException("key不能使用带$的前缀，因为$被系统使用");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) site
				.getService("$.graph.options");
		map.put(key, value);
	}

	public String[] enumOptions() {
		return options.keySet().toArray(new String[0]);
	}

	@Override
	public Object options(String key) {
		return options.get(key);
	}

	@Override
	public void removeOptions(String key) {
		options.remove(key);
	}

	@Override
	public void options(String key, Object value) {
		if (key.startsWith("$")) {
			throw new EcmException("key 前缀$为系统所用，开发者不能指定为$");
		}
		options.put(key, value);
		if(optionsEvent!=null){
			optionsEvent.onPut(key,value);
		}
	}

	/**
	 */
	public void print(StringBuffer sb) {
		print(sb, " ");
	}

	public void print(StringBuffer sb, String indent) {
		sb.append("--------" + name() + "----------\r\n");
		if (first != null) {
			first.print(sb, indent);
		}
	}

	@Override
	public IPlug getPlug(String sinkName) {
		if (first == null) {
			return null;
		} else {
			Plug current = first;
			while (current != null) {
				if (current.name.equals(sinkName)) {
					return current;
				}
				current = current.next;
			}
		}
		return null;
	}

	@Override
	public String[] enumSinkName() {
		List<String> list = new ArrayList<String>();
		if (first == null) {
			return new String[0];
		} else {
			Plug current = first;
			while (current != null) {
				list.add(current.name);
				current = current.next;
			}
		}
		return list.toArray(new String[0]);
	}

	@Override
	public boolean contains(String sinkName) {
		if (first == null) {
			return false;
		} else {
			Plug current = first;
			while (current != null) {
				if (current.name.equals(sinkName))
					return true;
				current = current.next;
			}
		}
		return false;
	}

	private void configSinkPlugsMap(Plug p) {
		if (site == null)
			return;
		ISocketTable st = (ISocketTable) site()
				.getService("$.graph.socketTable");
		if (st == null)
			return;
		st.put(p, p.owner());

	}

	protected Plug newPlug(String sinkName, Map<String, Object> options,
			ISink sink) {
		if (sink instanceof SinkCreateBy) {// added by cj,time 2015.06.24
			((SinkCreateBy) sink).newSink(sinkName,this);
		}else if (sink instanceof ISinkCreateBy) {// added by cj,time 2015.06.24
			((ISinkCreateBy) sink).newSink(sinkName,this);
		}
		return new Plug(sinkName, options, site, sink);
	}

	@Override
	public Access access() {
		String v = (String) options.get("$.pin.access");
		if (StringUtil.isEmpty(v))
			v = "inner";

		return Access.valueOf(v);
	}

	// private Plug getAssignedPlug(String sinkName, ISink sink) {
	// Map<ISink, Map<String, Plug>> map = creator().mutiPlugSinks;
	// Map<String, Plug> plugs = map.get(sink);
	// return plugs == null ? null : plugs.get(name()+"/"+sinkName);
	// }
	private void removeAssegnedPlug(Plug p) {
		if (site == null)
			return;
		ISocketTable st = (ISocketTable) site()
				.getService("$.graph.socketTable");
		if (st == null)
			return;
		st.remove(p.plugin.hashCode(), p.owner());
	}

	@Override
	public IPlug plugLast(String sinkName, ISink sink) {
		if (sink == null)
			throw new EcmException("参数为空:" + sinkName);
		if (first == null) {
			Plug p = newPlug(sinkName, options, sink);
			p.plugin = sink;
			configSinkPlugsMap(p);
			first = p;
			return p;
		}
		Plug plug = (Plug) getPlug(sinkName);
		if (plug != null) {
			throw new EcmException("已存在sink：" + sinkName);
		}
		Plug current = first;
		while (current.next != null) {
			current = current.next;
		}

		Plug p = newPlug(sinkName, options, sink);
		current.next = p;
		p.plugin = sink;
		configSinkPlugsMap(p);
		return p;
	}

	@Override
	public IPlug plugFirst(String sinkName, ISink sink) {
		if (first == null) {
			Plug p = newPlug(sinkName, options, sink);
			p.plugin = sink;
			configSinkPlugsMap(p);
			first = p;
			return p;
		}
		Plug old = (Plug) getPlug(sinkName);
		if (old != null && sinkName.equals(old.name)) {
			throw new EcmException("已存在sink：" + sinkName);
		}
		Plug p = newPlug(sinkName, options, sink);
		p.next = first;
		p.plugin = sink;
		configSinkPlugsMap(p);
		first = p;
		return p;
	}

	@Override
	public IPlug plugBefore(String posSinkName, String sinkName, ISink sink) {
		if (sink == null)
			throw new EcmException(String.format("参数为空:%s", sinkName));
		if (first == null) {
			Plug p = newPlug(sinkName, options, sink);
			p.plugin = sink;
			configSinkPlugsMap(p);
			first = p;
			return p;
		}
		if (first.name.equals(posSinkName)) {
			Plug p = newPlug(sinkName, options, sink);
			p.plugin = sink;
			p.next = first;
			configSinkPlugsMap(p);
			first = p;
			return p;
		}
		Plug pos = (Plug) getPlug(posSinkName);
		if (pos == null) {
			throw new EcmException("指定的位置不存在：" + sinkName);
		}
		Plug old = (Plug) getPlug(sinkName);
		if (old != null) {
			throw new EcmException("已存在：" + sinkName);
		}
		Plug current = first;
		Plug next = current.next;
		while (next != null) {
			if (next.name.equals(posSinkName)) {
				Plug p = newPlug(sinkName, options, sink);
				current.next = p;
				p.next = next;
//				p.plugin = sink;
				configSinkPlugsMap(p);
				return p;
			}
			current = next;
			next = current.next;
		}

		return null;
	}

	@Override
	public IPlug plugAfter(String posSinkName, String sinkName, ISink sink) {
		if (sink == null)
			throw new EcmException("参数为空:" + sinkName);
		if (first == null) {
			Plug p = newPlug(sinkName, options, sink);
			p.plugin = sink;
			configSinkPlugsMap(p);
			first = p;
			return p;
		}
		Plug plug = (Plug) getPlug(posSinkName);
		if (plug == null) {
			throw new EcmException("指定的位置不存在：" + sinkName);
		}
		Plug newP = (Plug) getPlug(sinkName);
		if (newP != null) {
			throw new EcmException("已存在：" + sinkName);
		}
		Plug p = newPlug(sinkName, options, sink);
		plug.next = p;
		p.next = plug.next;
		p.plugin = sink;
		configSinkPlugsMap(p);
		return p;
	}

	@Override
	public IPlug plugReplace(String sinkName, ISink sink) {
		if (sink == null)
			throw new EcmException("参数为空:" + sinkName);
		Plug current = first;
		while (current != null) {
			if (sinkName.equals(current.name)) {
				IPlug old = current;
				current.plugin = sink;
				configSinkPlugsMap(current);
				old.dispose();
			}
			current = current.next;
		}
		return current;
	}

	@Override
	public void unPlug(String sinkName) {
		if(first==null)return;
		Plug current = first;
		if (current == null)
			return;
		if(sinkName.equals(first.name)){
			Plug del=first;
			this.first=first.next;
			removeAssegnedPlug(del);
			del.dispose();
			return;
		}
		Plug next = current.next;
		while (next != null) {
			if (sinkName.equals(next.name)) {
				current.next = next.next;
				removeAssegnedPlug(next/*current*/);
				next.dispose();
				break;
			}
			current = next;
			next = current.next;
		}
		
	}

	@Override
	public String toString() {
		return name();
	}
}
