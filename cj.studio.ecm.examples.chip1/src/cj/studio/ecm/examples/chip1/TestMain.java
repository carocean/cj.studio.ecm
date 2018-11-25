package cj.studio.ecm.examples.chip1;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.examples.chip1.aspect.IRefAspect;

@CjService(name="main",isExoteric=true)
public class TestMain {
	@CjServiceRef(refByName="refAspect")
	IRefAspect refAspect;
	public void test(String args) {
		refAspect.test(args);
		System.out.println(args);
	}
}
