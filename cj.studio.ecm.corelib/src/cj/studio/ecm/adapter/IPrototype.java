package cj.studio.ecm.adapter;

public interface IPrototype {
	String getServiceDefinitionId();
	Object unWrapper() ;
	String getAspects();
	boolean isBridge();
}
