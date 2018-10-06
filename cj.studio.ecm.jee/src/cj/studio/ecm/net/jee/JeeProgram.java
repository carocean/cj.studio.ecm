package cj.studio.ecm.net.jee;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cj.studio.ecm.Assembly;
import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.IChip;
import cj.studio.ecm.IChipInfo;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceSite;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.net.web.WebUtil;
import cj.studio.ecm.net.web.page.context.HttpContext;
import cj.studio.ecm.script.IJssModule;
import cj.ultimate.gson2.com.google.gson.Gson;
import cj.ultimate.gson2.com.google.gson.JsonArray;
import cj.ultimate.gson2.com.google.gson.JsonElement;
import cj.ultimate.gson2.com.google.gson.JsonObject;
import cj.ultimate.util.FileHelper;
import cj.ultimate.util.StringUtil;

public abstract class JeeProgram implements IJeeProgram {
	ILogging logger;
	@CjServiceSite
	IServiceSite site;
	IJeeSiteResource resource;
	Map<String, IJeeProgram> plugins;// key=plugin assemblyTitle
	Map<String, IJeeWebView> mappings;// key=url=service name plus
	private Pipeline resourcePipeline;
	private Pipeline documentPipeline;
	private String documentType;
	private Map<String, String> errors;
	private Map<String,String> mimes;
	// method alias because it
	// is combined by path,if
	// the url is path,it is
	// must end of /
	protected final Pipeline documentpipeline() {
		return documentPipeline;
	}

	protected final Pipeline resourcePipeline() {
		return resourcePipeline;
	}

	@Override
	public void init(ServletConfig config, String home) {
		logger = CJSystem.current().environment().logging();
		mappings = new HashMap<>();
		plugins = new HashMap<>();
		mimes=new HashMap<>();
		errors = new HashMap<>();
		IChip chip = (IChip) site.getService(IChip.class.getName());
		IChipInfo info = chip.info();
		String http_root = String
				.format("%s/%s/%s", info.getProperty("home.dir"),
						IJssModule.RUNTIME_SITE_DIR,
						info.getResourceProp("http.root"))
				.replace("///", "/").replace("//", "/")
				.replace("/", File.separator);
		// SiteConfig sc=new SiteConfig();
		// sc.parse(info);
		parseErrors(info);
		parseMimes(info);
		documentPipeline = new Pipeline();
		// pipeline.add(new EncodeJeeHttpFilter());
		String pljson = info.getProperty("site.pipeline.document");
		if (!StringUtil.isEmpty(pljson)) {
			loadFilters(pljson, documentPipeline);
		}
		JeeHttpFilterWrapper docend = new JeeHttpFilterWrapper(
				Integer.MAX_VALUE, new DocumentEndFilter());
		documentPipeline.filters.add(docend);
		documentPipeline.sort();
		((Pipeline) documentPipeline).isLoaded = true;

		resourcePipeline = new Pipeline();
		String resjson = info.getProperty("site.pipeline.resource");
		if (!StringUtil.isEmpty(resjson)) {
			loadFilters(resjson, resourcePipeline);
		}
		JeeHttpFilterWrapper resend = new JeeHttpFilterWrapper(
				Integer.MAX_VALUE, new ResourceEndFilter());
		resourcePipeline.filters.add(resend);
		resourcePipeline.sort();
		((Pipeline) documentPipeline).isLoaded = true;

		resource = new JeeSiteResource(http_root);
		documentType = info.getProperty("site.document");
		indexMappings();
		scansAndLoadPlugins(config, info.getProperty("home.dir"), plugins);
	}

	private void parseMimes(IChipInfo info) {
		String[] keys = info.enumProperty();
		for (String key : keys) {
			int pos = key.indexOf("site.http.mime.");
			if (pos == 0) {
				String k = key.substring("site.http.mime.".length(),
						key.length());
				String v = info.getProperty(key);
				mimes.put(k, v);
			}
		}
	}

	private void parseErrors(IChipInfo info) {
		String[] keys = info.enumProperty();
		for (String key : keys) {
			int pos = key.indexOf("site.http.error.");
			if (pos == 0) {
				String k = key.substring("site.http.error.".length(),
						key.length());
				String v = info.getProperty(key);
				errors.put(k, v);
			}
		}
	}

	private void loadFilters(String json, IJeePipeline pipeline) {
		JsonElement je = new Gson().fromJson(json, JsonElement.class);
		if (!je.isJsonArray()) {
			logger.warn("pipeline不是数组,过滤器配置无效");
		} else {
			JsonArray arr = je.getAsJsonArray();
			for (JsonElement jo : arr) {
				if (!jo.isJsonObject()) {
					logger.warn("pipeline的过滤器项不是对象,过滤器配置无效:" + jo.toString());
					continue;
				}
				JsonObject jobj = jo.getAsJsonObject();
				String sort = jobj.get("sort") == null ? "0"
						: jobj.get("sort").getAsString();
				String clazz = jobj.get("class") == null ? "0"
						: jobj.get("class").getAsString();
				JeeHttpFilterWrapper w = new JeeHttpFilterWrapper(
						Integer.valueOf(sort), clazz);
				pipeline.add(w);
			}
		}
	}

	private void indexMappings() {
		ServiceCollection<IJeeWebView> col = site
				.getServices(IJeeWebView.class);
		for (IJeeWebView view : col) {
			if (view.getClass().getSimpleName()
					.endsWith("$$NashornJavaAdapter")) {
				continue;
			}
			CjService cs = view.getClass().getAnnotation(CjService.class);
			if (cs == null) {
				logger.error(getClass(),
						String.format("映射错误：%s 未有CjService注解", view));
				continue;
			}
			String name = cs.name();
			if (!name.startsWith("/")) {
				name = String.format("/%s", name);
			}
			if (name.lastIndexOf(".") == -1 && !name.endsWith("/")) {
				name = String.format("%s/", name);
			}
			this.mappings.put(name, view);
		}
	}

	protected void scansAndLoadPlugins(ServletConfig config, String home,
			Map<String, IJeeProgram> plugins) {
		String dir = String.format("%s/plugins", home);// 相对于主程序集的路径，或其它绝对路径
		File fdir = new File(dir);
		File arr[] = fdir.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		if (arr == null) {
			return;
		}
		for (File h : arr) {
			File[] jars = h.listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					// TODO Auto-generated method stub
					return name.endsWith(".plugin");
				}
			});
			if (jars.length > 1) {
				logger.error(getClass(), "在目录中有多个插件." + h);
				continue;
			}
			String fn = jars[0].getAbsolutePath();
			Assembly p = Assembly.loadAssembly(fn);
			p.start();
			ServiceCollection<JeeProgram> col = p.workbin()
					.part(JeeProgram.class);
			if (col.isEmpty()) {
				throw new EcmException("插件验证失败，原因：未发现JeeProgram的派生实现");
			}
			if (col.size() > 1) {
				throw new EcmException("插件验证失败，原因：多处定义JeeProgram的派生实现");
			}
			IJeeProgram program = col.get(0);
			this.plugins.put(p.getName(), program);
			program.init(config, h.getAbsolutePath());
		}
	}

	private String[] parseURI(String url) {
		String cntname = "";
		String reurl = "/";// 如果reurl不是文件（无扩展名）则在其后面加/
		if (!"/".equals(url)) {
			int firstpos = url.indexOf("/", 1);
			if (firstpos > -1) {
				cntname = url.substring(1, firstpos);
				reurl = url.substring(firstpos + 1, url.length());
			}
		}
		if (reurl.lastIndexOf(".") == -1 && !reurl.endsWith("/")) {
			reurl = String.format("%s/", reurl);
		}
		if (!reurl.startsWith("/")) {
			reurl = String.format("/%s", reurl);
		}
		return new String[] { cntname, reurl };
	}

	protected void dispatch(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		JeeHttpRequest request=(JeeHttpRequest)req;
		JeeHttpResponse response=(JeeHttpResponse)resp;
		
		
		String uri = request.relativeUrl();
		if(StringUtil.isEmpty(uri)){
			uri=req.getRequestURI();
		}
		if(uri.lastIndexOf(".")==-1&&uri.charAt(uri.length()-1)!='/'){
			uri=String.format("%s/", uri);
			resp.sendRedirect(uri);
			return;
		}
		if (WebUtil.documentMatch(uri, documentType)) {
			String[] url = parseURI(uri);
			request.relativeUrl(url[1]);
			IJeePipeline dpipeline=this.documentPipeline.createPipeline();
			try {
				dpipeline.flow(request, response);
			} catch (Exception e) {
				CircuitException ce = CircuitException.search(e);
				if (ce == null) {
					if(errors.containsKey("default")){
						try {
							doError("default", e, request, response);
						} catch (CircuitException e2) {
							resp.sendError(Integer.valueOf(e2.getStatus()),
									e2.getMessage());
							logger.error(getClass(), e2);
						}
						return;
					}
					resp.sendError(Integer.valueOf("503"),
							e.getMessage());
					logger.error(getClass(), e);
					return;
				}
				if ("404".equals(ce.getStatus())) {
					IJeePipeline rpipeline=this.resourcePipeline.createPipeline();
					try {
						rpipeline.flow(request, response);
					} catch (CircuitException e1) {
						if (errors.containsKey("404")) {// 处理错误页
							try {
								doError("404", e1, request, response);
							} catch (CircuitException e2) {
								resp.sendError(Integer.valueOf(e2.getStatus()),
										e2.getMessage());
								logger.error(getClass(), e2);
							}
							return;
						}
						if (errors.containsKey("default")) {
							try {
								doError("default", e1, request, response);
							} catch (CircuitException e2) {
								resp.sendError(Integer.valueOf(e2.getStatus()),
										e2.getMessage());
								logger.error(getClass(), e2);
							}
							return;
						}
						resp.sendError(Integer.valueOf(e1.getStatus()),
								e1.getMessage());
						logger.error(getClass(), e1);
					}finally{
						rpipeline.dispose();
					}
				} else {
					if (errors.containsKey(ce.getStatus())) {// 处理错误页
						try {
							doError(ce.getStatus(), e, request, response);
						} catch (CircuitException e2) {
							resp.sendError(Integer.valueOf(e2.getStatus()),
									e2.getMessage());
							logger.error(getClass(), e2);
						}
						return;
					}
					if (errors.containsKey("default")) {
						try {
							doError("default", e, request, response);
						} catch (CircuitException e2) {
							resp.sendError(Integer.valueOf(e2.getStatus()),
									e2.getMessage());
							logger.error(getClass(), e2);
						}
						return;
					}
					resp.sendError(Integer.valueOf(ce.getStatus()),
							ce.getMessage());
					logger.error(getClass(), ce);
				}

			}finally{
				dpipeline.dispose();
			}
		} else {
			String[] url = parseURI(uri);
			request.relativeUrl(url[1]);
			IJeePipeline rpipeline=this.resourcePipeline.createPipeline();
			try {
				rpipeline.flow(request, response);
			} catch (Exception e) {
				CircuitException ce=CircuitException.search(e);
				if(ce==null){
					if(errors.containsKey("default")){
						try {
							doError("default", e, request, response);
						} catch (CircuitException e2) {
							resp.sendError(Integer.valueOf(e2.getStatus()),
									e2.getMessage());
							logger.error(getClass(), e2);
						}
						return;
					}
					resp.sendError(Integer.valueOf("503"),
							e.getMessage());
					logger.error(getClass(), e);
					return;
				}
				if (errors.containsKey(ce.getStatus())) {// 处理错误页
					try {
						doError(ce.getStatus(), e, request, response);
					} catch (CircuitException e2) {
						resp.sendError(Integer.valueOf(e2.getStatus()),
								e2.getMessage());
						logger.error(getClass(), e2);
					}
					return;
				}
				if (errors.containsKey("default")) {
					try {
						doError("default", e, request, response);
					} catch (CircuitException e2) {
						resp.sendError(Integer.valueOf(e2.getStatus()),
								e2.getMessage());
						logger.error(getClass(), e2);
					}
					return;
				}
				resp.sendError(Integer.valueOf(ce.getStatus()), e.getMessage());
				logger.error(getClass(), e);
			}finally{
				rpipeline.dispose();
			}
		}

	}

	private String getExt(String url) {
		int pos=url.lastIndexOf(".");
		if(pos==-1)return "";
		return url.substring(pos+1,url.length());
	}

	private void doError(String code, Exception e, JeeHttpRequest request,
			JeeHttpResponse response)
			throws CircuitException, ServletException, IOException {
		logger.error(getClass(), e);
		String error = errors.get(code);
		if (StringUtil.isEmpty(error))
			return;
		request.relativeUrl(error);
		response.setError(e);
		IJeePipeline dpipeline=documentPipeline.createPipeline();
		try {
			dpipeline.flow(request, response);
		} catch (Exception e2) {
			CircuitException ce=CircuitException.search(e2);
			if (ce!=null) {
				if ("404".equals(ce.getStatus())) {
					IJeePipeline rpipeline=resourcePipeline.createPipeline();
					try{
						rpipeline.flow(request, response);
					}catch(Exception e3){
						throw e3;
					}finally{
						rpipeline.dispose();
					}
					return;
				}
				throw e2;
			}
			throw e2;
		}finally{
			dpipeline.dispose();
		}
	}

	@Override
	public final void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		dispatch(req, resp);
	}

	@Override
	public final void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		dispatch(req, resp);
	}

	@Override
	public final void doOptions(HttpServletRequest req,
			HttpServletResponse resp) throws ServletException, IOException {
		resp.sendError(503, "doOptions is not supported.");
	}

	@Override
	public final void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.sendError(503, "doPut is not supported.");
	}

	@Override
	public final void doTrace(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.sendError(503, "doTrace is not supported.");
	}

	class JeeSiteResource extends HttpContext implements IJeeSiteResource {

		public JeeSiteResource(String httpRoot) {
			super(httpRoot);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void redirect(String relativedUrl, JeeHttpResponse resp)
				throws IOException {
			resp.sendRedirect(relativedUrl);
		}
	}

	class JeeHttpFilterWrapper {
		int sort;
		IJeeHttpFilter filter;

		public JeeHttpFilterWrapper(int sort, IJeeHttpFilter filter) {
			this.sort = sort;
			this.filter = filter;
		}

		public JeeHttpFilterWrapper(int sort, String clazz) {
			super();
			this.sort = sort;
			try {
				Class<?> c = Class.forName(clazz);
				filter = (IJeeHttpFilter) c.newInstance();
			} catch (Exception e) {
				throw new EcmException(e);
			}
		}

	}

	class Pipeline implements IJeePipeline {
		boolean isLoaded;
		List<JeeHttpFilterWrapper> filters;
		int pos;

		@Override
		public IJeePipeline createPipeline() {
			// TODO Auto-generated method stub
			Pipeline p= new Pipeline();
			p.pos=0;
			p.isLoaded=isLoaded;
			p.filters=filters;
			return p;
		}
		@Override
		public void dispose() {
			pos=0;
			filters=null;
			isLoaded=false;
		}
		public Pipeline() {
			filters = new ArrayList<>();
		}

		@Override
		public void flow(JeeHttpRequest req, JeeHttpResponse resp)
				throws CircuitException, ServletException, IOException {
			if (pos >= filters.size())
				return;
			JeeHttpFilterWrapper w = filters.get(pos);
			pos++;
			w.filter.flow(req, resp, this);
		}

		@Override
		public void add(JeeHttpFilterWrapper wrapper) {
			filters.add(wrapper);
			if (isLoaded) {
				sort();
			}
		}

		@Override
		public void sort() {
			Collections.sort(filters, new Comparator<JeeHttpFilterWrapper>() {
				@Override
				public int compare(JeeHttpFilterWrapper o1,
						JeeHttpFilterWrapper o2) {
					return o1.sort > o2.sort ? 1 : -1;
				}
			});
		}

	}

	class DocumentEndFilter implements IJeeHttpFilter {

		@Override
		public void flow(JeeHttpRequest req, JeeHttpResponse resp,
				IJeePipeline pipeline)
				throws CircuitException, ServletException, IOException {
			String reurl = req.relativeUrl();
			String uri[] = parseURI(reurl);

			if (plugins.containsKey(uri[0])) {
				req.relativeUrl(reurl);
				JeeProgram jp = (JeeProgram) plugins.get(uri[0]);
				jp.dispatch(req, resp);
				return;
			}

//			req.relativeUrl(reurl);
			if (mappings.containsKey(reurl)) {
				IJeeWebView view = mappings.get(reurl);
				view.flow(req, resp, resource);
				return;
			}
			String filePath = FileHelper.getFilePathNoEx(reurl);
			filePath = filePath.replace("/", ".");
			String jssSelectName = "";
			if (filePath.startsWith(".")) {
				jssSelectName = String.format("$.cj.jss.%s%s",
						IJssModule.FIXED_MODULENAME_HTTP_JSS, filePath);
			} else {
				jssSelectName = String.format("$.cj.jss.%s.%s",
						IJssModule.FIXED_MODULENAME_HTTP_JSS, filePath);
			}
			Object jssview = null;
			jssview = site.getService(jssSelectName);
			if (jssview != null) {
				if (!(jssview instanceof IJeeWebView)) {
					String message = "未定义web视图接口,extends:'cj.studio.ecm.net.jee.IJeeWebView',必须声明为强jss类型：isStronglyJss:true,请求的jss服务："
							+ jssSelectName;
					throw new CircuitException("503", message);
				}
				IJeeWebView view = (IJeeWebView) jssview;
				view.flow(req, resp, resource);
				return;
			}
			String message = "找不到地址：" + req.getRequestURL();
			throw new CircuitException("404", message);
		}

	}

	class ResourceEndFilter implements IJeeHttpFilter {

		@Override
		public void flow(JeeHttpRequest req, JeeHttpResponse resp,
				IJeePipeline pipeline)
				throws CircuitException, ServletException, IOException {
			String reurl = req.relativeUrl();
			String uri[] = parseURI(reurl);
			String ext=getExt(reurl);
			if(mimes.containsKey(ext)){
				resp.setContentType(mimes.get(ext));
			}
			if (plugins.containsKey(uri[0])) {
				req.relativeUrl(reurl);
				JeeProgram jp = (JeeProgram) plugins.get(uri[0]);
				jp.dispatch(req, resp);
				return;
			}

//			req.relativeUrl(reurl);
			resp.getOutputStream().write(resource.resource(reurl));
		}

	}
}
