package cj.studio.ecm.weaving;

import java.util.List;

import cj.studio.ecm.IServiceTypeWeaver;

//类型编织器链，因为有织入顺序，它组织和管理一组编织器链，如：工厂方法编织器、适配器编织器，服务代理编织器等
public class ServiceTypeWeaverChain implements IWeaverChain {
	private List<IServiceTypeWeaver> weavers;
	private int index;
	private IServiceTypeWeaver current;

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		index=0;
		current=null;
	}
	public ServiceTypeWeaverChain(List<IServiceTypeWeaver> weavers) {
		this.weavers = weavers;
	}

	@Override
	public byte[] weave(String className, byte[] b) {
		if (current == null)
			return null;
		return current.weave(className, b, this);
	}

	@Override
	public boolean hasWeavingType(String sClassName) {
		boolean ret=false;
		for (int i = index; i < weavers.size(); i++) {
			IServiceTypeWeaver w = weavers.get(i);
			index++;
			if (w.hasWeavingType(sClassName)) {
				current = w;
				ret= true;
				break;
			}
		}
		return ret;
	}

}
