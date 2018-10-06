package cj.studio.ecm.graph;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IChipInfo;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.net.nio.NetConstans;
import cj.ultimate.IDisposable;
import cj.ultimate.util.StringUtil;

public class Plug implements IPlug {
	Map<String, Object> options;// 引用pin的选项
	HashMap<IBranchKey, IPin> branches;
	String name;
	IServiceSite site;

	ISink plugin;
	Plug next;
	private boolean isDisposed;
	static String KEY_OPTIONS_PIN = "$.pin.options";
	// private Logger log = LoggerFactory.getLogger(this.getClass());

	public Plug(String sinkName, Map<String, Object> optionsPin,
			IServiceSite site, ISink sink) {
		this.name = sinkName;
		this.options = new HashMap<>();
		this.options.put(KEY_OPTIONS_PIN, optionsPin);
		branches = new HashMap<IBranchKey, IPin>();
		this.plugin = sink;
		this.site = site;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object optionsPin(String key) {
		Map<String, Object> opPin = (Map<String, Object>) this.options
				.get(KEY_OPTIONS_PIN);
		if (opPin.containsKey(key)) {
			return opPin.get(key);
		} else {
			Map<String, Object> map = (Map<String, Object>) site()
					.getService("cable-options");
			if (map == null)
				return null;
			return map.get(key);
		}
	}
	@Override
	public void optionsPin(String key,Object value){
		@SuppressWarnings("unchecked")
		Map<String, Object> opPin = (Map<String, Object>) this.options
				.get(KEY_OPTIONS_PIN);
		opPin.put(key, value);
	}
	@Override
	public String fromPinName() {
		return (String) optionsPin("$pinName");
	}

	public IAtom atom(String name) {
		IAtom atom = IAtom.get(name, site);
		if (atom == null)
			return null;
		atom.bind(this);
		return atom;
	}

	@Override
	public Access fromPinAccess() {
		String v = (String) optionsPin("$.pin.access");
		if (StringUtil.isEmpty(v))
			v = "inner";

		return Access.valueOf(v);
	}

	@Override
	public IProtocolFactory protocol() {
		return (IProtocolFactory) this.site()
				.getService("$.graph.protocolFactory");
	}

	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		return next == null ? false : true;
	}

	@Override
	public void emptyBranches() {
		branches.clear();
	}

	@Override
	public Object optionsGraph(String key) {
		if(site==null)return null;
		Map<?, ?> map = (Map<?, ?>) site.getService("$.graph.options");
		if(map==null)return null;
		return map.get(key);
	}

	@Override
	public void optionsGraph(String key, Object value) {
		if (key.startsWith("$")) {
			throw new RuntimeException("key不能使用带$的前缀，因为$被系统使用");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) site
				.getService("$.graph.options");
		map.put(key, value);
	}

	@Override
	public String fullName() {
		return (String) optionsPin("$pinName") + "#" + name;
	}

	@Override
	public String owner() {
		return (String) optionsPin("$pinName");
	}

	@Override
	public void dispose() {

		if (plugin != null) {
			if (plugin instanceof IDisposable) {
				((IDisposable) plugin).dispose();
				plugin = null;
			}

		}
		options .clear();;
		branches.clear();
		next = null;
		name = null;
		site = null;
		isDisposed=true;
	}
	@Override
	public boolean isDisposed() {
		return isDisposed;
	}
	public Plug plugin(String sinkName, ISink sink) {
		Plug plug = null;
		plug = new Plug(sinkName, options, site, sink);
		if (next != null) {
			plug.next = next;
		}
		next = plug;
		ISocketTable st = (ISocketTable) site()
				.getService("$.graph.socketTable");
		if (st != null)
			st.put(plug, owner());
		return plug;
	}

	public IServiceSite site() {
		return site;
	}

	@Override
	public IChipInfo chipInfo() {
		return (IChipInfo) site.getService(IChipInfo.class.getName());
	}

	@Override
	public Object option(String key) {
		if (KEY_OPTIONS_PIN.equals(key)) {
			throw new EcmException(String.format("系统键:%s", key));
		}
		return options.get(key);
	}

	@Override
	public IPlug option(String key, Object v) {
		if (KEY_OPTIONS_PIN.equals(key)) {
			throw new EcmException(String.format("系统键:%s", key));
		}
		options.put(key, v);
		return this;
	}

	@Override
	public String[] enumBranch() {
		String[] arr = new String[branches.size()];
		IBranchKey[] keys = enumBranchKey();
		for (int i = 0; i < arr.length; i++) {
			arr[i] = keys[i].key();
		}
		return arr;
	}

	@Override
	public boolean containsBranch(IBranchKey key) {
		// TODO Auto-generated method stub
		return branches.containsKey(key);
	}

	@Override
	public void plugBranch(IBranchKey key, IPin pin) {
		branches.put(key, pin);
	}

	@Override
	public IBranchKey[] enumBranchKey() {
		// TODO Auto-generated method stub
		return branches.keySet().toArray(new IBranchKey[0]);
	}

	@Override
	public int branchCount() {
		return branches.size();
	}

	@Override
	public boolean containsBranch(String name) {
		StringBranchKey key = new StringBranchKey(name);
		return branches.containsKey(key);
	}

	@Override
	public IPin branch(String name) {
		StringBranchKey key = new StringBranchKey(name);

		return branches.get(key);
	}

	@Override
	public IPin branch(IBranchKey key) {
		// TODO Auto-generated method stub
		return branches.get(key);
	}

	@Override
	public IPin branch(String name, IBranchSearcher searcher) {
		return searcher.search(name, branches);
	}

	@Override
	public IPlug plugBranch(String name, IPin pin) {
		StringBranchKey key = new StringBranchKey(name);
		if (branches.containsKey(key)) {
			throw new RuntimeException("已存在分支端子：" + name);
		}
		branches.put(key, pin);
		return this;
	}

	@Override
	public void removeBranch(String name) {
		StringBranchKey key = new StringBranchKey(name);
		branches.remove(key);
	}

	@Override
	public void removeBranch(IBranchKey key) {
		branches.remove(key);
	}

	@Override
	public void flow(Frame frame, Circuit circuit) throws CircuitException {
		if (next != null) {
			try {
				next.plugin.flow(frame, circuit, next);
			} catch (Exception e) {
				Throwable t = e;
				if (e instanceof InvocationTargetException) {
					t = ((InvocationTargetException) e).getTargetException();
				}
				CircuitException ce = CircuitException.search(t);
				if (ce == null) {
					ce = new CircuitException(NetConstans.STATUS_603, t);
				}
				ce.update(getGraphSimpleName(), name(), next.name,
						next.plugin.getClass());
				circuit.status(ce.status);
				circuit.message(ce.getMessage());
				circuit.cause(ce.messageCause());
				// System.out.println("*****" + ce.hashCode());
				// log.error(circuit.status() + " " + circuit.message());
				throw ce;
			}
		}
	}

	private String getGraphSimpleName() {
		String gn = site().getProperty("$.graph.name");
		gn = gn.substring(gn.lastIndexOf(".") + 1, gn.length());
		return gn;
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return name;
	}

	public IPlug socket(String pinName) {
		ISocketTable st = (ISocketTable) site()
				.getService("$.graph.socketTable");
		if (st == null)
			return null;
		return st.getSocket(plugin.hashCode(), owner());
	}

	public String[] enumSocket() {
		ISocketTable st = (ISocketTable) site()
				.getService("$.graph.socketTable");
		if (st == null)
			return new String[0];
		List<String> list = st.getSockets(plugin.hashCode());
		if (list == null)
			return new String[0];
		return list.toArray(new String[0]);
	}

	@Override
	public String toString() {
		return fullName();
	}

	/**
	 */
	@Override
	public void print(StringBuffer sb) {
		print(sb, " ");
	}

	@Override
	public boolean equals(Object obj) {
		Plug plug = (Plug) obj;
		return plug.name().equals(this.name())
				&& plug.owner().equals(this.owner());
	}

	@Override
	public void print(StringBuffer sb, String indent) {
		sb.append(indent + name());

		// for (String pinName : branches.keySet()) {
		// Pin b = branch(pinName);
		// b.print(sb, indent);
		// }
		// if (next != null)
		// next.print(sb, indent);
	}

	class StringBranchKey implements IBranchKey {
		String key;

		public StringBranchKey() {
			// TODO Auto-generated constructor stub
		}

		public StringBranchKey(String key) {
			this.key = key;
		}

		public String key() {
			return key;
		}

		@Override
		public int hashCode() {
			if (StringUtil.isEmpty(key)) {
				return super.hashCode();
			}
			return key.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			IBranchKey k = (IBranchKey) obj;
			return this.key.equals(k.key());
		}
	}
}
