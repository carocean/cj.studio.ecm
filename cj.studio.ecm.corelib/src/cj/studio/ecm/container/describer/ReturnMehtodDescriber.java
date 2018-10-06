package cj.studio.ecm.container.describer;

public class ReturnMehtodDescriber extends MethodDescriber {
	private String byDefinitionId;
	private String byDefinitionType;
	public String getByDefinitionId() {
		return byDefinitionId;
	}
	public String getByDefinitionType() {
		return byDefinitionType;
	}
	public void setByDefinitionId(String byDefinitionId) {
		this.byDefinitionId = byDefinitionId;
	}
	public void setByDefinitionType(String byDefinitionType) {
		this.byDefinitionType = byDefinitionType;
	}
}
