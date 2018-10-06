package cj.studio.ecm.context;

import java.util.ArrayList;
import java.util.List;

import cj.ultimate.gson2.com.google.gson.JsonElement;

public class Element extends Node implements IElement {
	private List<INode> properties;
	private IElementParser parser;
	public Element() {
		init();
	}

	public Element(String name) {
		super(name);
		init();
	}
	@Override
	public IElementParser parser() {
		return parser;
	}
	private void init() {
		this.properties = new ArrayList<INode>();
	}
	public void parse(JsonElement je, IElementParser parser) {
		parser.parse(this,je);
		this.parser=parser;
	}
	@Override
	public INode getNode(String name) {
		for (INode n : this.properties) {
			if (n.getName().equals(name))
				return n;
		}
		return null;
	}

	@Override
	public String[] enumNodeNames() {
		String[] arr = new String[this.properties.size()];
		for (int i = 0; i < this.properties.size(); i++) {
			INode n = this.properties.get(i);
			arr[i] = n.getName();
		}
		return arr;
	}

	@Override
	public void addNode(INode node) {
		if (this.properties.contains(node))
			throw new RuntimeException("已存在此属性");
		this.properties.add(node);
	}

	public String toString(int level) {
		StringBuffer sb = new StringBuffer();
		sb.append(getName());
		sb.append("\r\n");
		for (INode n : properties) {
			int j = level + 1;
			sb.append(getIndent(j) );
			sb.append(n.toString(j));
			if(!(n instanceof Element))
			sb.append("\r\n");
		}
		return  sb.toString();
	}

}
