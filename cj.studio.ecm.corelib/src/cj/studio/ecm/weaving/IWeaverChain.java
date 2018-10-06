package cj.studio.ecm.weaving;


public interface IWeaverChain {
	byte[] weave(String className, byte[] b);
	boolean hasWeavingType(String sClassName);
	void reset();
}
