package cj.studio.ecm.domain;

import cj.studio.ecm.resource.IRuntimeBoundary;
import cj.studio.ecm.util.ObjectmMedium;


public final class DriverDomain extends AbstractDomain implements IDomain {
	
	public DriverDomain(ObjectmMedium medium) {
		super(medium);
		// TODO Auto-generated constructor stub
	}

	public static DriverDomain current(){
		IRuntimeBoundary rb=(IRuntimeBoundary) DriverDomain.class.getClassLoader();
		return (DriverDomain)rb.getDomain();
	}
}
