/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package cj.studio.ecm.net.nio.netty.http;

import static io.netty.handler.codec.http.HttpHeaders.is100ContinueExpected;
import static io.netty.handler.codec.http.HttpHeaders.removeTransferEncodingChunked;

import java.util.List;

import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpObjectDecoder;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.util.ReferenceCountUtil;

/**
 * A {@link ChannelHandler} that aggregates an {@link HttpMessage} and its
 * following {@link HttpContent}s into a single {@link HttpMessage} with no
 * following {@link HttpContent}s. It is useful when you don't want to take care
 * of HTTP messages whose transfer encoding is 'chunked'. Insert this handler
 * after {@link HttpObjectDecoder} in the {@link ChannelPipeline}:
 * 
 * <pre>
 * {@link ChannelPipeline} p = ...;
 * ...
 * p.addLast("decoder", new {@link HttpRequestDecoder}());
 * p.addLast("aggregator", <b>new {@link HttpObjectAggregator}(1048576)</b>);
 * ...
 * p.addLast("encoder", new {@link HttpResponseEncoder}());
 * p.addLast("handler", new HttpRequestHandler());
 * </pre>
 */
public class CjHttpObjectAggregator
		extends MessageToMessageDecoder<HttpObject> {
	public static final int DEFAULT_MAX_COMPOSITEBUFFER_COMPONENTS = 1024;
	private static final FullHttpResponse CONTINUE = new DefaultFullHttpResponse(
			HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE,
			Unpooled.EMPTY_BUFFER);

	private final int maxContentLength;
	private FullHttpMessage currentMessage;
	private boolean tooLongFrameFound;

	private int maxCumulationBufferComponents = DEFAULT_MAX_COMPOSITEBUFFER_COMPONENTS;
	private ChannelHandlerContext ctx;
	private String uploadsite;

	/**
	 * Creates a new instance.
	 *
	 * @param maxContentLength
	 *            the maximum length of the aggregated content. If the length of
	 *            the aggregated content exceeds this value, a
	 *            {@link TooLongFrameException} will be raised.
	 */
	public CjHttpObjectAggregator(int maxContentLength, String uploadsite) {
		if (maxContentLength <= 0) {
			throw new IllegalArgumentException(
					"maxContentLength must be a positive integer: "
							+ maxContentLength);
		}
		this.maxContentLength = maxContentLength;
		this.uploadsite = uploadsite;
	}

	private String contentPath(String uri) {
		String path = uri.startsWith("/") ? uri : String.format("/%s" , uri);
		if (path.length() == 1)
			return path;
		int nextSp = path.indexOf("/", 1);
		if (nextSp < 0) {
			return path;
		}
		path = path.substring(0, nextSp);
		return path;
	}

	public boolean isUploadSite(HttpRequest req) {
		// 上下文
		String content = contentPath(req.getUri());
		String usite = String.format("%s%s", content, uploadsite);
		if (usite.contains("//")) {
			usite = usite.replace("//", "/");
		}
		return HttpPostRequestDecoder.isMultipart(req)
				&& req.getUri().startsWith(usite);
	}

	/**
	 * Returns the maximum number of components in the cumulation buffer. If the
	 * number of the components in the cumulation buffer exceeds this value, the
	 * components of the cumulation buffer are consolidated into a single
	 * component, involving memory copies. The default value of this property is
	 * {@link #DEFAULT_MAX_COMPOSITEBUFFER_COMPONENTS}.
	 */
	public final int getMaxCumulationBufferComponents() {
		return maxCumulationBufferComponents;
	}

	/**
	 * Sets the maximum number of components in the cumulation buffer. If the
	 * number of the components in the cumulation buffer exceeds this value, the
	 * components of the cumulation buffer are consolidated into a single
	 * component, involving memory copies. The default value of this property is
	 * {@link #DEFAULT_MAX_COMPOSITEBUFFER_COMPONENTS} and its minimum allowed
	 * value is {@code 2}.
	 */
	public final void setMaxCumulationBufferComponents(
			int maxCumulationBufferComponents) {
		if (maxCumulationBufferComponents < 2) {
			throw new IllegalArgumentException("maxCumulationBufferComponents: "
					+ maxCumulationBufferComponents + " (expected: >= 2)");
		}

		if (ctx == null) {
			this.maxCumulationBufferComponents = maxCumulationBufferComponents;
		} else {
			throw new IllegalStateException(
					"decoder properties cannot be changed once the decoder is added to a pipeline.");
		}
	}

	@Override
	protected void decode(final ChannelHandlerContext ctx, HttpObject msg,
			List<Object> out) throws Exception {
		FullHttpMessage currentMessage = this.currentMessage;

		if (msg instanceof HttpMessage) {
			tooLongFrameFound = false;
			assert currentMessage == null;

			HttpMessage m = (HttpMessage) msg;

			// Handle the 'Expect: 100-continue' header if necessary.
			// TODO: Respond with 413 Request Entity Too Large
			// and discard the traffic or close the connection.
			// No need to notify the upstream handlers - just log.
			// If decoding a response, just throw an exception.
			if (is100ContinueExpected(m)) {
				ctx.writeAndFlush(CONTINUE)
						.addListener(new ChannelFutureListener() {
							@Override
							public void operationComplete(ChannelFuture future)
									throws Exception {
								if (!future.isSuccess()) {
									ctx.fireExceptionCaught(future.cause());
								}
							}
						});
			}

			if (!m.getDecoderResult().isSuccess()) {
				removeTransferEncodingChunked(m);
				this.currentMessage = null;
				out.add(ReferenceCountUtil.retain(m));
				return;
			}
			if (msg instanceof HttpRequest) {
				HttpRequest header = (HttpRequest) msg;
				if (isUploadSite(header)) {
					this.currentMessage = currentMessage = new UploadSiteHttpRequest(
							header.getProtocolVersion(), header.getMethod(),
							header.getUri(), Unpooled.compositeBuffer(
									maxCumulationBufferComponents));
				} else {
					this.currentMessage = currentMessage = new DefaultFullHttpRequest(
							header.getProtocolVersion(), header.getMethod(),
							header.getUri(), Unpooled.compositeBuffer(
									maxCumulationBufferComponents));
				}

			} else if (msg instanceof HttpResponse) {
				HttpResponse header = (HttpResponse) msg;
				this.currentMessage = currentMessage = new DefaultFullHttpResponse(
						header.getProtocolVersion(), header.getStatus(),
						Unpooled.compositeBuffer(
								maxCumulationBufferComponents));
			} else {
				throw new Error();
			}

			currentMessage.headers().set(m.headers());
			// 如果是上传请求，则不聚合，包括后续块都不聚合，这样做的好处是：
			// 在网络中到处转发请求时，如果在此处聚合了，比如采用netty的HttpPostRequestDecoder
			// 则数据保存到了本地磁盘，则在处理端接收不到，如果再在本地磁盘上发起这个文件，势必影晌性能
			// 注意：由于要保持上传请求，后面的removeTransferEncodingChunked方法不得执行
			if (currentMessage instanceof UploadSiteHttpRequest) {
				out.add(currentMessage);
				return;
			}

			// A streamed message - initialize the cumulative buffer, and wait
			// for incoming chunks.
			removeTransferEncodingChunked(currentMessage);
		} else if (msg instanceof HttpContent) {
			if (currentMessage instanceof UploadSiteHttpRequest) {
				CompositeByteBuf compositeBuffer=Unpooled.compositeBuffer(
						maxCumulationBufferComponents);
				HttpContent cnt=(HttpContent)msg;
				compositeBuffer.addComponent(cnt.content());
				UploadSiteHttpRequest req = new UploadSiteHttpRequest(
						currentMessage.getProtocolVersion(),
						((UploadSiteHttpRequest) currentMessage).getMethod(),
						((UploadSiteHttpRequest) currentMessage).getUri(), compositeBuffer);
				
				req.headers().set(HttpHeaders.Names.CONTENT_LENGTH,
						String.valueOf(compositeBuffer.readableBytes()));
				req.headers().add("Decoder-Result-IsFailure", req.getDecoderResult().isFailure());
				req.headers().add("Decoder-Result-IsFinished", req.getDecoderResult().isFinished());
				req.headers().add("Decoder-Result-IsSuccess", req.getDecoderResult().isSuccess());
				req.headers().add("Is-Upload-Chunked", true);
				long msgNum=((UploadSiteHttpRequest) currentMessage).msgNum();
				msgNum++;
				((UploadSiteHttpRequest) currentMessage).msgNum(msgNum);
				req.headers().add("Is-Upload-Chunked-Num", msgNum);
				req.headers().add("Is-Upload-LastChunked", msg instanceof LastHttpContent);
				out.add(req);// 上传请求则将块放过去
				return;
			}
			if (tooLongFrameFound) {
				if (msg instanceof LastHttpContent) {
					this.currentMessage = null;
				}
				// already detect the too long frame so just discard the content
				return;
			}
			assert currentMessage != null;

			// Merge the received chunk into the content of the current message.
			HttpContent chunk = (HttpContent) msg;
			CompositeByteBuf content = (CompositeByteBuf) currentMessage
					.content();

			if (content.readableBytes() > maxContentLength
					- chunk.content().readableBytes()) {
				tooLongFrameFound = true;

				// release current message to prevent leaks
				currentMessage.release();
				this.currentMessage = null;

				throw new TooLongFrameException("HTTP content length exceeded "
						+ maxContentLength + " bytes.");
			}

			// Append the content of the chunk
			if (chunk.content().isReadable()) {
				chunk.retain();
				content.addComponent(chunk.content());
				content.writerIndex(content.writerIndex()
						+ chunk.content().readableBytes());
			}

			final boolean last;
			if (!chunk.getDecoderResult().isSuccess()) {
				currentMessage.setDecoderResult(DecoderResult
						.failure(chunk.getDecoderResult().cause()));
				last = true;
			} else {
				last = chunk instanceof LastHttpContent;
			}

			if (last) {
				this.currentMessage = null;

				// Merge trailing headers into the message.
				if (chunk instanceof LastHttpContent) {
					LastHttpContent trailer = (LastHttpContent) chunk;
					currentMessage.headers().add(trailer.trailingHeaders());
				}

				// Set the 'Content-Length' header.
				currentMessage.headers().set(HttpHeaders.Names.CONTENT_LENGTH,
						String.valueOf(content.readableBytes()));

				// All done
				out.add(currentMessage);
			}
		} else {
			throw new Error();
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);

		// release current message if it is not null as it may be a left-over
		if (currentMessage != null) {
			currentMessage.release();
			currentMessage = null;
		}
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		this.ctx = ctx;
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		super.handlerRemoved(ctx);
		// release current message if it is not null as it may be a left-over as
		// there is not much more we can do in
		// this case
		if (currentMessage != null) {
			if(currentMessage.refCnt()>0){
			currentMessage.release();
			}
			currentMessage = null;
		}
	}
}
