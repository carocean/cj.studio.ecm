package cj.studio.ecm.examples.chip1;

import cj.studio.ecm.adapter.AdapterFactory;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.examples.chip1.aspect.IRefAspect;
import cj.studio.ecm.examples.test.IRest;

@CjService(name="main",isExoteric=true)
public class TestMain {
	@CjServiceRef(refByName="refAspect")
	IRefAspect refAspect;
	@CjServiceRef(refByName="refService")
	RefService refService;
	@CjServiceRef(refByName="$.rest")
	IRest rest;
	public void test(String args) {
		refAspect.test(args);
		AdapterFactory.createAdaptable(new Object());
		System.out.println(args);
		
	}
}
