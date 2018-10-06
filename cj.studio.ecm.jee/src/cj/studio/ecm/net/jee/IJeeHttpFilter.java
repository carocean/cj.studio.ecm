package cj.studio.ecm.net.jee;

import java.io.IOException;

import javax.servlet.ServletException;

import cj.studio.ecm.graph.CircuitException;

public interface IJeeHttpFilter {
	void flow(JeeHttpRequest req, JeeHttpResponse resp,
			IJeePipeline pipeline) throws CircuitException,ServletException, IOException;
}
