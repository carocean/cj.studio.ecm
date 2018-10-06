package cj.studio.ecm.net.web.page.context;

import cj.studio.ecm.graph.ISink;

public class JssPageContext extends PageContext {
	public JssPageContext(ISink lookup,  String httpRoot) {
		super(lookup,  httpRoot);
		// TODO Auto-generated constructor stub
	}
	boolean jssFound;


	public boolean isJssFound() {
		return jssFound;
	}
	public void setJssFound(boolean jssFound) {
		this.jssFound = jssFound;
	}
}
