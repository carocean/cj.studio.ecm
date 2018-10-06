package cj.studio.ecm.net.nio;

import java.lang.reflect.Field;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.CjStatusDef;
import cj.studio.ecm.graph.IConstans;

/**
 * net的常量
 * 
 * <pre>
 *
 * </pre>
 * 
 * @author carocean
 *
 */
public interface NetConstans extends IConstans {
	public static CircuitException newCircuitException(String status,Object...args){
		String format=format(status, args);
		return new CircuitException(status, format);
	}
	public static String format(String status,Object...args){
		String format=message(status);
		return String.format(format, args);
	}
	public static String message(String status) {
		Class<?> clazz = NetConstans.class;
		Field f;
		try {
			f = clazz.getDeclaredField(String.format("STATUS_%s", status));
			CjStatusDef e = f.getAnnotation(CjStatusDef.class);
			if (e == null) {
				throw new EcmException(
						String.format("协义代码:%s 未声明为CjStatusDef或者代码定义不合规范，规范为:STATUS_NUM", status));
			}
			return e.message();
		} catch (NoSuchFieldException | SecurityException e1) {
			throw new EcmException(e1);
		}
	}

	@CjStatusDef(message = "NET/1.1")
	String PROTOCAL = "protocol";
	/**
	 * 同步接收成功
	 */
	@CjStatusDef(message = "ok")
	String STATUS_200 = "200";
	/**
	 * 异步接收成功
	 */
	@CjStatusDef(message = "同步超时，已成功调整为异步接收")
	String STATUS_201 = "201";
	/**
	 * 不论同步和异步均超时失败
	 */
	@CjStatusDef(message = "超时，失败")
	String STATUS_202 = "202";
	@CjStatusDef(message = "net建立连接失败，拒绝连接")
	int STATUS_691 = 691;
	@CjStatusDef(message = "电缆中不存在选定的导线名：%s")
	String STATUS_604 = "604";
	
	@CjStatusDef(message = "电缆线中不存在默认导线。")
	String STATUS_605 = "605";
	@CjStatusDef(message = "电缆线中没有可用导线，请检查net:%s工作状态。")
	String STATUS_606 = "606";
	
	@CjStatusDef(message = "回路中未定义异常。")
	String STATUS_603 = "603";
	@CjStatusDef(message = "channel 未查到.select-id=%s")
	String STATUS_602 = "602";
	@CjStatusDef(message = "填充侦出错。")
	String STATUS_601 = "601";
	@CjStatusDef(message = "不接受的侦协议")
	String STATUS_600 = "600";
	@CjStatusDef(message = "回路中设置的超时间溢出，net要求最大同步超时时间为：%s")
	String STATUS_607 = "607";
	@CjStatusDef(message = "回路中设置的异步超时溢出，net要求最大异步超时时间为：%s")
	String STATUS_608 = "608";
	@CjStatusDef(message = "写入远程套节字出错")
	String STATUS_609 = "609";
	@CjStatusDef(message = "信道池超时")
	String STATUS_610 = "610";
	@CjStatusDef(message = "客户端连接器不支持指向连接")
	String STATUS_611 = "611";
	@CjStatusDef(message = "http不支持主动发起连接申请")
	String STATUS_612 = "612";
	@CjStatusDef(message = "UDT不支持主动发起连接申请")
	String STATUS_613 = "613";
	
	@CjStatusDef(message = "资源不存在")
	String STATUS_404 = "404";
	@CjStatusDef(message = "服务器发生错误")
	String STATUS_503 = "503";
	
	@CjStatusDef(message = "会话为空")
	String STATUS_560 = "560";
	@CjStatusDef(message = "会话非法")
	String STATUS_561 = "561";
	@CjStatusDef(message = "website服务器内部重定向异常")
	String STATUS_571 = "571";
	@CjStatusDef(message = "程序集不支持httpframe侦")
	String STATUS_801 = "801";
	@CjStatusDef(message = "未在frame.parameter(select-simple)中指定通路")
	String STATUS_802 = "802";
	@CjStatusDef(message = "websiteGraph未使用会话层")
	String STATUS_803 = "803";
	@CjStatusDef(message = "website处理异常")
	String STATUS_805 = "805";
	/**
	 * 同步侦的id键，由net自动产生值
	 */
	String FRAME_HEADKEY_FRAME_ID = "cj-frame-id";
	/**
	 * 显示异常原因，将在捎带的侦的内容中显示异常明细，默认为不显示,true为显示 <br>
	 * 
	 */
	// 这行功能还没实现，如果实现更为合理：如果欲关闭开发者在侦中请求的该项信息，可在程序集上下文属性文件中将chip.net.rio.display.exceptionCause=false，默认为false
	String FRAME_HEADKEY_RIO_DESPLAYEXCEPTIONCAUSE = "cj-frame-rio-exception-dispaly";
	/**
	 * 回路是否同步等待，侦指定此键，但并不用于net间传输。
	 */
	String FRAME_HEADKEY_CIRCUIT_SYNC = "cj-circuit-sync";
	/**
	 * 等待客户端连接超时时间的默认值
	 */
	long waitClientDisConnectTimeout=8000;
	/**
	 * 等待服务器端接受事件超时
	 */
	long waitServerDisConnectTimeout=8000;
	/**
	 * 等待客户端连接超时时间的默认值
	 */
	long waitClientConnectTimeout=60000;
	/**
	 * 等待服务器端接受事件超时
	 */
	long waitServerConnectTimeout=15000;
	/**
	 * 同步回路待等超时时间键
	 * 
	 * <pre>
	 * －在frame.head中设置
	 * －默认是3600毫秒,如果开发者设定的少于此值，仍是3600
	 * </pre>
	 */
	String FRAME_HEADKEY_CIRCUIT_SYNC_TIMEOUT = "cj-sync-timeout";

	/**
	 * 异步回路待等超时时间键
	 * 
	 * <pre>
	 * －在frame.head中设置
	 * －默认是3600毫秒,如果开发者设定的少于此值，仍是3600
	 * 
	 * 约束：该常量键仅支持rio net
	 * </pre>
	 */
	String FRAME_HEADKEY_CIRCUIT_ASYNC_TIMEOUT = "cj-async-timeout";
	/**
	 * 回路支持的最大同步超时时间为12小时
	 */
	long FRAME_HEADKEY_CIRCUIT_SYNC_MAX_TIMEOUT = 43200000L;
	/**
	 * 回路支持的最大异步超时时间为1小时
	 */
	long FRAME_HEADKEY_CIRCUIT_ASYNC_MAX_TIMEOUT = 3600000L;
	
	long FRAME_MAX_LENGTH=1073741824L;//默认1g
	
	String CIRCUIT_ATTRKEY_CHUNK_DATAFILE = "Cj-Chunk-File";
	String FRAME_HEADKEY_CHUNK_DATAFILENAME = "Cj-Chunk-FileName";
	String FRAME_HEADKEY_CHUNK_DATAFILELENGTH = "Cj-Chunk-FileLength";
	String FRAME_HEADKEY_CHUNK_HASDATAFILE = "Cj-chunk-hasFile";
//	String NEURON_CHUNKED = "Neuron-Chunked";
	String RIO_NET_SENDQUEUESIZE = "1024";//发送等待队列大小，里面是等待的回路
	int CHANNEL_READ_BUFSIZE = 8192;//64K
	String PIN_WORK_STATUS_KEY = "net_work_status";
	
}
