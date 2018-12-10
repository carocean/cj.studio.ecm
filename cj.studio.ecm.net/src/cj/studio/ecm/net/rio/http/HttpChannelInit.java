package cj.studio.ecm.net.rio.http;

import cj.studio.ecm.net.nio.netty.http.CjHttpContentCompressor;
import cj.studio.ecm.net.nio.netty.http.CjHttpObjectAggregator;
import cj.studio.ecm.net.nio.netty.http.CjHttpResponseEncoder;
import cj.ultimate.util.StringUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;

public class HttpChannelInit extends ChannelInitializer<SocketChannel> {

	private HttpCjNetServer server;

	public HttpChannelInit(HttpCjNetServer server) {
		this.server = server;
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
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
		String uploadsite = server.getProperty("uploadsite");
		if (StringUtil.isEmpty(uploadsite)) {
			uploadsite = "/uploadsite";
		}
		if (!uploadsite.startsWith("/")) {
			uploadsite = String.format("/%s", uploadsite);
		}
		String propMaxFrameContentLength = server
				.getProperty("maxFrameContentLength");
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
		// 块写入处理器 ，块处理是否与aggregator处理冲突，之后再研究对块的支持
		SimpleChannelInboundHandler<Object> hb = new HttpServerHandler(server,
				server.getProperty("isCompatibleHttp"),
				server.getProperty(("wsPath")));
		pipeline.addLast("httpHandler", hb);

	}

}
