package cj.studio.ecm.frame;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPrinter;
import cj.studio.ecm.net.nio.NetConstans;
import cj.ultimate.IDisposable;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 回路。它是一个执行序列
 * 
 * <pre>
 * 用于方法调用的信息回馈、执行序中传递属性引用等。
 * 
 * 回路不是固定的，它在flow的过程中才惯穿各相关的插头节点。
 * 
 * 如果回路要去向一个分支端子，需要将主插头中的回路传给下一个分支端子或者为之新建回路亦可。
 * 
 * 回路的机制保证在一个端子中不能被替换，从而使得端子的flow方法调用者处理回馈和传入属性。
 * 
 * 回路不像frame一样放到sink的flow参数上，是为了避免每个sink开发者任意替换掉回路，而导致回路的最初持有者失去回路的引用。另外，它不止用于回馈，它还要在执行序内传递属性。
 * 
 * 
 * </pre>
 * 
 * <b>注意：</b>对于net同步模式下，响应侦被包装在回路的content中。如果想跳过回路中的各sink得到侦，可使用回路的callback方法。
 * 
 * @author carocean
 *
 */
public class Circuit implements IPrinter, IDisposable {
	private Map<String, String> headmap;
	private Map<String, Object> attributemap;
	protected IFlowContent content;
	static final String CODE = "utf-8";
	private IFeedback feedback;

	// private List<ICircuitCallback> callbacks;
	public Circuit(String frame_line,IFeedback feedback) {
		this(frame_line);
		this.feedback=feedback;
	}
	public Circuit(String frame_line) {
		init();
		String[] arr = frame_line.split(" ");
		if (arr.length > 0)
			head("protocol", arr[0].toUpperCase());
		if (arr.length > 1)
			head("status", arr[1]);
		if (arr.length > 2)
			head("message", arr[2]);
		if (arr.length > 3)
			throw new RuntimeException("格式错误");
	}
	public boolean containAtrribute(String attr){
		if(attributemap==null){
			return false;
		}
		return attributemap.containsKey(attr);
	}
	@Override
	public void dispose() {
		headmap.clear();
		if (attributemap != null)
			attributemap.clear();
		if (content.refCnt() > 0)
			content.release();
	}

	Circuit() {
		init();
	}

	/**
	 * 以一个侦作为原型创建回路。
	 * 
	 * <pre>
	 * 浅表复制。
	 * 侦的参数将丢弃
	 * </pre>
	 * 
	 * @param f
	 */
	public Circuit(Frame f) {
		init();
		headmap = f.headmap;
		content.writeBytes(f.content.copy());
	}
	public Circuit(Frame f,IFeedback feedback) {
		this(f);
		this.feedback=feedback;
	}
	private void init() {
		headmap = new Hashtable<String, String>(4);
		content = createContent(8192);
	}

	public static void main(String... headmap) {
		Circuit c = new Circuit("net/1.1 200 ok");
		c.head("name", "zhaoxb");
		c.head("password", "22222");
		c.content.writeBytes("asld\r\nkfjalsdfjklad".getBytes());
		c.contentChartset("utf-8");
		Frame f = c.snapshot();
//		Frame f=new Frame(c.toBytes());
		Frame f2=new Frame(f.toBytes());
		byte[] b2 = new byte[f2.content.readableBytes()];
		f2.content.readBytes(b2);
		System.out.println(new String(b2));
	}


	/**
	 * 回路返馈器
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
	 * <b>建议采用回馈机制接收返回侦，这样可尽大可能确保能等到返回侦</b>
	 * </pre>
	 * 
	 * @see NetConstans
	 * @return 注意：可能返回为空。
	 * @throws CircuitException 
	 */
	public void writeFeeds(Object content) throws CircuitException {
		if(feedback!=null) {
			feedback.write(content);
		}
	}
	public void beginFeeds(Object content) {
		if(feedback!=null) {
			feedback.begin(content);
		}
	}
	public void doneFeeds(Object content){
		if(feedback!=null) {
			feedback.done(content);
		}
	}
	public boolean hasFeedback() {
		return feedback!=null;
	}
	/**
	 * 回路信息是否可被网络捎带.该属性仅在net/1.1中有效 attributemap.get("piggybacking");
	 * 
	 * <pre>
	 * -1.如果是sync=true则必须捎带
	 * 0.如果回路有错误(status!=200)，则捎带
	 * 1.如果有piggybacking属性且由其值确定捎不捎带
	 * 2.否则：
	 * 2.1.如果有内容则捎带
	 * 2.2.否则不捎带
	 * </pre>
	 * 
	 * @return
	 */
	public boolean isPiggybacking() {
		boolean piggy = false;
		if ("true".equals(headmap
				.containsKey(NetConstans.FRAME_HEADKEY_FRAME_ID)))
			return true;
		if (!"200".equals(status())) {
			piggy = true;
			return piggy;
		}
		if (attributemap != null && attributemap.containsKey("piggybacking")) {
			piggy = (boolean) attributemap.get("piggybacking");
		} else {
			if (content.readableBytes() > 0)
				piggy = true;
			else
				piggy = false;
		}
		return piggy;
	}

	/**
	 * 设为本回路信息可被网络捎带。
	 * 
	 * <pre>
	 * 如果设为true一定要捎带，那怕为空。
	 * 如果设为false一定不要捎带哪怕有内容。
	 * 
	 * </pre>
	 * 
	 * @param piggybacking
	 */
	public void piggybacking(boolean piggybacking) {
		attribute("piggybacking", piggybacking);
	}

	protected IFlowContent createContent(int initLen) {
		ByteBuf buf = null;
		buf = Unpooled./* directBuffer */buffer(initLen);
		return new NettyContent(buf);
	}

	public byte[] toBytes() {
		ByteBuf b = Unpooled.buffer();
		byte[] crcf = null;
		try {
			crcf = "\r\n".getBytes(CODE);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		headmap.put("Content-Length",
				Integer.toString(content.readableBytes()));
		for (String key : headmap.keySet()) {
			String v = headmap.get(key);
			if (StringUtil.isEmpty(v)) {
				continue;
			}
			String tow = key + "=" + v + "\r\n";
			try {
				b.writeBytes(tow.getBytes(CODE));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		b.writeBytes(crcf);
		b.writeBytes(crcf);
		b.writeBytes(content.copy());// 非常变态，bytebuf数组总是在结尾入多一个0，因此其长度总是比写入的长度多1个字节
		byte[] newArr = new byte[b.readableBytes()];
		b.readBytes(newArr);
		b.release();
		return newArr;
	}

	public ByteBuf toByteBuf() {
		ByteBuf buf = Unpooled.copiedBuffer(toBytes());
		return buf;
	}

	/**
	 * 将回路转换为侦
	 * 
	 * <pre>
	 * 与快照不同，转换的侦持有回路头和内容的引用。
	 * </pre>
	 * 
	 * @param frame_line
	 * @return
	 */
	public Frame convert(String frame_line) {
		String[] arr = frame_line.split(" ");
		if (arr.length != 3) {
			throw new EcmException("侦行格式不正确");
		}
		Frame f = new Frame();
		f.content = content;
		f.headmap = headmap;
		f.command(arr[0]);
		if (StringUtil.isEmpty(arr[1]) || !arr[1].contains("/")) {
			throw new EcmException("侦行中的地址定义格式为空或不正确");
		}
		f.url(arr[1]);
		f.protocol(arr[2]);
		return f;
	}

	/**
	 * 将回路转换为侦
	 * 
	 * <pre>
	 * 与快照不同，转换的侦持有回路头和内容的引用。
	 * </pre>
	 * 
	 * @param frame_line
	 * @return
	 */
	public Frame convert(String cmd, String url) {
		Frame f = new Frame();
		f.content = content;
		f.headmap = headmap;
		f.command(cmd);
		f.url(url);
		return f;
	}

	/**
	 * 将回路转换为侦
	 * 
	 * <pre>
	 * 与快照不同，转换的侦持有回路头和内容的引用。
	 * </pre>
	 * 
	 * @param frame_line
	 * @return
	 */
	public Frame convert() {
		return convert(String.format("%s / %s", "piggyback", protocol()));
	}

	/**
	 * 回路产生一个侦快照。
	 * 
	 * <pre>
	 * 1.head集合将放入新侦的head集合中，且回路的head保留
	 * 2.快照对回路数据不影响
	 * 
	 * 注：如果回路中的参数与指定的侦行中的querystring参数同名，则报错
	 * </pre>
	 * 
	 * @param frame_line
	 *            如果指定了协议，则作用于新侦，如果没有则采用回路的协议。
	 * @return
	 */
	public Frame snapshot(String frame_line) {
		if (frame_line.split(" ").length == 2) {
			frame_line += " " + protocol();
		}
		Frame frame = new Frame(frame_line);
		for (String key : headmap.keySet()) {
			if ("protocol".equals(key))
				continue;
			frame.head(key, headmap.get(key));
		}
		// frame.head("is-piggybacking", "true");
		frame.content = content.copyIt();
		// content.clear();
		return frame;
	}

	public Frame snapshot() {
		return snapshot(String.format("%s / %s", "piggyback", protocol()));
	}

	public Frame snapshotBy(String cmd, String url) {
		return snapshot(String.format("%s %s %s", cmd, url, protocol()));
	}

	public IFlowContent content() {
		return content;
	}

	/**
	 * 
	 * <pre>
	 * 注意：回路的回馈集合不被拷贝，即每个回路的回馈集合是唯一的。
	 * </pre>
	 * 
	 * @param c
	 * @param shallow
	 */
	public void copyFrom(Circuit c, boolean shallow) {
		if (shallow) {
			this.content = c.content;
			for (String key : c.headmap.keySet()) {
				if (key.equalsIgnoreCase("protocol")
						|| key.equalsIgnoreCase("url")
						|| key.equalsIgnoreCase("command")) {
					continue;
				}
				headmap.put(key, c.headmap.get(key));
			}

		} else {
			content = new NettyContent(c.content.copy());
			for (String key : c.headmap.keySet()) {
				if (key.equalsIgnoreCase("protocol")
						|| key.equalsIgnoreCase("url")
						|| key.equalsIgnoreCase("command")) {
					continue;
				}
				headmap.put(key, c.headmap.get(key));
			}
		}
		String[] atts = c.enumAttributeName();
		for (String key : atts) {
			attribute(key, c.attribute(key));
		}
		// feedbacks.clear();
		// feedbacks.putAll(c.feedbacks);//回馈不能拷贝，因为被复制的回路对象，不一定用于同一回路返回，否则会造成回路干涉。
	}

	public String protocol() {
		return head("protocol");
	}

	/**
	 * 状态
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @return
	 */
	public String status() {
		return head("status");
	}

	public void status(String v) {
		head("status", v);
	}

	/**
	 * 消息
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param msg
	 */
	public void message(String msg) {
		head("message", msg);
	}

	/**
	 * 消息
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @return
	 */
	public String message() {
		return head("message");
	}

	/**
	 * 如发生错误，此值为错误原因，无错则为空
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @return
	 */
	public String cause() {
		return (String) attribute("$cause");
	}

	public void cause(String cause) {
		attribute("$cause", cause);
	}

	public String contentChartset() {
		return head("content-chartset");
	}

	public void contentChartset(String chartset) {
		head("content-chartset", chartset);
	}

	public String[] enumAttributeName() {
		if (attributemap == null)
			return new String[0];
		return attributemap.keySet().toArray(new String[0]);
	}

	public Object attribute(String name) {
		if (attributemap == null)
			return null;
		return attributemap.get(name);
	}

	public void attribute(String key, Object v) {
		if (attributemap == null) {
			attributemap = new HashMap<String, Object>();
		}
		attributemap.put(key, v);
	}

	public void removeAttribute(String key) {
		if (attributemap == null)
			return;
		attributemap.remove(key);
	}

	public String[] enumHeadName() {
		return headmap.keySet().toArray(new String[0]);
	}

	public String head(String name) {
		return headmap.get(name);
	}

	public void head(String key, String v) {
		if (StringUtil.isEmpty(v)){
			headmap.put(key, "");
			return;
		}
		if (!"message".equals(key) && !"cause".equals(key)
				&& (v.contains("\r") || v.contains("\n"))) {
			throw new EcmException(
					String.format("不能包含\\r 或 \\n value is %s", v));
		}
		headmap.put(key, v);
	}

	public void removeHead(String key) {
		headmap.remove(key);
	}

	@Override
	public String toString() {
		return protocol() + " " + status() + " " + message();
	}

	public String contentType() {
		return head("Content-Type");
	}

	/**
	 * min类型
	 * 
	 * <pre>
	 * 如frame/bin,frame/json,others
	 * </pre>
	 * 
	 * @param type
	 */
	public void contentType(String type) {
		head("Content-Type", type);
	}

	public boolean containsContentType() {
		return headmap.containsKey("Content-Type");
	}

	/**
	 */
	@Override
	public void print(StringBuffer sb) {
		print(sb, null);
	}

	@Override
	public void print(StringBuffer sb, String indent) {
		sb.append(new String(toBytes()));
	}

	public boolean containsHead(String name) {
		// TODO Auto-generated method stub
		return headmap.containsKey(name);
	}

	public void protocol(String protocol) {
		head("protocol", protocol);
	}
	/**
	 * 覆盖
	 * <pre>
	 * 包含by的除回馈属性
	 * </pre>
	 * @param by
	 */
	public void coverFrom(Circuit by) {
		this.attributemap=by.attributemap;
		this.content=by.content;
		this.headmap=by.headmap;
		this.feedback=by.feedback;
	}

}
