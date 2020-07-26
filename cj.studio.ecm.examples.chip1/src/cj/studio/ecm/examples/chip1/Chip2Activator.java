package cj.studio.ecm.examples.chip1;

import cj.studio.ecm.IEntryPointActivator;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.context.IElement;

public class Chip2Activator implements IEntryPointActivator {
    @Override
    public void activate(IServiceSite site, IElement args) {
        System.out.println("---Chip2Activator---activate");
    }

    @Override
    public void inactivate(IServiceSite site) {
        System.out.println("---Chip2Activator---inactivate");
    }
}
