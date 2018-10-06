package cj.studio.ecm.net.jee;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
/**
 * jee程序入口
 * <pre>
 * - jee程序必须派生此类
 * - 提供了地址调度功能
 * - 插件管理
 * </pre>
 * @author carocean
 *
 */
public interface IJeeProgram {
	 void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException;
	 void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException ;

	 void doOptions(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException ;
	 void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException;

	 void doTrace(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException;
	void init(ServletConfig config, String home);
}
