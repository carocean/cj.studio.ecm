package cj.studio.ecm.net.web.widget.context;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.net.web.HttpCircuit;
import cj.ultimate.IDisposable;

public abstract class WidgetContext implements IDisposable {
	String http_root;

	public WidgetContext(String httpRoot) {
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
	public Document html(String relativedUrl) throws IOException {
		return html(relativedUrl, "utf-8");

	}
	public Document html(String relativedUrl, String charset)
			throws IOException {

		String rp = String.format("%s/%s", http_root, relativedUrl);
		rp = rp.replace("//", "/").replace("/", File.separator);
		DataInputStream dis = null;
		try {
			File f = new File(rp);
			if (f.length() >= Integer.MAX_VALUE) {
				throw new IOException(String.format("要读取的doc文档太大，不能超过%s个字节",
						Integer.MAX_VALUE));
			}
			FileInputStream in = new FileInputStream(f);
			dis = new DataInputStream(in);
			String html = "";
			byte[] b = new byte[(int) f.length()];
			dis.readFully(b);
			html = new String(b, charset);
			Document doc = Jsoup.parse(html, charset/* r.getEncoding() */);
			return doc;
		} catch (IOException e) {
			throw new IOException(e);
		} finally {
			if (dis != null)
				dis.close();
		}
	}

	public String resourceText(String relativedUrl) throws IOException {
		byte[] b=resource(relativedUrl);
		return new String(b);
	}
	public String getRealHttpSiteRootPath(Circuit circuit){
		HttpCircuit c=(HttpCircuit)circuit;
		return c.httpSiteRoot();
	}
	public byte[] resource(String relativedUrl) throws IOException {

		String rp = String.format("%s/%s", http_root, relativedUrl);
		rp = rp.replace("//", "/").replace("/", File.separator);
		DataInputStream dis = null;
		try {
			File f = new File(rp);
			if (f.length() >= Integer.MAX_VALUE) {
				throw new IOException(String.format("要读取的doc文档太大，不能超过%s个字节",
						Integer.MAX_VALUE));
			}
			FileInputStream in = new FileInputStream(f);
			dis = new DataInputStream(in);
			byte[] b = new byte[(int) f.length()];
			dis.readFully(b);
			return b;
		} catch (IOException e) {
			throw new IOException(e);
		} finally {
			if (dis != null)
				dis.close();
		}
	}

	
}
