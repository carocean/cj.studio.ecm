package cj.studio.ecm.net.web;

import java.util.HashMap;
import java.util.Map;

import cj.studio.ecm.IChipInfo;
import cj.ultimate.util.StringUtil;

/**
 * web配置项，assembly.properties
 * 
 * <pre>
 *
 * </pre>
 * 
 * @author carocean
 *
 */
public class SiteConfig {
	public static final int DEFAULT_EXPIRE_TIME=1800;//1800秒

	public final static String WEBCONFIG_KEY = "siteconfig";
	private String siteIcon;
	private String siteWelcome;
	private Map<String, String> errors;// key=errorCode,value=error pages
										// address
	private Map<String, Long> sessionTimeout;
	private String httpdocument;
	private Map<String, String> mime;// key=extName,value=mime type
										// application/x-shockwave-flash
//	private String wsInDirDefaultDoc;
	private String wsWelcome;
	private String responseFrameType;

	public void parse(IChipInfo info) {
		errors = new HashMap<String, String>(4);
		mime = new HashMap<String, String>(4);
		sessionTimeout = new HashMap<String, Long>(4);
		String[] props = info.enumProperty();
		for (String key : props) {
			String v = info.getProperty(key);
			switch (key) {
			case "site.icon":
				siteIcon = v;
				break;
			case "site.welcome":
				siteWelcome = v;
				break;
			case "site.http.document":
				httpdocument = v;
				break;
			case "site.ws.welcome":
				wsWelcome = StringUtil.isEmpty(v) ? "" : v;
				break;
//			case "site.ws.inDirDefaultDoc":
//				wsInDirDefaultDoc = StringUtil.isEmpty(v) ? "index.wt" : v;
//				break;
			case "site.ws.responseFrameType":
				responseFrameType = StringUtil.isEmpty(v) ? "text/raw" : v;
				break;
			default:
				if ("site.http.error".equals(key)) {
					errors.put("default", v);
					break;
				}
				if (key.startsWith("site.http.error")) {
					String name = key.replace("site.http.error.", "");
					errors.put(name, v);
					break;
				}
				if (key.startsWith("site.http.mime")) {
					String name = key.replace("site.http.mime.", "");
					mime.put(name, v);
					break;
				}
				if (key.startsWith("site.session") && key.endsWith(".timeout")) {
					String simple = key.replace("site.session.", "").replace(
							".timeout", "");
					Long timeout = StringUtil.isEmpty(v) ? 1800 : Long
							.valueOf(v);
					sessionTimeout.put(simple, timeout);
					break;
				}
				break;
			}
		}
	}

//	public String getWsInDirDefaultDoc() {
//		return wsInDirDefaultDoc;
//	}

	public String getWsWelcome() {
		return wsWelcome;
	}


	public String getHttpdocument() {
		return httpdocument;
	}

	public String getSiteIcon() {
		return siteIcon;
	}

	public String getSiteWelcome() {
		return siteWelcome;
	}

	public String[] enumError() {
		return errors.keySet().toArray(new String[0]);
	}

	public String error(String code) {
		return errors.get(code);
	}

	public boolean containsError(String code) {
		return errors.containsKey(code);
	}

	public boolean containsMime(String extName) {
		return mime.containsKey(extName);
	}

	public long getSessionTimeout(String simple){
		Long i=sessionTimeout.get(simple);
		return i==null?DEFAULT_EXPIRE_TIME:i;
	}

	public String[] enumMime() {
		return mime.keySet().toArray(new String[0]);
	}

	public String mime(String extName) {
		return mime.get(extName);
	}
	
	/**
	 * 如:image/png
	 * <pre>
	 *
	 * </pre>
	 * @param mineType
	 * @return
	 */
	public boolean containsMimeType(String mineType){
		return mime.containsValue(mineType);
	}
	public String responseFrameType() {
		return responseFrameType;
	}

}
