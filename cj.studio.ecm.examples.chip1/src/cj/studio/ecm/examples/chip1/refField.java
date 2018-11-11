package cj.studio.ecm.examples.chip1;

import cj.studio.ecm.IServiceAfter;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceSite;

@CjService(name="refField",scope=Scope.multiton,isExoteric=true)
public class refField implements IServiceAfter{
	@CjServiceSite
	IServiceSite site;

	@Override
	public void onAfter(IServiceSite site) {
		// TODO Auto-generated method stub
		System.out.println(site);
	}
}
