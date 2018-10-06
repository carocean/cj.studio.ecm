package cj.studio.ecm.net.jee;

import java.io.IOException;

import javax.servlet.ServletException;

import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.net.jee.JeeProgram.JeeHttpFilterWrapper;
import cj.ultimate.IDisposable;

public interface IJeePipeline extends IDisposable{
	void flow(JeeHttpRequest req, JeeHttpResponse resp) throws CircuitException,ServletException, IOException;

	void add(JeeHttpFilterWrapper wrapper);

	void sort();

	IJeePipeline createPipeline();
}
