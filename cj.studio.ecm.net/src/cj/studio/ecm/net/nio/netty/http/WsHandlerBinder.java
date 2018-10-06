package cj.studio.ecm.net.nio.netty.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.graph.IPin;
import cj.studio.ecm.net.graph.IHandleBinder;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.nio.netty.INettyGraph;

/**
 * netty中有两种实现websocket的方式，一种是兼容http协议方式（因为要处理http请求的html页面）；一种是不兼容http，
 * 直接就是用websocket。如果不需要兼容，最好用第二种，因为它只处理websocket，所以性能好。
 * 本例是第二种实现。服务器端无法向客户端写html文件，因为它不兼容http处理。要了解详情可参见netty simples例程。
 */
//兼容与非兼容其实netty在底层都是调用握手的那几个类，只是在不兼容http模式下，netty将请求封装成了handler，这等同于只要将兼容模式的请求处于只用于处理ws://请求地址而忽略其它地址即可，而且性能是一样。
//因此被httphandlerBinder替代
@CjService(name = "wsHandle", scope = Scope.multiton)
@Deprecated
public class WsHandlerBinder extends
		SimpleChannelInboundHandler<TextWebSocketFrame> implements
		IHandleBinder ,NetConstans{
	Logger log = LoggerFactory.getLogger(WsHandlerBinder.class);
	private String serverName;
	IPin acceptpin;
	IPin senderIn;
	String path;
	SenderFrameHelper helper;
	@Override
	public void init(INettyGraph g) {
		this.acceptpin = g.newAcceptorPin();
		this.senderIn = g.in("senderIn");
		helper=new SenderFrameHelper();
	}
	public WsHandlerBinder(String serverName,String path) {
		this.serverName = serverName;
		this.path=path;
	}

	@Override
	public void channelRead(ChannelHandlerContext arg0, Object arg1)
			throws Exception {
		// TODO Auto-generated method stub
		super.channelRead(arg0, arg1);
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		flowWebsocketOpened(ctx);
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		flowWebsocketClosed(ctx);
		super.channelInactive(ctx);
	}

	private void initCircuit(Circuit circuit, ChannelHandlerContext ctx,
			String type) {
		circuit.attribute("transfer-protocol","net/1.1");
		circuit.attribute("select-type","server");
		circuit.attribute("local-address", ctx.channel().localAddress()
				.toString());
		circuit.attribute("remote-address", ctx.channel().remoteAddress()
				.toString());
		circuit.attribute("websocket-path", path);
		circuit.attribute("select-name", serverName);
		circuit.attribute("select-simple", type);
		circuit.attribute("select-id", ctx.channel().id().asShortText());
		circuit.attribute("isCompatibleHttp", "false");
	}

	private void flowWebsocketOpened(ChannelHandlerContext ctx) {
		Frame frame = new Frame("connect / net/1.1");
		Circuit circuit = new Circuit("net/1.1 200 ok");
		initCircuit(circuit, ctx, "ws");
		try {
			circuit.attribute("ctx", ctx);
			senderIn.flow(frame, circuit);
			circuit.removeAttribute("ctx");

			acceptpin.flow(frame, circuit);
			frame.dispose();
		} catch (Exception e) {
			CircuitException.print(e, log);
			throw CircuitException.throwRuntimeIt(e, null);
		}

	}

	private void flowWebsocketClosed(ChannelHandlerContext ctx) {
		Frame frame = new Frame("disconnect / net/1.1");
		Circuit circuit = new Circuit("net/1.1 200 ok");
		initCircuit(circuit, ctx, "ws");
		try {
			circuit.attribute("ctx", ctx);
			senderIn.flow(frame, circuit);
			circuit.removeAttribute("ctx");

			acceptpin.flow(frame, circuit);
			frame.dispose();
		} catch (Exception e) {
			CircuitException.print(e, log);
			throw CircuitException.throwRuntimeIt(e, null);
		}

	}

	@Override
	protected void messageReceived(ChannelHandlerContext ctx,
			TextWebSocketFrame f) throws Exception {
		Circuit circuit = new Circuit("http/1.1 200 ok");
		try {
			Frame frame = new Frame("flow "+path+" http/1.1");
			frame.content().writeBytes(f.content());
			initCircuit(circuit, ctx, "ws");
			String frameId=frame.head(NetConstans.FRAME_HEADKEY_FRAME_ID);
			if (frame.containsHead(NetConstans.FRAME_HEADKEY_FRAME_ID)) {
				//用于通知下游的net对该侦采用同步模式发送。如果开发者在链路中去除了该属性，则源头的调用者将等待超时。因为后续没有同步返回结果。
				frame.head(FRAME_HEADKEY_CIRCUIT_SYNC,"true");
				circuit.piggybacking(true);
				AttributeKey<String> key = AttributeKey
						.valueOf(NetConstans.FRAME_HEADKEY_FRAME_ID);
				ctx.attr(key).set(frameId);
				//侦的编号对开发者不可见。
				frame.removeHead(FRAME_HEADKEY_FRAME_ID);
			}
			acceptpin.flow(frame, circuit);
			if (circuit.isPiggybacking()) {// 捎带信息
				Frame back = null;
				boolean isunwrap=false;
				if (circuit.containsContentType()) {
					String v = circuit.contentType();
					if ("frame/bin".equals(v) || "frame/json".equals(v)) {
						isunwrap=true;
					}
				}
				if(isunwrap){
					back = new Frame(circuit.content().readFully());
				}else{
					back = circuit.snapshot("piggyback / net/1.1");
				}
				back.head(NetConstans.FRAME_HEADKEY_FRAME_ID, frameId);
				helper.sendWsMsg(ctx.channel(), back);
			}
			frame.dispose();
			circuit.dispose();
		} catch (Exception e) {
			CircuitException.print(e, log);
			throw new RuntimeException(e);
		} finally {
			circuit.dispose();
		}

	}
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		errorCaught(ctx, cause);
	}
	protected void errorCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		
		Throwable e = null;
		e = CircuitException.search(cause);
		if (e == null) {
			e = cause;
		}
		if (e instanceof CircuitException) {
			TextWebSocketFrame f=new TextWebSocketFrame();
			CircuitException ce=(CircuitException)e;
			f.content().writeBytes(ce.messageCause().getBytes());
			ctx.writeAndFlush(f);
		} else {
			TextWebSocketFrame f=new TextWebSocketFrame();
			f.content().writeBytes(("503 "+e.getMessage()).getBytes());
			ctx.writeAndFlush(f);
		}
	}
}
