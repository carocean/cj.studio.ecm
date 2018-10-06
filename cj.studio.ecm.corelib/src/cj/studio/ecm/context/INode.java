package cj.studio.ecm.context;


public interface INode {
	/**
	 * 节点名，如果代表数组就返回下标
	 * @return
	 */
	 String getName();
	 /**
	  * 节点名，如果代表数组就设置为下标
	  * @param name
	  */
	 void setName(String name);
	 String toString(int level);
}
