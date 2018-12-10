package cj.studio.ecm.net.rio;

import cj.studio.ecm.net.SyncPool;

public class SendQueue extends SyncPool{
	private long used;
	private long circuitDefaultSyncTime=3600;//默认的同步等待时间
	/**
	 * 
	 * <pre>
	 *
	 * </pre>
	 * @param capacity
	 * @param defaultCircuitSyncTime2 默认的同步等待时间
	 */
	public SendQueue(int capacity, long defaultCircuitSyncTime2) {
		super(capacity);
		this.circuitDefaultSyncTime=defaultCircuitSyncTime2;
	}
	/**
	 * 只要产生的id不与发送队列中的冲突即可。
	 * <pre>
	 *
	 * </pre>
	 * @return
	 */
	public String genFrameId(){
		if(used>=Long.MAX_VALUE
				){
			used=1;
			return String.valueOf(used);
		}
		used++;
		return String.valueOf(used);
	}
	public void setCircuitDefaultSyncTimeout(long circuitSyncTime) {
		this.circuitDefaultSyncTime=circuitSyncTime;
	}
	public long getCircuitDefaultSyncTime() {
		return circuitDefaultSyncTime;
	}
}
