package cj.studio.ecm.examples.chip1.aspect;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.bridge.IAspect;
import cj.studio.ecm.bridge.ICutpoint;
@CjService(name="myAspect")
public class MyAspect implements IAspect{

	@Override
	public Object cut(Object bridge, Object[] args, ICutpoint point) {
		System.out.println("aspect");
		return null;
	}

	@Override
	public Class<?>[] getCutInterfaces() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void observe(Object service) {
		// TODO Auto-generated method stub
		
	}

}
