package cj.studio.ecm.net.jee;

import java.io.IOException;

import cj.studio.ecm.net.web.page.context.IHttpContext;

public interface IJeeSiteResource extends IHttpContext {
	void redirect(String relativedUrl,JeeHttpResponse resp) throws IOException;
}
