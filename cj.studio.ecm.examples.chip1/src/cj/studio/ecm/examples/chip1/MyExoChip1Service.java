package cj.studio.ecm.examples.chip1;

import cj.studio.ecm.annotation.CjService;

@CjService(name="myExoChip1Service",isExoteric=true)
public class MyExoChip1Service implements IExoType{

	@Override
	public void test(String v) {
		System.out.println("-----chip1"+v);
		
	}

}
