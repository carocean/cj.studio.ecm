package cj.studio.ecm.pin;

import org.apache.log4j.PropertyConfigurator;

import cj.studio.ecm.net.nio.netty.udt.UdtNettyClient;

public class TestUdt {

	public static void main(String[] args) {
		PropertyConfigurator.configure("/Users/carocean/studio/lns/cj.studio.ecm.corelib/src.test/cj/studio/ecm/pin/log4j.properties");
		// TODO Auto-generated method stub
		UdtNettyClient c=new UdtNettyClient();
		try {
			c.connect("192.168.0.1", "9098");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
