package cj.studio.ecm.script;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
/**
 * 表示编译前与编译后的代码。
 * <pre>
 *
 * </pre>
 * @author carocean
 *
 */
public class JssCode {
	private String segmentName;
	private String rawbody;
	private Map<String, String> rawhead;
	private String bodyCode;
	private String headCode;
	public JssCode() {
		rawhead=new HashMap<String, String>(4);
	}
	public String getRawBody() {
		return rawbody;
	}
	public String[] enumRawHead() {
		return rawhead.keySet().toArray(new String[0]);
	}
	public String getRawHead(String key){
		return rawhead.get(key);
	}
	public void addRawHead(String key,String value){
		rawhead.put(key, value);
	}
	public void removeRawHead(String key){
		rawhead.remove(key);
	}
	public int headRawSize(){
		return rawhead.size();
	}
	public String getSegmentName() {
		return segmentName;
	}
	public void setRawBody(String rawbody) {
		this.rawbody = rawbody;
	}
	public void setSegmentName(String segmentName) {
		this.segmentName = segmentName;
	}
	public String getRawHeadCode(){
		Iterator<Entry<String, String>> it=rawhead.entrySet().iterator();
		if(!it.hasNext()){
			return "{}";
		}
		StringBuffer sb=new StringBuffer();
		sb.append("{");
		for(;;){
			Entry<String, String> e = it.next();
			sb.append(String.format("%s:%s",e.getKey(), e.getValue()));
			if(!it.hasNext()){
				sb.append("}");
				break;
			}
			sb.append(",");
		}
		return sb.toString() ;
	}
	public String getBodyCode() {
		return bodyCode;
	}
	public void setBodyCode(String bodyCode) {
		this.bodyCode = bodyCode;
	}
	public String getHeadCode() {
		return headCode;
	}
	public void setHeadCode(String headCode) {
		this.headCode = headCode;
	}
}
