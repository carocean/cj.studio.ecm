package cj.studio.ecm.net.rio;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class ConnnectMoniterResult implements IMoniterResult {

	private int state;
	private String message;
	private Object value;
	private CountDownLatch mutex;

	public ConnnectMoniterResult() {
		state = -1;// 表示要等待客户端连接完成
		message = "wait connect timeout.";
		mutex = new CountDownLatch(1);
	}

	@Override
	public void notifyFinished() {
		// TODO Auto-generated method stub
		mutex.countDown();
	}

	@Override
	public void waitFinished(long time) throws InterruptedException {
		// TODO Auto-generated method stub
		mutex.await(time, TimeUnit.MILLISECONDS);
	}

	@Override
	public void message(String msg) {
		// TODO Auto-generated method stub
		this.message = msg;
	}

	@Override
	public int state() {
		// TODO Auto-generated method stub
		return state;
	}

	@Override
	public void state(int state) {
		this.state = state;
	}

	@Override
	public String message() {
		// TODO Auto-generated method stub
		return message;
	}

	@Override
	public Object value() {
		// TODO Auto-generated method stub
		return value;
	}

}