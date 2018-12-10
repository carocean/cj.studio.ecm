package cj.studio.ecm.net.switch0;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.graph.INetGraph;
import cj.studio.ecm.net.nio.BaseServerNIO;
import cj.ultimate.util.StringUtil;

/**
 * 交换机
 * 
 * <pre>
 * 用于在netsite中交换graph的请求，以请求中的协议作为导向
 * </pre>
 * 
 * @author carocean
 *
 */
public class Switch extends BaseServerNIO {

	public Switch() {
		// TODO Auto-generated constructor stub
	}


	@Override
	protected INetGraph createNetGraph() {
		CjService s = this.getClass().getAnnotation(
				CjService.class);
		String name="";
		if (s == null) {
			 name = this.getProperty("serverName");
			if (StringUtil.isEmpty(name))// 如果为空则以端口号作为名字
				name = this.simple() + "-"
						+ this.getProperty("port");
		}else{
			name=s.name();
		}
		INetGraph g= new SwitchGraph(name,super.props);
		
		return g;
	}

	@Override
	public String simple() {
		return "switch";
	}

	@Override
	protected void startServer() {
		// 交换器的端口号即端子名
//		getNetGraph().options("port",getPort());
	}

	@Override
	protected void stopServer() {
		buildNetGraph().dispose();
		
	}

	

}
