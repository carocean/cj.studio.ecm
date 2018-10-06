package cj.studio.ecm.net;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import cj.studio.ecm.CJSystem;
import cj.ultimate.IDisposable;

public class SyncPool implements IDisposable {
	Semaphore pool;
	ReentrantLock reen;
	ConcurrentHashMap<String, Condition> locks;
	ConcurrentHashMap<String, Object> extra;

	public SyncPool(int capacity) {
		locks = new ConcurrentHashMap<String, Condition>(capacity);
		extra = new ConcurrentHashMap<String, Object>();
		pool = new Semaphore(capacity);
		reen = new ReentrantLock();
	}

	@Override
	public void dispose() {
		pool.release(pool.availablePermits());
		locks.clear();
		extra.clear();
	}

	public static void main(String... pool) throws InterruptedException {
		SyncPool p = new SyncPool(2);
		p.put("2");
		p.put("3");
		p.take("2", -1);
		p.put("4");
		p.put("5");
		p.put("6");
	}

	/**
	 * 放入一个元素，如果池满则等待
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param key
	 * @return 如果放入成功则返回true
	 */
	public boolean put(String key) {
		try {
			reen.lock();
			if (!locks.containsKey(key)) {// 如果已存在则是替换，不存在则视为添加，因此需要等等
				pool.acquire();
			}
			locks.put(key, reen.newCondition());
			return true;
		} catch (InterruptedException e) {
			return false;
		} finally {
			reen.unlock();
		}

	}

	/**
	 * 等待一个元素，如果超时线程将被放行
	 * 
	 * <pre>
	 * 如果超时了，则移除锁
	 * </pre>
	 * 
	 * @param key
	 * @param m
	 *            等待毫秒数，－1表示无限期直到得到通知
	 * @return 返回可拿的数据，如果有。该数据被通知线程设置
	 */
	public Object take(String key, long m) {
		Condition lock = locks.get(key);
		if (lock != null) {
			try {
				reen.lock();
				if (m < 0) {
					lock.await();
				} else {
					// 当该方法返回false时为超时。可是在黄金点上受到通知时，它并不总是在超时时退出，有时正确的通知到了它也返回false，但不影响结果正确。
					// 估计是通知时与超时时的判断逻辑不正确，故而无法用超时。
					// 之前版本是使用:Object.wait(222);
					// lock.await(m,TimeUnit.MILLISECONDS);
					if (!lock.await(m, TimeUnit.MILLISECONDS)) {
						// throw new
						// TimeOutException(String.format("%s,设定的超时毫秒：%s",
						// key,m));
//						CJSystem.current()
//								.environment()
//								.logging()
//								.debug(String.format(
//										"确认侦等待超时.id：%s,设定的超时毫秒：%s", key, m));
					}
				}
				Object obj = extra.get(key);
				return obj;
			} catch (InterruptedException e) {
//				locks.remove(key);// 如果超时了，则移除，并同步池数
//				extra.remove(key);
				// pool.release();
				// System.out.println(String.format("线程中断或因超时：%s",key));
				return null;
			} finally {
				extra.remove(key);
				locks.remove(key);// 只要方法能执行过，则说明锁用完就必须释放。
				reen.unlock();
				pool.release();
			}
		}
		return null;

	}

	/**
	 * 是包含有指定的key
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param key
	 * @return
	 */
	public boolean contains(String key) {
		return locks.containsKey(key);
	}

	/**
	 * 通知等待的key继续线程，并移除指定的key
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param key
	 * @param data
	 *            附加的数据，该数据可通过take方法获取到
	 * @return true成功通知，不管是否已超时；false不成功，即key锁已不存在,发生这种情况，一是可能从没有放入过该key，
	 *         二是或已被take也可能超时时take的
	 */
	public boolean notifya(String key, Object data) {
		Condition lock = locks.get(key);
		if (lock != null) {
			try {
				reen.lock();
				// System.out.println("notifya :" + key + " " + data);
				extra.put(key, data);
				lock.signalAll();
				return true;
				// locks.remove(key);//注掉原因：如果通知时将锁移除，则在take时得不到锁，如果take线程还未收到notify通知，而将此锁移除了，因为在take方法锁非空才进入方法，因此为导致take返回为空值。
				// pool.release();//注掉原因：因为在take一个时释放一个容量最为合理，否则在此处释放一个，take处的locks&extra还未腾出一个池元素。
			} catch (Exception e) {
				CJSystem.current().environment().logging()
						.error(this.getClass(), e.getMessage());
				return false;
			} finally {
				reen.unlock();
			}
		} else {
			return false;
		}
	}

}
