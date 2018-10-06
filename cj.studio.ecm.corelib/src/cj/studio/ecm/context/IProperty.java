package cj.studio.ecm.context;


public interface IProperty extends INode {
	INode getValue();
	void setValue(INode value);
}
