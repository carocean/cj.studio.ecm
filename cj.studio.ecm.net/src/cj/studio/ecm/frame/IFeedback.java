package cj.studio.ecm.frame;

import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.net.nio.NetConstans;
import io.netty.buffer.ByteBuf;

/**
 * 用于回路中倒序执行。
 * 
 * <pre>
 * 回路返回器是在正向执行序中实现倒序执行再正向的机制，则正向执行序是主回路，倒序执行是子回路
 * 
 * 在net回路中有以下规定：
 * －当前回路如果是由net线程执行的，则net线程为主回路，是条从net接收信息的回路
 *   －－在主回路中可获取到IFeedback.KEY_OUTPUT_FEEDBACK回馈对象，可以通过它实现向net的输出
 *   －－如果输出回路circuit对象中设置了IFeedback.KEY_INPUT_FEEDBACK回路，则此子回可用于接收从net中返回的侦
 * －当前回路如果是由应用线程执行的，则应用线程为主回路，是条向net输出的回路
 *   －－如果输出回路circuit对象中设置了IFeedback.KEY_INPUT_FEEDBACK回路，则此子回可用于接收从net中返回的侦
 * 	 
 * <b>注意：如果向net输出信息且欲等待返回侦，建议采用输出回馈回路，这是因为：
 * －输出等待返回侦都有个线程超时问题，一般大并发系统下，超时时间过长，非常耗性能，而对于长时任务处理正常的同步模式基本上只能得到超时，因此：
 *   采用输入回路做等待，当超时时net线程会压送给接收点，因此：
 *   输出回路如果采用了这种机制，线程在超时时会切换到net线程，开发者不必关注线程切换问题，但应知道有这样的工作。
 *   超时后采用的机制是异步机制，可在接收sink的circuit状态中判断是同步收到的返回侦还是异步收到的返回侦。
 *   net维护一个请求池，请求池默认等待时间是8分钟，超过此时间丢弃请求侦，而
 *   即便该请求侦的反回侦由net收到，由于已没有请求侦匹配，因此被丢弃。
 * </b>
 * 
 * 先前解释：
 * 
 * 即处理晌应的流,它使得一个回路成为双工通道
 * 
 * 回馈含有一个导线,导线的插入每次是向首部插入，故最后插入的最先执行，故此在当前回路中形成回馈作用
 * 
 * 用法：
 * 可在回路的两端控制，在源端插入sink，在终端执行回馈方法：doBack,
 * 	其中在回路的任意sink的flow方法中，
 * 	均可为之添加回馈sink以过滤回馈信息，
 * 	如果其中的一个sink中断当前回馈流，
 * 	则回馈流到此终止。
 * 
 * 注意：
 * net的输出端子具有一个输出回馈点
 * net的输入端子如有需要可添加一个回馈点以发送侦。
 * </pre>
 * 
 * @author carocean
 * @see NetConstans
 */
public interface IFeedback {

	/**
	 * 回馈到回馈点。该方法从当前点向源头执行
	 * 
	 * <pre>
	 * 回馈点由circuit对象设置，可设置多个回馈点。
	 * 
	 * net的output线路中使用了输出回馈点,用于主动向对端推送，其用法如下：
	 * 	1.frame.head(netconstans.FRAME_HEADKEY_CIRCUIT_SYNC,"true")
	 * 		表示要同步接收对端捎带返回信息。
	 * 	2.circuit可指定输入回馈点，用于同步等待接收对端返回的捎带侦
	 * 	3.可以多次调用doBack以与对端无限次通讯
	 * 
	 * httpChunked的用法：
	 * 根据请求资源的扩展名（在assemblies.properties中配的mime type)找到mime，系统为之自动设置，如果调用者设置了mime类型，则使用调用者的设置
	 * IFeedback fb = circuit.feedback(IFeedback.KEY_OUTPUT_FEEDBACK);
				byte[] b = new byte[8096];
				int readlen = 0;
				Frame f=new Frame("open / chunked/1.1");
				f.content().writeBytes(b, 0, readlen);
				fb.doBack(f, circuit);//打开一个块写
				while ((readlen = reader.read(b)) > -1) {
					 f=new Frame("chunked / http/1.1");
					f.content().writeBytes(b, 0, readlen);
					fb.doBack(f, circuit);//写块数据
				}
				f=new Frame("close / chunked/1.1");
				fb.doBack(f, circuit);//关闭块写
	 * 
	 * 
	 * 注意：
	 * 1.如果是net的output线路中使用了输出回馈点，只用于在输出通道内向对端主动通讯，
	 * 	如果需要主通道捎带信息，则直接写入主通道的回路，而不应用输出回馈点。
	 * 2.如果使用了net的input线路中使用了输入回馈点，则所在主通道的回路对象circuit的内容中将不包含返回侦，
	 * 如有需要，可在回馈点中对主通道回路赋值。且一定将侦写入回路内容，并标记内容类型为frame/bin,frame/json
	 * 
	 * 再注意：
	 * －用于输出的回馈回路，即从KEY_OUTPUT_FEEDBACK得到的回路，调用其doBack可向net回馈信息，即向net输出了
	 * －用于输入的回馈回路，即开发者设置的KEY_OUTPUT_FEEDBACK回路，是由net内核调用的，开发者不能调用doBack，
	 *   而只需插入sink以接收net返回侦
	 * </pre>
	 * 
	 * @param content
	 *            回馈侦
	 */
	public void write(ByteBuf content,Circuit circuit) throws CircuitException;
	public void begin(Circuit circuit);
	public void done(ByteBuf content,Circuit circuit);

}
