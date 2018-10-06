package cj.studio.ecm.context;

public class Node implements INode {
	private String name;

	public Node() {
		// TODO Auto-generated constructor stub
	}

	public Node(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public final String toString() {
		return toString(0);
	}
	public String toString(int level) {
		StringBuffer sb=new StringBuffer();
//		sb.append(getIndent(level));
		sb.append(name);
		return sb.toString();
	}
	protected StringBuffer getIndent(int level) {
		StringBuffer indent = new StringBuffer();
		for (int i = 0; i < level; i++) {
			indent.append("  ");
		}
		return indent;
	}
}
