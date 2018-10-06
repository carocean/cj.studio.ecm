package cj.studio.ecm.net.layer;

import cj.ultimate.util.StringUtil;

public class AC {
	private String nodeName;
	private String cjtoken;
	public AC(String selectName,String cjtoken) {
		nodeName=selectName;
		this.cjtoken=cjtoken;
	}
	public AC(String selectName){
		nodeName=selectName;
	}
	/**
	 * cjtoken在站点内唯一，而nodename是一个服务器在站点内唯一，因此二者共同决定了在路由的路径上也是唯一的。
	 */
	@Override
	public boolean equals(Object obj) {
		AC o = (AC) obj;
		if (StringUtil.isEmpty(nodeName) || obj == null) {
			return super.equals(obj);
		}
		if(StringUtil.isEmpty(o.cjtoken)){
			return o.nodeName.equals(nodeName)&&StringUtil.isEmpty(cjtoken);
		}
		return o.nodeName.equals(nodeName)&&o.cjtoken.equals(cjtoken);
	}
	public String getCjtoken() {
		return cjtoken;
	}
	public String getNodeName() {
		return nodeName;
	}
}
