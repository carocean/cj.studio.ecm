package cj.studio.ecm.context;


public class Property extends Node implements IProperty  {
	public Property() {
		// TODO Auto-generated constructor stub
	}
	public Property(String name,INode value) {
		super(name);
		this.value=value;
	}
	public Property(String name,String value) {
		super(name);
		this.value=new Node(value);
	}
	private INode value;
	@Override
	public void setValue(INode value) {
		this.value=value;
	}
	@Override
	public INode getValue() {
		return value;
	}
	public String toString(int level) {
		StringBuffer sb=new StringBuffer();
//		sb.append(getIndent(level));
		sb.append(getName());
		sb.append("=");
		sb.append("\"");
		sb.append(value);
		sb.append("\"");
		return sb.toString();
	}
}
