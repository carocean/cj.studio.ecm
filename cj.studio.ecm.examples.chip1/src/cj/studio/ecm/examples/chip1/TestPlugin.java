package cj.studio.ecm.examples.chip1;

import cj.studio.ecm.IAssemblyContext;
import cj.studio.ecm.IChipPlugin;
import cj.studio.ecm.context.IElement;

public class TestPlugin implements IChipPlugin {
	
	@Override
	public Object getService(String serviceId) {
		if("text".equals(serviceId)) {
			return "xxxxxx";
		}
		return null;
	}

	@Override
	public void load(IAssemblyContext ctx, IElement args) {
		// TODO Auto-generated method stub
		System.out.println("------TestPlugin_load");
	}

	@Override
	public void unload() {
		// TODO Auto-generated method stub
		System.out.println("------TestPlugin_unload");
	}

}
