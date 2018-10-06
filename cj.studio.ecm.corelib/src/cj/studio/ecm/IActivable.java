package cj.studio.ecm;

import cj.studio.ecm.context.IElement;

public interface IActivable {
	public void activate(IServiceSite site,IElement args);
	void inactivate(IServiceSite site);
}
