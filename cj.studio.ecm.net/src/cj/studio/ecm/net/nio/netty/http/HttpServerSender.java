package cj.studio.ecm.net.nio.netty.http;

import java.util.HashMap;
import java.util.Map;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPlug;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.nio.NetConstans;
import cj.ultimate.util.StringUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

/**
 * 绑定物理通道，用于发送信息，并将graph输出的协议转换为物理通道认识的消息。
 * 
 * <pre>
 *
 * </pre>
 * 
 * @author carocean
 *
 */
public class HttpServerSender implements ISink {
	// DefaultChannelGroup channels;
	Map<String, Channel> channels;
	SenderFrameHelper helper;
	public HttpServerSender() {
		// channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
		channels = new HashMap<String, Channel>();
		this.helper=new SenderFrameHelper();
	}

	/*
	 * 经测试，主动回复块给netty 的管道是不行的，原因是：
	 * netty的请求在执行时必须回写TRANSFER_ENCODING，而后不能退出，直到处理完。而此时如果什么也不做就等远程数所送达，而从远程的ouputpin得到数据再主动写回原pipleline
	 * 而此时的管道仍在堵塞中，因此netty的handler都没有收到数据输入。大概是非当前请求线程是其它从尾到头要执行的写线程均会被堵塞，只有在原请求线程中能回写回去
	 * 因此，该方法不适用于非原请求线程下的调用,在原请求线程下可以，也就是可看成是通过http的netinput回写的一个用例。
	 * 
	 * netty nio的结构是channel绑定一个pipeline,而一个pipeline绑定一个channel，这导致在外部主动回写的争用，而我的rio采用的是bomchat原生程序，与netty无关，因此不受影响。
	 * channel.write()
	 * 		pipeline.write()
	 * 			handler.write();
	 * 
	 * 检测netty的这个问题方法很简单，就是在原请求执行方法中开始一个新线程回写，则一定堵塞，直接在主线程下就可以。
	 * 
	 * 证据：
	 * 
	 * boolean SingleThreadEventExecutor.inEventLoop(Thread thread)
	@Override
	public boolean inEventLoop(Thread thread) {
	    return thread == this.thread;
	}
	
	AbstractEventExecutor
	@Override
	public boolean inEventLoop() {
	    return inEventLoop(Thread.currentThread());
	}
	 * 
	 * 
	 * 
	 *  @Override
    public void invokeWrite(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg == null) {
            throw new NullPointerException("msg");
        }

        validatePromise(ctx, promise, true);

        if (executor.inEventLoop()) {
            invokeWriteNow(ctx, msg, promise);
        } else {//如果不在inEventLoop线程全放在了等待队列而不执行
            AbstractChannel channel = (AbstractChannel) ctx.channel();
            int size = channel.estimatorHandle().size(msg);
            if (size > 0) {
                ChannelOutboundBuffer buffer = channel.unsafe().outboundBuffer();
                // Check for null as it may be set to null if the channel is closed already
                if (buffer != null) {
                    buffer.incrementPendingOutboundBytes(size);
                }
            }
            safeExecuteOutbound(WriteTask.newInstance(ctx, msg, size, promise), promise, msg);
        }
        
    }
    
    	由于netty相关类搞成了：final，因此无法扩展，实际上只要executor.inEventLoop()永远返回true即可，目前折衷办法：
    	或者重新编译原码。由于正向反向的执行，netty不停的改变绑定的thead因此下面的代码一点都不可靠
    	for (EventExecutor e : server.getGroup()
									.children()) {
								Field f = findField(e.getClass());
								f.setAccessible(true);
								f.set(e, Thread.currentThread());
							}
							
		如果能成功编译源码，可以在创建工作线程池时，指定executor的创建，因为此时可以将final去掉，将构造公开
	 */
	protected void sendHttpChunked(Channel ch, Frame frame, Circuit circuit)
			throws CircuitException {
		if ("open".equals(frame.command())) {
			DefaultHttpResponse resp = new DefaultHttpResponse(
					HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
			String mime = frame.contentType();// 在httpSink.setMime的方法中已设置mime类型
			if (StringUtil.isEmpty(mime)) {
				mime = "text/html";
			}
			resp.headers().set(HttpHeaders.Names.CONTENT_TYPE, mime);
			resp.headers().set(HttpHeaders.Names.TRANSFER_ENCODING,
					HttpHeaders.Values.CHUNKED);
			if (!"keep-alive".equals(frame.head("Connection"))) {
				resp.headers().set(HttpHeaders.Names.CONNECTION,
						HttpHeaders.Values.KEEP_ALIVE);
			}

			ch.writeAndFlush(resp);
			System.out.println(
					ch.isActive() + " " + ch.isOpen() + " " + ch.isWritable());
			circuit.piggybacking(false);
			return;
		} else if ("close".equals(frame.command())) {
			LastHttpContent last = LastHttpContent.EMPTY_LAST_CONTENT;// new
																		// DefaultLastHttpContent();
			ChannelFuture lastContentFuture = ch.writeAndFlush(last);
			// Decide whether to close the connection or not.
			if (!"keep-alive".equals(frame.head("Connection"))) {
				// Close the connection when the whole content is written
				// out.
				lastContentFuture.addListener(ChannelFutureListener.CLOSE);
			}
			circuit.piggybacking(false);
			return;
		} else {
			DefaultHttpContent backmsg = new DefaultHttpContent(
					frame.content().raw());
			ch.writeAndFlush(backmsg);
		}
	}

	@Override
	public void flow(Frame frame, Circuit circuit, IPlug plug)
			throws CircuitException {
		if ("NET/1.1".equals(frame.protocol())) {
			if ("connect".equals(frame.command())) {
				// http主动通讯:ws或chunked
				// if (!"ws".equals(circuit.attribute("select-simple"))) {
				// return;
				// }
				if ("senderIn".equals(plug.owner())) {
					ChannelHandlerContext ctx = (ChannelHandlerContext) circuit
							.attribute("ctx");
					channels.put(ctx.channel().id().asShortText(),
							ctx.channel());
				} else {
					throw NetConstans
							.newCircuitException(NetConstans.STATUS_612);
				}
				return;
			}
			if (frame.command().equals("disconnect")) {
				if ("senderIn".equals(plug.owner())) {
					ChannelHandlerContext ctx = (ChannelHandlerContext) circuit
							.attribute("ctx");
					channels.remove(ctx.channel().id().asShortText());
				} else {// 调用者请求关闭连接
					String sid = (String) circuit.attribute("select-id");
					Channel ch = channels.get(sid);
					if (ch == null) {
						circuit.status("404");
						circuit.message("要关闭的连接不存在。");
						return;
					}
					ch.close();
					channels.remove(sid);
				}
				return;
			}
			return;
		}
		// 以下是主动发送，因为http仅支持ws的主动发送(不对还有chunked情况），而http的晌应是捎带，并在server处理后已有捎带处理。
		// if (!"ws".equalsIgnoreCase((String)
		// circuit.attribute("select-simple"))) {
		// throw new CircuitException("509", "httpServerSender 仅支持ws信道发送");
		// }
		String id = (String) circuit.attribute("select-id");
		Channel ch = channels.get(id);
		if (ch == null) {
			throw NetConstans.newCircuitException(NetConstans.STATUS_602,
					circuit.attribute("select-id"));
		}
		// frame.head("status",circuit.head("status"));
		// frame.head("message",circuit.message());
		// frame.head("cause",circuit.cause());
			
		if ("CHUNKED/1.0".equals(frame.protocol())) {
			// 不论是http/rio-等协议，都可能向http发送块请求，非http是远程转入的请求
			sendHttpChunked(ch, frame, circuit);
		}else{
			helper.sendWsMsg(ch, frame);
		}
	}

}
