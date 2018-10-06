package cj.studio.ecm.domain;

import cj.studio.ecm.resource.IRuntimeBoundary;
import cj.studio.ecm.util.ObjectmMedium;

//应将驱动放到芯片域中加载
//芯片的属性索引，芯片的日志\及芯片信息
public final class ChipDomain extends AbstractDomain implements IDomain{
	public final static String CHIP_INFO_KEY="CHIP_INFO";
	public ChipDomain(ObjectmMedium medium) {
		super(medium);
		// TODO Auto-generated constructor stub
	}

	public static ChipDomain current(){
		IRuntimeBoundary rb=(IRuntimeBoundary) ChipDomain.class.getClassLoader();
		return (ChipDomain)rb.getDomain();
	}
	public String getChipId(){
		return (String)medium.get("guid");
	}
	public String getChipName(){
		return (String)medium.get("assemblyTitle");
	}
	public String getChipResource(){
		return (String)medium.get("assemblyResource");
	}
	public String getChipVersion(){
		return (String)medium.get("assemblyVersion");
	}
	public String getChipDescription(){
		return (String)medium.get("assemblyDescription");
	}
	
	
}
