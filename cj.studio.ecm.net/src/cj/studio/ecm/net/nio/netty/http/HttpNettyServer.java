package cj.studio.ecm.net.nio.netty.http;

import cj.studio.ecm.graph.AnnotationProtocolFactory;
import cj.studio.ecm.graph.GraphCreator;
import cj.studio.ecm.graph.IProtocolFactory;
import cj.studio.ecm.graph.ISink;
import cj.studio.ecm.net.graph.IHandleBinder;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.nio.netty.IChannelInitializer;
import cj.studio.ecm.net.nio.netty.NettyServer;
import cj.ultimate.util.StringUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;

/*
 *如果要实现文件上下传功能，可参考netty5.0官方示例 ，客户端可以是浏览器，也可为自己开发的客户端代码。
 * @author cj
 *
 */
public class HttpNettyServer extends NettyServer<SocketChannel> {
	
	@Override
	public String simple() {
		// TODO Auto-generated method stub
		return "http";
	}
	
	public EventLoopGroup getGroup(){
		return super.workerGroup;
	}
	@Override
	protected IChannelInitializer<SocketChannel> createChildChannel() {
		return new NioServerInitializer(props.get("isCompatibleHttp"),
				props.get("wsPath"));
	}

	@Override
	protected Class<? extends ServerChannel> getParentChannelClass() {
		return NioServerSocketChannel.class;
	}

	@Override
	protected void initPorperties(ServerBootstrap b) {
		super.initPorperties(b);
		String SO_KEEPALIVE = props.get("child.SO_KEEPALIVE");
		if (StringUtil.isEmpty(SO_KEEPALIVE))
			SO_KEEPALIVE = "true";
		b.childOption(ChannelOption.SO_KEEPALIVE,
				Boolean.valueOf(SO_KEEPALIVE));
		// String cname = props.get("connector");
		// client=null;
		// if(!StringUtil.isEmpty(cname)){
		// client=(NettyClient<?>)ClientManager.activeConnector(cname);
		//
		// }
	}

	@Override
	protected GraphCreator createNetGraphCreator() {
		return new GraphCreator() {
			@Override
			protected IProtocolFactory newProtocol() {
				// TODO Auto-generated method stub
				return AnnotationProtocolFactory.factory(NetConstans.class);
			}

			@Override
			public ISink createSink(String sink) {
				if ("acceptor".equals(sink)) {
					return new HttpServerAcceptor();
				}
				if ("sender".equals(sink)) {
					return new HttpServerSender();
				}
				if ("diverter".equals(sink)) {
					return new HttpServerDiverter();
				}
				return null;
			}
		};
	}

	class NioServerInitializer implements IChannelInitializer<SocketChannel> {
		boolean isCompatibleHttp;
		String wsPath;

		public NioServerInitializer(String isCompatibleHttp, String wsPath) {
			this.isCompatibleHttp = StringUtil.isEmpty(isCompatibleHttp) ? true
					: Boolean.parseBoolean(isCompatibleHttp);
			this.wsPath = StringUtil.isEmpty(wsPath) ? "/wssite" : wsPath;
		}

		// netty中有两种实现websocket的方式，一种是兼容http协议方式（因为要处理http请求的html页面）；一种是不兼容http，直接就是用websocket。如果不需要兼容，最好用第二种，因为它只处理websocket，所以性能好。

		// 将来ssl的实现可参见netty例程，在source包中
		@Override
		public IHandleBinder initChannel(SocketChannel ch, long idleCheckin)
				throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			// ch.pipeline().addLast(new ReadTimeoutHandler(10));
			// ch.pipeline().addLast(new WriteTimeoutHandler(1));
			// System.out.println("ch....." + ch);
			// if (HttpUploadServer.isSSL) {
			// SSLEngine engine =
			// SecureChatSslContextFactory.getServerContext().createSSLEngine();
			// engine.setUseClientMode(false);
			// pipeline.addLast("ssl", new SslHandler(engine));
			// }
			IHandleBinder hb = null;
			// *******if后面的被该段替代。
			pipeline.addLast("decoder", new HttpRequestDecoder());
			/*
			 * HttpObjectAggregator为什么叫聚合，因为：一次浏览器请求，会多次提交到服务器，如http协议中的chunked。
			 * 而在服务器上对一个请求会生成多次事件，每次事件的消息可能不同，因此netty通过这个类将多次处理事件聚合为一个请求对象，即：DefaultFullHttpRequest
			 * 因此，如果文件太大，会占用内存。netty在架构上实际上应设立一个callback调用，将正在处理的事件留给程序实现。
			 * 看servlet2.4规范，各种容器应该都是类似于HttpObjectAggregator的实现机制，因此均有文件最大大小限制，也都是内存中合并处理事件。
			 * 因此：在文件服务器上，如果非要采用http方式上传文件，应去掉HttpObjectAggregator而采用HttpHandlerBinder中的HttpPostRequestDecoder进行多次事件的处理，从而能无限制文件大小
			 *
			 * uploadsite是上传地址
			 * 为了实现大文件上传，原理是这样：
			 * 在聚合类中如果是多formdata类型的请求，且前段地址是指定的uploadsite地址，则启用大文件上传机制
			 * 1.该机制是在聚合类中放过原请求和后续块，将之全改造为UploadSiteHttpRequest请求，并编号：第0号是原请求，之后是块请求
			 * 2.在httpHandleBinder类中还按对httprequest的正常请求一样将之流入website或其它网络
			 * 3.在接受端的website中根据请求地址和编号重新组装（注意：在多层网络间转发其顺序不一定有连续性）
			 * 4.在接受端将之转化为httprequest和httpContent对象，即可使用netty的HttpPostRequestDecoder对之进行解码并持化到目标机磁盘或其它地方
			 * 5.例子：http前置->tcpClient->tcpSite->website 该方案才能解决这样的需求。
			 * 6.如果让httpSite直接支持netty的大文件机制，则只能在前置机收下文件，而后做个程序将文件再转存，这样做也有好处，可以做专门的文件服务器
			 *   但缺点是与应用逻辑不搭界了，在做一个应用存储数据时，还得去改变或添加在文件服务器上的转存逻辑
			 *   但这种方案适合做专门的http文件服务器，因此在开发网盘时采用此方案，需要为之专门做一个httpServer实现
			 *   
			 * netty大文件的用法：
			 * 主要是HttpPostRequestDecoder.decode(httpContent)即可，在请求中建立此对象，绑定httprequest和一个数据工厂，在netty的uploadfile例程中很明白，实际上两行代码即可完成。
			 */
			// MyHttpObjectAggregator
			String uploadsite = getProperty("uploadsite");
			if (StringUtil.isEmpty(uploadsite)) {
				uploadsite = "/uploadsite";
			}
			if (!uploadsite.startsWith("/")) {
				uploadsite = String.format("/%s", uploadsite);
			}
			String propMaxFrameContentLength = getProperty(
					"maxFrameContentLength");
			int maxlength = 60485760;// 2097152约定上传最大2m大小，此类将生成DefaultFullHttpRequest对象
			if (!StringUtil.isEmpty(propMaxFrameContentLength)) {
				maxlength = Integer.valueOf(propMaxFrameContentLength);
			}
			// log.info(String.format("aggregatorMode：%s，tooLongFrameFound：%s
			// 字节", mode,maxlength));
			pipeline.addLast("aggregator",
					new CjHttpObjectAggregator(maxlength, uploadsite));
			pipeline.addLast("encoder", new CjHttpResponseEncoder());
			pipeline.addLast("deflater", new CjHttpContentCompressor());// 响应时的内容压缩
			// pipeline.addLast("codec-http", new HttpServerCodec());

			// 块写入处理器 ，块处理是否与aggregator处理冲突，之后再研究对块的支持
			// pipeline.addLast("http-chunked", new ChunkedWriteHandler());
			hb = new HttpHandlerBinder(buildNetGraph().name(),
					isCompatibleHttp);
			((HttpHandlerBinder) hb).setWsPath(props.get("wsPath"));
			pipeline.addLast("httpHandler", hb);
			// ************end.
			// if (isCompatibleHttp) {//
			// 本例是第一种实现。此种实现即可向客户端写html文件，也可处理websocket请求。要了解详情可参见netty
			// // simples例程。
			// pipeline.addLast("codec-http", new HttpServerCodec());
			// pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
			// // ch.pipeline().addLast("chunkedWriter", new
			// // ChunkedWriteHandler());
			// // a WebSocket handshake request: missing upgrade
			// //一次请求http页面时且在该页面中调用了websocket，会导致两次请求处理，一次是http，一次是websocket，因此会建立两个不同的信道，
			// //如果这两个信道使用了同一个handler的实例，将会导致websocket信道关闭或紊乱
			// hb=new HttpHandlerBinder(getNetGraph().name());
			// ((HttpHandlerBinder)hb).setWsPath(props.get("wsPath"));
			// pipeline.addLast("httpHandler", hb);
			// } else {// 本例是第二种实现。服务器端无法向客户端写html文件，因为它不兼容http处理。要了解详情可参见netty
			// // simples例程。
			// // 这种方式，必须客户端有现成的html文件，以浏览器打开，通过文件里的js发起websocket请求。
			// // 使用这种模式如果请求非websocket地址，浏览器报：not a WebSocket handshake
			// // request: missing upgrade
			// pipeline.addLast(new HttpRequestDecoder());
			//
			// pipeline.addLast(new HttpObjectAggregator(65536));// 最大内容长度
			// pipeline.addLast(new HttpResponseEncoder());
			// // Remove the following line if you don't want automatic content
			// // compression.
			// // pipeline.addLast(new HttpContentCompressor());
			// //兼容与非兼容其实netty在底层都是调用握手的那几个类，只是在不兼容http模式下，netty将请求封装成了handler，这等同于只要将兼容模式的请求处于只用于处理ws://请求地址而忽略其它地址即可，而且性能是一样。
			// pipeline.addLast(new
			// WebSocketServerProtocolHandler(props.get("wsPath")));//WebSocketServerProtocolHandler应跟据源码派生并重新实现，以取得会话cookie
			// // WsHandle wsHandle=(WsHandle)site.getService("wsHandle");
			// hb=new WsHandlerBinder(getNetGraph().name(),props.get("wsPath"));
			// pipeline.addLast("handler",hb);
			// // System.out.println("............................");
			// }
			return hb;

		}
	}
}
