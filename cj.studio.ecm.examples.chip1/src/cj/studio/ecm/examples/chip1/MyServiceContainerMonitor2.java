package cj.studio.ecm.examples.chip1;

import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.context.IServiceContainerMonitor;

public class MyServiceContainerMonitor2 implements IServiceContainerMonitor{

	@Override
	public void onBeforeRefresh(IServiceSite site) {
		System.out.println("------------onBeforeRefresh2+"+site);
	}

	@Override
	public void onAfterRefresh(IServiceSite site) {
		System.out.println("------------onAfterRefresh2+"+site);
	}

}
