package cj.studio.ecm.net.jee;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cj.studio.ecm.graph.CircuitException;

public interface IJeeWebView {
	void flow(JeeHttpRequest req, JeeHttpResponse resp,
			IJeeSiteResource resource) throws CircuitException, IOException;
}
