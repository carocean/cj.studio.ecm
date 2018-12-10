package cj.studio.ecm.net.rio;

import cj.studio.ecm.graph.IResult;

public interface IMoniterResult extends IResult{
	void message(String msg);
	void state(int state);
	void notifyFinished();
	void waitFinished(long time)throws InterruptedException;
}
