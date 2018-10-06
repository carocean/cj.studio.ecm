package cj.studio.ecm.container.describer;

public class ExotericalTypeDescriber extends TypeDescriber {
	private String exotericalTypeName;
	private boolean isPackage;
	public String getExotericalTypeName() {
		return exotericalTypeName;
	}
	public void setExotericalTypeName(String exotericalTypeName) {
		this.exotericalTypeName = exotericalTypeName;
	}
	public boolean isPackage() {
		return isPackage;
	}
	public void setPackage(boolean isPackage) {
		this.isPackage = isPackage;
	}
}
