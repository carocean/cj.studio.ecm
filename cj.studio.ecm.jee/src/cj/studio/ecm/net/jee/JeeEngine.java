package cj.studio.ecm.net.jee;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cj.studio.ecm.Assembly;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.ServiceCollection;
import cj.ultimate.util.StringUtil;

/**
 * j2ee引擎api
 * 
 * <pre>
 * 用于发现地址服务，并调度请求到相应的地址服务上。
 * </pre>
 * 
 * @author carocean
 *
 */
public class JeeEngine extends HttpServlet {
	private IJeeProgram program;
	private static final long serialVersionUID = 1L;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		String home = config.getInitParameter("assembly.home");
		home=super.getServletContext().getRealPath(home);
		if (StringUtil.isEmpty(home)) {
			home="/WEB-INF/assembly";
		}
		scanAssemblyAndLoad(home);
		program.init(config,home);
		
	}

	private void scanAssemblyAndLoad(String home) {
		File dir=new File(home);
		File[] assemblies=dir.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".jar");
			}
		});
		if(assemblies.length==0){
			throw new EcmException("缺少程序集:"+home);
		}
		if(assemblies.length>1){
			throw new EcmException("定义了多个程序集:"+home);
		}
		String fn=assemblies[0].getAbsolutePath();
		Assembly root = Assembly.loadAssembly(fn);
		root.start();
		ServiceCollection<JeeProgram> col = root.workbin()
				.part(JeeProgram.class);
		if (col.isEmpty()) {
			throw new EcmException("程序集验证失败，原因：未发现JeeProgram的派生实现");
		}
		if (col.size() > 1) {
			throw new EcmException("程序集验证失败，原因：多处定义JeeProgram的派生实现");
		}
		program = col.get(0);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		JeeHttpRequest request = new JeeHttpRequest(req);
		JeeHttpResponse response = new JeeHttpResponse(resp);
		program.doGet(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		JeeHttpRequest request = new JeeHttpRequest(req);
		JeeHttpResponse response = new JeeHttpResponse(resp);
		program.doPost(request, response);
	}

	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		JeeHttpRequest request = new JeeHttpRequest(req);
		JeeHttpResponse response = new JeeHttpResponse(resp);
		program.doOptions(request, response);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		JeeHttpRequest request = new JeeHttpRequest(req);
		JeeHttpResponse response = new JeeHttpResponse(resp);
		program.doPut(request, response);
	}

	@Override
	protected void doTrace(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		JeeHttpRequest request = new JeeHttpRequest(req);
		JeeHttpResponse response = new JeeHttpResponse(resp);
		program.doTrace(request, response);
	}
}
