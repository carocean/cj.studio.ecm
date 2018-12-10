package cj.studio.ecm.net.rio;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;

import org.apache.log4j.Logger;
 class HandlerThreadFactory implements ThreadFactory {
	 Logger log=Logger.getLogger(getClass());
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setUncaughtExceptionHandler(new NetEngineUnchecckedExceptionhandler());
        return t;
    }
    class NetEngineUnchecckedExceptionhandler implements UncaughtExceptionHandler{

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			log.error(e);
		}
    	
    }
}