package cj.studio.ecm.net.rio.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import cj.studio.ecm.net.nio.netty.http.HttpException;

 class JdkHttpChannel implements IHttpChannel {

	private int port;
	private String host;
	private String contentPath;

	public JdkHttpChannel() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String channelId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int port() {
		// TODO Auto-generated method stub
		return port;
	}

	@Override
	public String host() {
		// TODO Auto-generated method stub
		return host;
	}
	@Override
	public HttpURLConnection open(String host, int port, String contentPath)
			throws HttpException {
		try {
			if(!contentPath.startsWith("/"))
			{
				contentPath=String.format("/%s", contentPath);
			}
			String url = String.format("%s:%s%s", host, port, contentPath);
			if(!url.startsWith("http:")&&!url.startsWith("https:")){
				url=String.format("http://%s", url);
			}
			HttpURLConnection conn = (HttpURLConnection) new URL(url)
					.openConnection();
			conn.connect();
			this.host = host;
			this.port = port;
			this.contentPath = contentPath;
			return conn;
		} catch (IOException e) {
			throw new HttpException(e);
		}
	}

	@Override
	public String contentPath() {
		// TODO Auto-generated method stub
		return contentPath;
	}

	@Override
	public HttpURLConnection open(String method,String relativeurl) throws HttpException {
		if(!relativeurl.startsWith("/")){
			relativeurl=String.format("/%s", relativeurl);
		}
		try {
			relativeurl=String.format("%s%s", contentPath,"/".equals(relativeurl)?"":relativeurl).replace("//", "/");
			String url = String.format("%s:%s%s", host, port, relativeurl);
			if(!url.startsWith("http:")&&!url.startsWith("https:")){
				url=String.format("http://%s", url);
			}
			HttpURLConnection conn = (HttpURLConnection) new URL(url)
					.openConnection();
			/*
			 *可不得了，必须注释掉，如果设为0，服务器的setContentLength万一与侦的大小不同，则造成未读取完的悬停状态，
			 *这会导致所有后续请求均被堵死，堵死到哪，可能把connectMoniter堵死了，不论信道中开启了多少个http连接也不论有多少个信道
			 *因此该http连接不是最优实现，它会是大并发环境下的平经，将来上线时可以实现两套方案，一套是基于netty http client的异步实现
			 *一套是基于apache httpclient的异步实现，采用syncheckSink的线程池机制 
			 *目前开发能用就行了，或者正式生产环境不用它来作前置http请求的转发，而采用tcp,udt等协议，只需将普通侦转换为httpframe即可。这种方案更靠谱
			 */
//			conn.setConnectTimeout(0);
//			conn.setReadTimeout(0);
			conn.setDefaultUseCaches(true);
			conn.setRequestMethod(method.toUpperCase());// 提交模式
			conn.setDoOutput(true);// 是否输入参数
			return conn;
		} catch (IOException e) {
			throw new HttpException(e);
		}

	}

}
