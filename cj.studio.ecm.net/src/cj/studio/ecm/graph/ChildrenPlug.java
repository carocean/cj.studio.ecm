package cj.studio.ecm.graph;

import java.util.Map;

import cj.studio.ecm.IServiceSite;

public class ChildrenPlug extends Plug {
	public ChildrenPlug(String sinkName, Map<String, Object> options,
			IServiceSite site, ISink sink) {
		super(sinkName, options, site, sink);
		// TODO Auto-generated constructor stub
	}
	IPlug parent;
	
	public void setParent(IPlug parent) {
		this.parent = parent;
	}
	@Override
	public IPin branch(IBranchKey key) {
		// TODO Auto-generated method stub
		IPin pin= super.branch(key);
		if(pin!=null){
			return pin;
		}
		return parent.branch(key);
	}
	@Override
	public IAtom atom(String name) {
		IAtom atom= super.atom(name);
		if(atom!=null){
			return atom;
		}
		return parent.atom(name);
	}
	@Override
	public IBranchKey[] enumBranchKey() {
		IBranchKey[] arr1=null;
		if(parent==null){
			arr1=new IBranchKey[0];
		}else{
			arr1= parent.enumBranchKey();
		}
		if(this.branches.isEmpty())return arr1;
		IBranchKey[] ret=new IBranchKey[arr1.length+branches.size()];
		System.arraycopy(arr1, 0, ret, 0, arr1.length);
		IBranchKey[] arr2=branches.keySet().toArray(new IBranchKey[0]);
		System.arraycopy(arr2, 0, ret, arr1.length, arr2.length);
		return ret;
	}
	@Override
	public IPin branch(String name) {
		// TODO Auto-generated method stub
		IPin pin= super.branch(name);
		if(pin!=null)return pin;
		return parent.branch(name);
	}
	@Override
	public IPin branch(String name, IBranchSearcher searcher) {
		// TODO Auto-generated method stub
		IPin pin= super.branch(name, searcher);
		if(pin!=null)return pin;
		return parent.branch(name,searcher);
	}
	@Override
	public int branchCount() {
		// TODO Auto-generated method stub
		return super.branchCount();
	}
	@Override
	public void dispose() {
		super.dispose();
		this.parent=null;
	}
	@Override
	public Object option(String key) {
		// TODO Auto-generated method stub
		Object ret= super.option(key);
		if(ret!=null)return ret;
		return parent.option(key);
	}
	@Override
	public Object optionsGraph(String key) {
		// TODO Auto-generated method stub
		Object ret= super.optionsGraph(key);
		if(ret!=null)return ret;
		return parent.optionsGraph(key);
	}
}
