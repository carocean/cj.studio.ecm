package cj.studio.ecm.net.web.widget.context;

public class JssWidgetContext extends WidgetContext {
	boolean jssFound;


	public boolean isJssFound() {
		return jssFound;
	}
	public void setJssFound(boolean jssFound) {
		this.jssFound = jssFound;
	}
	public JssWidgetContext(String httpRoot) {
		super(httpRoot);
		// TODO Auto-generated constructor stub
	}

}
