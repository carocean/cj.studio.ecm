package cj.studio.ecm.net.web.page.context;

import org.jsoup.nodes.Document;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.graph.CircuitException;

public interface IHttpContext {
	public Document html(String relativedUrl) throws CircuitException;
	public void redirect(String relativedUrl, Circuit circuit);
	public Document html(String relativedUrl, String charset)
			throws CircuitException ;
	public Document html(String relativePath, String resourceRefix,
			String charset)throws CircuitException ;
	public String resourceText(String relativedUrl) throws CircuitException;
	public String getRealHttpSiteRootPath(Circuit circuit);
	public byte[] resource(String relativedUrl) throws CircuitException;
}
