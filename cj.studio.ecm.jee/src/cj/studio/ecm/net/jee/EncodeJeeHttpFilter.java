package cj.studio.ecm.net.jee;

import java.io.IOException;

import javax.servlet.ServletException;

import cj.studio.ecm.graph.CircuitException;

public class EncodeJeeHttpFilter implements IJeeHttpFilter {


	@Override
	public void flow(JeeHttpRequest req, JeeHttpResponse resp,
			IJeePipeline pipeline) throws CircuitException,ServletException, IOException {
		 resp.setContentType("text/html;charset=UTF-8");
		 pipeline.flow(req, resp);
	}

}
