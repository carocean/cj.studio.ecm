package cj.studio.ecm.net.web;

import cj.studio.ecm.graph.CjStatusDef;
import cj.studio.ecm.graph.IConstans;

public interface IWebSiteConstans extends IConstans{
	@CjStatusDef(message = "HTTP/1.1")
	String PROTOCAL = "protocol";
	@CjStatusDef(message="ok")
	String STATUS_200="200";
}
