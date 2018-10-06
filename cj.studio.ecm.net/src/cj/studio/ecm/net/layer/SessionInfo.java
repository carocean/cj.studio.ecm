package cj.studio.ecm.net.layer;

public class SessionInfo {
	String netSourceSimple;
	String netSourceName;
	String netRemoteAddress;
	String netLocalAddress;
	public SessionInfo() {
		// TODO Auto-generated constructor stub
	}
	public SessionInfo(String netSourceSimple, String netSourceName,
			String netRemoteAddress, String netLocalAddress) {
		this.netSourceSimple = netSourceSimple;
		this.netSourceName = netSourceName;
		this.netRemoteAddress = netRemoteAddress;
		this.netLocalAddress = netLocalAddress;
	}
	public String getNetSourceSimple() {
		return netSourceSimple;
	}
	public void setNetSourceSimple(String netSourceSimple) {
		this.netSourceSimple = netSourceSimple;
	}
	public String getNetSourceName() {
		return netSourceName;
	}
	public void setNetSourceName(String netSourceName) {
		this.netSourceName = netSourceName;
	}
	public String getNetRemoteAddress() {
		return netRemoteAddress;
	}
	public void setNetRemoteAddress(String netRemoteAddress) {
		this.netRemoteAddress = netRemoteAddress;
	}
	public String getNetLocalAddress() {
		return netLocalAddress;
	}
	public void setNetLocalAddress(String netLocalAddress) {
		this.netLocalAddress = netLocalAddress;
	}
	
}
