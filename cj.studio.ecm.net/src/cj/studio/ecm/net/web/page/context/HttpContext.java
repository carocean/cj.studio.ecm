package cj.studio.ecm.net.web.page.context;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.net.nio.NetConstans;
import cj.studio.ecm.net.web.HttpCircuit;
import cj.ultimate.IDisposable;
import cj.ultimate.util.StringUtil;

public abstract class HttpContext implements IDisposable, IHttpContext {
	protected String http_root;

	public HttpContext(String httpRoot) {
		http_root = httpRoot;
	}

	@Override
	public void dispose() {
		http_root = null;
	}

	/**
	 * 查询静态文档，如果不存在则报异常
	 * 
	 * <pre>
	 * 默认utf-8编码读取文件
	 * 用于组合静态html资源
	 * </pre>
	 * 
	 * @param path
	 *            相对路径，如果要取的文档地址正好与请求地址相同，则指定frame.relativePath()
	 * @return
	 * @throws IOException
	 */
	public Document html(String relativedUrl) throws CircuitException {
		return html(relativedUrl, "utf-8");

	}

	/**
	 * 交由客户端浏览器重新发起请求。
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param relativedUrl
	 * @throws CircuitException
	 */
	public void redirect(String fullUrl, Circuit circuit) {
		Circuit c = circuit;
		c.status("302");
		c.message("Redirect url.");
		c.head("Location", fullUrl);
	}

	public Document html(String relativedUrl, String charset)
			throws CircuitException {
		if (!relativedUrl.startsWith("/")) {
			relativedUrl = String.format("/%s", relativedUrl);
		}
		String rp = String.format("%s/%s", http_root, relativedUrl);
		rp = rp.replace("//", "/").replace("/", File.separator);
		DataInputStream dis = null;
		try {
			File f = new File(rp);
			if (f.length() >= Integer.MAX_VALUE) {
				throw new CircuitException(NetConstans.STATUS_503, String
						.format("要读取的doc文档太大，不能超过%s个字节", Integer.MAX_VALUE));
			}
			FileInputStream in = new FileInputStream(f);
			dis = new DataInputStream(in);
			String html = "";
			byte[] b = new byte[(int) f.length()];
			dis.readFully(b);
			html = new String(b, charset);
			Document doc = Jsoup.parse(html, charset/* r.getEncoding() */);
			return doc;
		} catch (FileNotFoundException e) {
			throw new CircuitException(NetConstans.STATUS_404,
					String.format("资源文件不存在:%s", relativedUrl));
		} catch (IOException e) {
			throw new CircuitException(NetConstans.STATUS_503, e);
		} finally {
			if (dis != null)
				try {
					dis.close();
				} catch (IOException e) {
				}
		}
	}

	/**
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param relativePath
	 *            文档路径，不带查询串
	 * @param resourceRefix
	 *            资源前缀：对于使用了plugin标记属性的元素，像<a href="test.svg" plugin>
	 *            如果指定前缀为：/dir1/则编译后在浏览器中看到的为：<a href="/dir1/test.svg" > <br>
	 *            如果没前缀则表示为当前，前缀如果为根则使用/它不是从上下文开始的
	 *            <b>格式：prefixPath?querystring，其中querystring将添加到资源地址的查询串中，它不区分是否重名，排在后的覆盖前者</b>
	 * @param charset
	 * @return
	 * @throws CircuitException
	 */
	@Override
	public Document html(String relativePath, String resourceRefix,
			String charset) throws CircuitException {
		Document doc = html(relativePath, charset);
		Elements all = doc.select("[plugin]");
		String q = "";
		String refixpath = "";
		String v = "";
		if (!StringUtil.isEmpty(resourceRefix)) {
			int pos = resourceRefix.indexOf("?");
			if (pos > -1) {
				q = resourceRefix.substring(pos, resourceRefix.length());
				refixpath = resourceRefix.substring(0, pos);
			} else {
				refixpath = resourceRefix;
			}
			if (!refixpath.endsWith("/")) {
				refixpath = String.format("%s/", refixpath);
			}
		}
		// if (refixpath.startsWith("/")) {
		// refixpath = String.format(".%s", refixpath);
		// }
		for (Element e : all) {
			switch (e.tagName()) {
			case "img":
			case "video":
			case "iframe":
			case "script":
				v = e.attr("src");
				v = String.format("%s%s%s", refixpath, v, q);
				e.attr("src", v);
				e.removeAttr("plugin");
				break;
			case "link":
			case "a":
				v = e.attr("href");
				v = String.format("%s%s%s", refixpath, v, q);
				e.attr("href", v);
				e.removeAttr("plugin");
				break;
			case "form":
				v = e.attr("action");
				v = String.format("%s%s%s", refixpath, v, q);
				e.attr("action", v);
				e.removeAttr("plugin");
				break;
			case "div":
			case "li":
				String value = e.attr("plugin");
				if ("part".equals(value)) {
					String part = e.attr(value);
					try {
						Document the = html(part, refixpath, charset);
						Elements childs = the.body().children();
						for (Element l : childs) {
							e.appendChild(l);
						}
						Elements heads = the.head().children();
						if (heads != null && heads.size() > 0) {
							for (Element l : heads) {
								e.appendChild(l);
							}
						}
						e.removeAttr("plugin");
						e.removeAttr(value);
					} catch (Exception err) {
						CJSystem.current().environment().logging()
								.error(getClass(), err.getCause());
					}
					break;
				}
			default:
				throw new CircuitException("503",
						String.format("请求地址：%s，资源前缀：%s,不支持的插件资源标签定义在：%s",
								relativePath, refixpath, e.tagName()));
			}
		}
		return doc;
	}

	public String resourceText(String relativedUrl) throws CircuitException {
		byte[] b = resource(relativedUrl);
		return new String(b);
	}

	public String getRealHttpSiteRootPath(Circuit circuit) {
		HttpCircuit c = (HttpCircuit) circuit;
		return c.httpSiteRoot();
	}

	public byte[] resource(String relativedUrl) throws CircuitException {

		String rp = String.format("%s/%s", http_root, relativedUrl);
		rp = rp.replace("//", "/").replace("/", File.separator);
		DataInputStream dis = null;
		try {
			File f = new File(rp);
			if (f.length() >= Integer.MAX_VALUE) {
				throw new CircuitException(NetConstans.STATUS_503, String
						.format("要读取的doc文档太大，不能超过%s个字节", Integer.MAX_VALUE));
			}
			FileInputStream in = new FileInputStream(f);
			dis = new DataInputStream(in);
			byte[] b = new byte[(int) f.length()];
			dis.readFully(b);
			return b;
		} catch (FileNotFoundException e) {
			throw new CircuitException(NetConstans.STATUS_404,
					String.format("资源文件不存在:%s", relativedUrl));
		} catch (IOException e) {
			throw new CircuitException(NetConstans.STATUS_503, e);
		} finally {
			if (dis != null)
				try {
					dis.close();
				} catch (IOException e) {
				}
		}
	}

}
