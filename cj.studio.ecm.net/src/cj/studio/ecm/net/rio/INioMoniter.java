package cj.studio.ecm.net.rio;

import java.nio.channels.Selector;
import java.util.concurrent.Callable;

public interface INioMoniter extends Callable<Object> {

	@Override
	public Object call() throws Exception;


	Selector getSelector();


	public boolean isRunning();
	public void stop();
}
