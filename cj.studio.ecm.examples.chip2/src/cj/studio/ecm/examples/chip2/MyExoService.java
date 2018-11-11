package cj.studio.ecm.examples.chip2;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.examples.chip1.IExoType;

@CjService(name="myExoService",isExoteric=true)
public class MyExoService implements IExoType{

	@Override
	public void test(String v) {
		System.out.println("-----"+v);
		
	}

}
