package cj.studio.ecm;

import cj.studio.ecm.context.IModuleContext;

public abstract class ChipInfo implements IChipInfo {

	public ChipInfo(IModuleContext ctx) {
		this.built(ctx);
	}

	protected abstract void built(IModuleContext ctx) ;
	

}
