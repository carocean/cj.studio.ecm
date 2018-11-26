package cj.studio.ecm.examples.chip1;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.annotation.CjServiceSite;

@CjService(name="refService",scope=Scope.runtime)
public class RefService {
	@CjServiceSite
	IServiceProvider site;
	@CjServiceRef
	Object refField;
	@CjServiceRef(refByName="refField")
	Object refField2;
	@CjServiceRef(refByName="$.object")
	Object refObject;
	@CjServiceRef(refByName="test.text")//引用插件服务
	String text;
}
