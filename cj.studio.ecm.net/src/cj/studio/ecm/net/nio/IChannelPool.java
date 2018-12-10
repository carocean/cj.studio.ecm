package cj.studio.ecm.net.nio;

import io.netty.channel.Channel;
import cj.ultimate.IDisposable;

/**
 * 信道池。用于客户端获取发送端子。
 * 
 * <pre>
 * 
 * 策略：
 * 1.获取空闲信道，采用mod算法，如果没有空闲的
 * 2.看是否到池上限，如果不到就新建连接信道，如果到上限
 * 3.则按mod算法在整个池中选择信道（此时池中的信道是busy状态的）
 * 4.由于是负载器不是连接池，因此支持返回busy信道，且负载器不堵塞
 * 
 * </pre>
 * 
 * @author carocean
 *
 */
public interface IChannelPool extends IDisposable,Runnable {
	long idleCheck();
	/**
	 * 在client连接时将信道放入
	 * <pre>
	 *
	 * </pre>
	 * @param channel
	 */
	void add(Channel channel);
	void remove(Channel ch);
	int idleCount();
	int busyCount();
	/**
	 * 根据负载算法返回可用的信道
	 * <pre>
	 * 该方法在client的sender中调用。
	 * 
	 * 如果池到上限可无空闲信道，则堵塞，直到有可用为止，或超时
	 * </pre>
	 * @return
	 */
	Channel get() throws TimeoutException;
	/**
	 * 设置指定的信道为空闲
	 * <pre>
	 *
	 * </pre>
	 * @param channel
	 */
	void idle(Channel channel);
	int capacity();
}
