package cj.studio.ecm.container.describer;


public class ParametersMehtodDescriber extends MethodDescriber {
	// 二数组相匹配
	private String[] args;
	//对应参数值，只有两种ref,value
	private String[] injectModes;
	public boolean isEmpty(){
		return args==null?true:false;
	}
	public ParametersMehtodDescriber() {
		args=new String[0];
		injectModes=new String[0];
	}
	public int getLength(){
		return args.length;
	}
	public String getArgDesc(int index) {
		return args[index];
	}
	public String getInjectMode(int index) {
		return injectModes[index];
	}
	public void put(String[] args,String[] injectMode){
		if(args.length!=injectMode.length)
			throw new RuntimeException("一个参数必须对应一个注入方式");
		if(this.args==null&&this.injectModes==null){
			this.args=args;
			this.injectModes=injectMode;
		}else{
			int len=this.args.length+args.length;
			String[] newArgs=new String[len];
			String[] newInj=new String[len];
			System.arraycopy(this.args, 0, newArgs, 0,this.args.length);
			System.arraycopy(this.injectModes, 0, newInj, 0,this.injectModes.length);
			System.arraycopy(args, 0, newArgs, this.args.length,args.length);
			System.arraycopy(injectMode, 0, newInj, this.injectModes.length,injectMode.length);
			this.args=newArgs;
			this.injectModes=newInj;
		}
		
	}
}
