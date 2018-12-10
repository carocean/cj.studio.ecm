package cj.studio.ecm.net.nio.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.net.nio.BaseClientNIO;

public abstract class NettyClient<C extends Channel> extends BaseClientNIO<C> {
	@Override
	protected Channel connectServer(Bootstrap b,Class<? extends Channel> chclass)
			throws NumberFormatException, InterruptedException {
		//基类提供Bootstrap，当前类中可多次执行连接，连接一次产生一个graph
		try{
		// 连接服务端
		Channel ch =null;
		if(chclass!=null&&chclass.isAssignableFrom(LocalChannel.class)){
			LocalAddress addr = new LocalAddress(getPort());
			ch=b.connect(addr).sync().channel();
//			ch= b.connect(addr).sync().channel();
		}else{
//			b.connect(getHost(), Integer.valueOf(getPort()));
			ch= b.connect(getHost(), Integer.valueOf(getPort())).sync().channel();
		}
		return ch;
//		cluster.add(ch);//在此不必添加信道,因为b.connect每次连接建立的信道与handler句柄中的信道是同一信道，且会被句柄捕获，因此不在此处设
		}catch(Exception e){
			CJSystem.current().environment().logging().error(getClass(),e.getMessage());
			throw new EcmException(e);
		}
	}
	

	
}
