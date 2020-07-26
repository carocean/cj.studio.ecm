package cj.studio.ecm.context;

import cj.ultimate.gson2.com.google.gson.JsonElement;


public interface IElement extends INode {
    INode getNode(String name);

    String[] enumNodeNames();

    void addNode(INode node);

    void parse(Object je, IElementParser parser);

    /**
     * 获取元素解析器，注意：可能元素不存在解析器。
     * <pre>
     *
     * </pre>
     *
     * @return
     */
    IElementParser parser();
}
