package cj.studio.ecm.examples.chip1.aspect;

import cj.studio.ecm.annotation.CjBridge;
import cj.studio.ecm.annotation.CjService;
@CjBridge(aspects="myAspect")
@CjService(name="refAspect")
public class RefAspect implements IRefAspect {
	/* (non-Javadoc)
	 * @see cj.studio.ecm.examples.chip1.aspect.IRefAspect#test(java.lang.String)
	 */
	@Override
	public void test(String args) {
		
	}
}
