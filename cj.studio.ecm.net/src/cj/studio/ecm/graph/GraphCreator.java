package cj.studio.ecm.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceSite;
import cj.ultimate.IDisposable;

public abstract class GraphCreator implements IGraphCreator, IDisposable {
	Map<String, IPin> in;
	Map<String, IPin> out;
	Map<String, IPin> inner;
	// Map<String, Pin> help;
	IServiceSite site;
	ISocketTable socketTable;
	// 此选项在整个graph中可见
	Map<String, Object> options;
	IProtocolFactory factory;
	Map<String, IComponent> components;

	public GraphCreator() {
		in = new HashMap<String, IPin>();
		out = new HashMap<String, IPin>();
		inner = new HashMap<String, IPin>();
		// help = new HashMap<String, Pin>();
		socketTable = new SocketTable();
		components = new HashMap<String, IComponent>();
	}

	/**
	 * 中间端子。
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param pinName
	 * @return
	 */
	public IPin middle(String pinName) {
		return inner.get(pinName);
	}

	public IPin in(String pinName) {
		return in.get(pinName);
	}

	public IPin out(String pinName) {
		return out.get(pinName);
	}

	protected Object option(String key) {
		return options.get(key);
	}

	protected void option(String key, Object v) {
		options.put(key, v);
	}

	protected void init(Map<String, Object> options) {
		this.options = options;
		// options.put("$graph.creator", this);
		options.put("$log.level", "error");
	}

	@Override
	public IServiceSite site() {
		return site;
	}

	protected Map<String, IPin> in() {
		return in;
	}

	protected Map<String, IPin> out() {
		return out;
	}

	protected Map<String, IPin> component() {
		return out;
	}

	public final IProtocolFactory protocolFactory() {
		return factory;
	}

	@Override
	public void dispose() {
		site = null;
		for (IPin p : in.values()) {
			p.dispose();
		}
		in.clear();
		for (IPin p : out.values()) {
			p.dispose();
		}
		out.clear();
		for (IPin p : inner.values()) {
			p.dispose();
		}
		inner.clear();
		// help.clear();
		components.clear();
		socketTable.dispose();
	}

	public boolean containsInputPin(String pinName) {
		if (in == null)
			return false;
		return in.containsKey(pinName);
	}

	public boolean containsOutputPin(String pinName) {
		if (out == null)
			return false;
		return out.containsKey(pinName);
	}

	@Override
	public final IComponent newComponent(String compName) {
		IComponent comp = createComponent(compName);
		components.put(compName, comp);
		return comp;
	}

	protected IComponent createComponent(String compName) {
		return null;
	}

	@Override
	public final ICablePin newCablePin(String pinName, Access access) {
		CablePin c = (CablePin) createPin(pinName, PinType.cuble);
		if (c == null)
			c = new CablePin(pinName);
		return (ICablePin) newPin(pinName, access, c);
	}

	@Override
	public final ICablePin newCablePin(String pinName) {
		return (ICablePin) newCablePin(pinName, Access.middle);
	}

	public final IWirePin newWirePin(String pinName) {
		return (IWirePin) newWirePin(pinName, Access.middle);
	}

	@Override
	public final IWirePin newWirePin(String pinName, Access access) {
		WirePin w = (WirePin) createPin(pinName, PinType.wire);
		if (w == null)
			w = new WirePin();

		return (IWirePin) newPin(pinName, access, w);
	}

	/**
	 * 创建端子。
	 * 
	 * <pre>
	 * 如果返回空，系统将分配默认端子。
	 * </pre>
	 * 
	 * @param pinName
	 * @param type
	 * @return
	 */
	protected IPin createPin(String pinName, PinType type) {
		return null;
	}

	private IPin newPin(String pinName, Access access, Pin p) {
		if (p == null) {
			if (site != null) {
				p = (Pin) site.getService(pinName);
			}
		}
		if (p == null) {
			throw new RuntimeException("指定的pin不存在。" + pinName);
		}
		p.options.put("$.pin.access", access);
		// 设置pin的选项
		p.options.put("$pinName", pinName);
		// p.options.put("$optionsGraph", options);
		p.site(site);

		if (access != null) {
			switch (access) {
			case input:
				if (in.containsKey(pinName)) {
					throw new RuntimeException("已存在输入端子：" + pinName);
				}
				in.put(pinName, p);
				break;
			case output:
				if (out.containsKey(pinName))
					throw new RuntimeException("已存在输出端子：" + pinName);
				out.put(pinName, p);
				break;
			default:
				inner.put(pinName, p);
				break;
			}
		} else {
			inner.put(pinName, p);// 内部端子
		}

		return p;
	}

	@Override
	public final ISink newSink(String sink) {
		ISink inst = createSink(sink);
		if (inst == null && site != null) {
			inst = (ISink) site.getService(sink);
		}
		return inst;
	}

	/**
	 * 根据sink服务名新建sink
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param sink
	 * @return
	 */
	protected abstract ISink createSink(String sink);

	protected void onBuildBefore() {
		// TODO Auto-generated method stub

	}

	protected void onBuildAfter() {
		// TODO Auto-generated method stub

	}

	/**
	 * 协议工厂，如果有使用可在派生类中指定.
	 * 
	 * <pre>
	 * 该方法在graphcreator构造时执行
	 * IProtocolFactory factory=AnnotationProtocolFactory.factory(constans interface);
	 * </pre>
	 * 
	 * @return
	 */
	protected IProtocolFactory newProtocol() {

		return null;
	}

	public final IAtom newAtom(String name) {
		return createAtom(name);
	}

	/**
	 * 可为流中的插头提供原子服务
	 * 
	 * <pre>
	 * 注意：原子只能是多例模式，每次申请均产生新实例。
	 * 原因是：它必须持有一个插头。
	 * </pre>
	 * 
	 * @param name
	 * @return
	 */
	protected IAtom createAtom(String name) {
		return null;
	}

	/**
	 * 新建导线，并插入到指定的插头作为分支。
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param pinName
	 * @param sitePlug
	 * @return
	 */
	public IPin newWirePin(String pinName, IPlug plug) {
		IPin p = newWirePin(pinName);
		plug.plugBranch(pinName, p);
		return p;
	}

	class SocketTable implements ISocketTable {
		Map<Integer, List<String>> map;// key(sink hashcode),list key(pinName)

		public SocketTable() {
			map = new ConcurrentHashMap<Integer, List<String>>();
		}

		public void put(Plug plug, String onPin) {
			int sinkHashCode=plug.plugin.hashCode();
			List<String> list = map.get(sinkHashCode);
			if (list == null) {
				list = new ArrayList<String>();
				map.put(sinkHashCode, list);
			}
			if (list.contains(onPin)) {
				throw new EcmException(
						String.format(
								"同一个名称的pin，不充许多次插入到同一名称的sink的同一实例. plug name is [%s] hashCode is [%s] 冲突pin[%s] ",
								plug.name(),sinkHashCode, onPin));
			}
			list.add(onPin);
		}
		
		public List<String> getSockets(int sinkHashCode) {
			return map.get(sinkHashCode);
		}
		public IPlug getSocket(int sinkHashCode,String pinName){
			List<String> list=map.get(sinkHashCode);
			if(list==null)return null;
			if(!list.contains(pinName))return null;
			
			return findPlug(pinName,sinkHashCode,in);
		}
		private IPlug findPlug(String pinName, int sinkHashCode,
				Map<String, IPin> map) {
			Integer [] keys=map.keySet().toArray(new Integer[0]);
			for(Integer key:keys){
				Pin p=(Pin)map.get(key);
				Plug cur=p.first;
				while(cur!=null){
					if(cur.plugin!=null&&cur.plugin.hashCode()==sinkHashCode){
						return cur;
					}
					cur=cur.next;
				}
			}
			return null;
		}

		public void remove(int sinkHashCode, String onPin) {
			List<String> list = map.get(sinkHashCode);
			if (list == null) {
				return;
			}
			list.remove(onPin);
			if(list.isEmpty()){
				map.remove(sinkHashCode);
			}
		}
		public void remove(String onPin) {
			Integer keys[] =map.keySet().toArray(new Integer[0]);
			for(Integer key:keys){
				List<String> list=map.get(key);
				if(list==null)continue;
				list.remove(onPin);
				if(list.isEmpty())
					map.remove(key);
			}
		}
		public void remove(int sinkHashCod) {
			map.remove(sinkHashCod);
		}

		public boolean contains(int sinkHashCod) {
			return map.containsKey(sinkHashCod);
		}

		@Override
		public void dispose() {
			map.clear();
		}
	}
}