package cj.studio.ecm.net.rio.http;

import java.net.HttpURLConnection;

import cj.studio.ecm.net.nio.netty.http.HttpException;

/**
 * 表示一个http信道
 * 
 * <pre>
 * 1.它模拟浏览器的一个进程，一个浏览器进程访问一个网站。
 * 2.它包装apache组件
 * </pre>
 * 
 * @author carocean
 *
 */
public interface IHttpChannel {
	String channelId();

	int port();

	String host();

	HttpURLConnection open(String host, int port, String contentPath) throws HttpException;


	String contentPath();

	HttpURLConnection open(String method,String relativeurl) throws HttpException;
}
