package cj.studio.ecm.net.rio;

import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.ICablePin;

public interface IWorker extends Callable<Object> {

	void init(Frame frame, Circuit circuit, SocketChannel ch,
			ICablePin netInput, ICablePin netOutput, String name,String simple);

}
