package cj.studio.ecm.net.nio.netty;

import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cj.studio.ecm.net.graph.IHandleBinder;
import cj.studio.ecm.net.nio.BaseServerNIO;
import cj.ultimate.util.StringUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.bootstrap.ServerChannelFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.ThreadPerTaskExecutor;

public abstract class NettyServer<C extends Channel> extends BaseServerNIO {
	protected EventLoopGroup bossGroup;
	protected EventLoopGroup workerGroup;

	protected Logger log = LoggerFactory.getLogger(this.getClass());

	@Override
	protected void startServer() {
		// channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
		String bossThreadCount = props.get("bossThreadCount");
		if (StringUtil.isEmpty(bossThreadCount)) {
			bossThreadCount = "1";
			props.put("bossThreadCount", bossThreadCount);
		}
		String workThreadCount = props.get("workThreadCount");
		if (StringUtil.isEmpty(workThreadCount)) {
			workThreadCount = "40";
			props.put("workThreadCount", workThreadCount);
		}

		bossGroup = createBossGroup(bossThreadCount);
		workerGroup = createWorkGroup(workThreadCount);
		ServerBootstrap b = new ServerBootstrap(); // (2)
		Class<? extends ServerChannel> chclass = getParentChannelClass();
		if (chclass != null
				&& LocalServerChannel.class.isAssignableFrom(chclass)) {
			b.group(bossGroup);
		} else {
			b.group(bossGroup, workerGroup);
		}
		ServerChannelFactory<?> factory = getServerChannelFactory();
		if (factory != null)
			b.channelFactory(factory);
		if (!StringUtil.isEmpty(props.get("log"))) {
			b.handler(new LoggingHandler(LogLevel.valueOf(props.get("log"))));
		}

		if (chclass != null)
			b.channel(chclass);
		IChannelInitializer<C> ci = createChildChannel();
		DefaultChannelInitializer<C> dci = new DefaultChannelInitializer<C>(ci);
		b.childHandler(dci);

		initPorperties(b);
		// Bind and start to accept incoming connections.
		ChannelFuture f;
		try {
			if (chclass != null
					&& LocalServerChannel.class.isAssignableFrom(chclass)) {
				LocalAddress address = new LocalAddress(getPort());
				f = b.bind(address)/*.sync()*/;
			} else {
				if (StringUtil.isEmpty(getINetHost())) {
					f = b.bind(Integer.valueOf(getPort()));
				} else {
					f = b.bind(getINetHost(), Integer.valueOf(getPort()));
				}
			}
			f.channel().closeFuture()/*.sync()*/;
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new RuntimeException(e);
		} // (7)
	}

	protected void errorCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		throw new Exception(cause);
	}

	@Override
	protected void stopServer() {
		workerGroup.shutdownGracefully();
		bossGroup.shutdownGracefully();
		// channels.clear();
		// channels = null;
	}

	protected NioEventLoopGroup createBossGroup(String threadCount) {
		return new NioEventLoopGroup(Integer.valueOf(threadCount)); // (1)
	}

	protected NioEventLoopGroup createWorkGroup(String threadCount) {
		ThreadFactory factory=new DefaultThreadFactory(getClass());
		ThreadPerTaskExecutor executor = new ThreadPerTaskExecutor(factory);
		
		return new NioEventLoopGroup(Integer.valueOf(threadCount),executor); // (1)
	}

	protected ServerChannelFactory<? extends ServerChannel> getServerChannelFactory() {
		return null;
	}

	/**
	 * 用于配置信道的属性。
	 * 
	 * <pre>
	 * netty信道分为父信道和子信道，父为boss线程管理的用于接受客户端连接请求，子为每个客户端连接请求对应的信道
	 * 对于其它net服务器，没有所谓的子信道，可视为父信道或主信道即可。
	 * </pre>
	 * 
	 * @param b
	 */
	protected void initPorperties(ServerBootstrap b) {
		String SO_BACKLOG = props.get("parent.SO_BACKLOG");
		if (StringUtil.isEmpty(SO_BACKLOG))
			SO_BACKLOG = "1024";
		b.option(ChannelOption.SO_BACKLOG, Integer.valueOf(SO_BACKLOG));
	}

	protected abstract Class<? extends ServerChannel> getParentChannelClass();

	/**
	 * 创建子channel，派生类可覆盖
	 * 
	 * @return
	 */
	protected abstract IChannelInitializer<C> createChildChannel();

	class DefaultChannelInitializer<T extends Channel>
			extends ChannelInitializer<T> {
		IChannelInitializer<T> initializer;

		public DefaultChannelInitializer(IChannelInitializer<T> initializer) {
			this.initializer = initializer;
		}

		@Override
		protected void initChannel(T ch) throws Exception {
			try {
				IHandleBinder hb = initializer.initChannel(ch, -1);
				if (hb == null)
					return;
				hb.init((INettyGraph) buildNetGraph());
			} catch (Exception e) {
				log.error(e.getMessage());
				throw e;
			}
		}
	}
}
