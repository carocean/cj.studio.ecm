package cj.studio.ecm.net.web.page.context;

import cj.studio.ecm.graph.ISink;

public class JavaPageContext extends PageContext {
	private String oldRelatePath;
	public JavaPageContext(ISink lookup, String httpRoot, String oldRelatePath) {
		super(lookup,  httpRoot);
		this.oldRelatePath=oldRelatePath;
	}
	public String getOldRelatePath() {
		return oldRelatePath;
	}
	boolean jssFound;


	public boolean isJssFound() {
		return jssFound;
	}
	public void setJssFound(boolean jssFound) {
		this.jssFound = jssFound;
	}
}
