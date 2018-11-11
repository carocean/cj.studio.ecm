package cj.studio.ecm.examples.chip1;

import cj.studio.ecm.IChip;
import cj.studio.ecm.IExotericServiceFinder;
import cj.studio.ecm.IServiceAfter;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.Scope;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.annotation.CjExotericalType;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.annotation.CjServiceSite;

@CjExotericalType(typeName = "aa")
@CjService(name = "singleOn", scope = Scope.multiton, isExoteric = true)
public class SingleOnService implements IServiceAfter {
	@CjServiceRef
	RefService refService;
	@CjServiceRef(refByName = "refService")
	RefService refService2;
	@CjServiceSite
	IServiceSite site;
	@CjServiceRef
	Object jsonObject;
	@CjServiceRef(refByName="openServersCommand")
	Object servers;
	@Override
	public void onAfter(IServiceSite site) {
		site.addService("$.aa", new Object());
		System.out.println("-----" + refService);
		Object obj = site.getService("$.aa");
		System.out.println(site.getProperty("site.http.mime.mp2"));
		IChip chip = (IChip) site.getService(IChip.class.getName());
		System.out.println(chip.info().getResourceProp("http.root"));
	}

	public void test(String v) {
		IExotericServiceFinder finder = (IExotericServiceFinder) site.getService("$.exoteric.service.finder");
		ServiceCollection<IExoType> col=finder.getExotericServices(IExoType.class);
		for(IExoType exo:col) {
			System.out.println("----这是在chip1中搜到的chip2中的外部服务:"+exo);
			exo.test(v);
		}
	}

}
