package cj.studio.ecm.context;

import cj.ultimate.gson2.com.google.gson.JsonElement;

public interface IElementParser {
	/**
	 * 返回实体
	 * <pre>
	 *
	 * </pre>
	 * @return
	 */
	Object getEntity();
	void parse(IElement e,JsonElement je);

}
