package cj.studio.ecm.examples.chip2;

import cj.studio.ecm.IEntryPointActivator;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.context.IElement;

public class Chip2Activator implements IEntryPointActivator{

	@Override
	public void activate(IServiceSite site, IElement args) {
		// TODO Auto-generated method stub
		System.out.println("----这是chip2活动器启动后，参数："+args);
	}

	@Override
	public void inactivate(IServiceSite site) {
		// TODO Auto-generated method stub
		System.out.println("-----这是chip2活动器停止前");
	}

}
