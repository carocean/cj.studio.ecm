package cj.studio.ecm.container.describer;

public class BridgeDescriber extends TypeDescriber {
	private String aspects;
	private boolean isValid;
	public String getAspects() {
		return aspects;
	}
	public void setAspects(String aspects) {
		this.aspects = aspects;
	}
	public boolean isValid() {
		return isValid;
	}
	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}
}
