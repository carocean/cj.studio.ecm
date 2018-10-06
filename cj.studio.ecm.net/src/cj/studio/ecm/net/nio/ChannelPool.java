package cj.studio.ecm.net.nio;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.net.IClient;
import cj.studio.ecm.net.util.IdGenerator;

public class ChannelPool implements IChannelPool {
	DefaultChannelGroup set;// data
	TreeMap<ChannelId, Long> idle;// indexer
	Set<ChannelId> busy;// indexer
	int capcity;
	long idleTimeout;// 空闲休眠时间，空闲在池中超出此时间则关闭连接。默认－1永远休眠，表示空闲连接不被释放
	long idleCheck;
	long activeTimeout;// 激活等待超时，池满且无空闲时，申请新信道等待时间，超出超时。默认－1永远等待。
	IClient client;
	Condition mutex;
	ReentrantLock lock;

	public ChannelPool(IClient client, int capcity, long idleTimeout,
			long idleCheck, long activeTimeout) {
		set = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
		busy = new LinkedHashSet<ChannelId>();
		idle = new TreeMap<ChannelId, Long>();
		lock = new ReentrantLock();
		mutex = lock.newCondition();
		this.client = client;
		this.capcity = capcity;
		this.idleTimeout = idleTimeout;
		this.idleCheck = idleCheck;
		this.activeTimeout = activeTimeout;
	}

	@Override
	public long idleCheck() {
		return idleCheck;
	}

	@Override
	public void dispose() {
		idle.clear();
		busy.clear();
		set.clear();
	}

	@Override
	public void add(Channel channel) {
		if (set.size() >= capcity) {
			channel.close();
			throw new EcmException("信道池已满");
		}
		if (set.contains(channel))
			return;
		lock.lock();
		set.add(channel);
		idle.put(channel.id(), System.currentTimeMillis());

		try {
			mutex.signal();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void remove(Channel ch) {
		idle.remove(ch.id());
		busy.remove(ch.id());
		set.remove(ch);
	}

	@Override
	public int idleCount() {
		return idle.size();
	}

	@Override
	public int busyCount() {
		return busy.size();
	}

	@Override
	public int capacity() {
		return capcity;
	}

	@Override
	public Channel get() throws TimeoutException {

		lock.lock();
		try {
			// 如果不进行idle检入检测，则说明只是在现有的池大小里负载。
			Channel ch = null;
			if (idleCheck >= 0 && idle.size() < 1 && set.size() >= capcity) {
				// 池满且全是busy了，则堵塞。
				try {
					if (activeTimeout < 0)
						mutex.await(activeTimeout, TimeUnit.MILLISECONDS);
					else
						mutex.await();
				} catch (InterruptedException e) {
					throw new TimeoutException(e);
				}
			}
			if (idleCheck < 0&&set.size()>=capcity) {// 如果不检入空闲且已满，则按负载算法
				int random = IdGenerator.newInstance().asShortText().hashCode();
				int t = random % set.size();
				int i = 0;
				for (Channel c : set) {
					if (i == t) {
						ch = c;
						idle.remove(ch.id());
						busy.add(ch.id());
						return ch;
					}
					i++;
				}
			} else {
				if (idle.size() > 0) {
					ch = set.find(idle.firstKey());
					idle.remove(ch.id());
					busy.add(ch.id());
					return ch;
				}
			}
			if (set.size() < capcity) {// 申请新连接,事件方式,baseClient中实现
				try {
					ch = (Channel)client.connect(client.getHost(), client.getPort());// 该方法会触发此新建信道的入池动作，由于在此直接入池了，所以得屏蔽掉。
					set.add(ch);
					busy.add(ch.id());
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				return ch;
			}

			return null;
		} catch (Exception e) {
			throw e;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void idle(Channel channel) {
		if (!set.contains(channel))
			return;
		if (idle.containsKey(channel.id()))
			return;
		lock.lock();
		busy.remove(channel.id());
		idle.put(channel.id(), System.currentTimeMillis());
		try {
			mutex.signal();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void run() {
		if (idle.size() < 2 && busy.size() < 1)
			return;// 至少留下一个连接.
		Set<ChannelId> idleSet = idle.keySet();
//		System.out.println("pool:" + idleSet.size());
		for (ChannelId id : idleSet) {
			long l = idle.get(id);
			long i = System.currentTimeMillis() - l;
//			System.out.println("pool:" + i + " timeout:" + idleTimeout);
			if (i >= idleTimeout) {
				Channel ch = this.set.find(id);
				remove(ch);
				ch.close();
			}
		}
	}
}