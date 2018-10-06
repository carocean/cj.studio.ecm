package cj.studio.ecm.container.describer;


public class ServiceInvertInjectionDescriber extends PropertyDescriber {
	private boolean isForce;
	public boolean isForce() {
		return isForce;
	}
	public void setForce(boolean isForce) {
		this.isForce = isForce;
	}
}
