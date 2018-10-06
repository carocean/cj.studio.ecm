package cj.studio.ecm.container.describer;

import cj.studio.ecm.bridge.UseBridgeMode;


public class ServiceRefDescriber extends PropertyDescriber {
	private String refByType;
	private String refByName;
	private String refByMethod;
	private UseBridgeMode useBridge=UseBridgeMode.auto;
	private BridgeJoinpoint bridgeJoinpoint;
	
	public BridgeJoinpoint getBridgeJoinpoint() {
		return bridgeJoinpoint;
	}
	public void setBridgeJoinpoint(BridgeJoinpoint bridgeJoinpoint) {
		this.bridgeJoinpoint = bridgeJoinpoint;
	}
	public UseBridgeMode getUseBridge() {
		return useBridge;
	}
	public void setUseBridge(UseBridgeMode useBridge) {
		this.useBridge = useBridge;
	}
	public String getRefByType() {
		return refByType;
	}
	public void setRefByType(String refByType) {
		this.refByType = refByType;
	}
	public String getRefByName() {
		return refByName;
	}
	public void setRefByName(String refByName) {
		this.refByName = refByName;
	}
	public String getRefByMethod() {
		return refByMethod;
	}
	public void setRefByMethod(String refByMethod) {
		this.refByMethod = refByMethod;
	}
}
