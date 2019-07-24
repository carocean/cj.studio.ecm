package cj.studio.ecm.examples.chip1;

import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.context.IServiceContainerMonitor;

public class MyServiceContainerMonitor implements IServiceContainerMonitor{

	@Override
	public void onBeforeRefresh(IServiceSite site) {
		System.out.println("------------onBeforeRefresh+"+site);
	}

	@Override
	public void onAfterRefresh(IServiceSite site) {
		System.out.println("------------onAfterRefresh+"+site);
	}

}
