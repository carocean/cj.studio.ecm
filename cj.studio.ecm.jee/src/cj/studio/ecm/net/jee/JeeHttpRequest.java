package cj.studio.ecm.net.jee;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

public class JeeHttpRequest implements HttpServletRequest{
	private HttpServletRequest request;
	private String relativeUrl;
	public JeeHttpRequest(HttpServletRequest request) {
		this.request=request;
	}
	@Override
	public Object getAttribute(String name) {
		return request.getAttribute(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		// TODO Auto-generated method stub
		return request.getAttributeNames();
	}

	@Override
	public String getCharacterEncoding() {
		// TODO Auto-generated method stub
		return request.getCharacterEncoding();
	}

	@Override
	public void setCharacterEncoding(String env)
			throws UnsupportedEncodingException {
		// TODO Auto-generated method stub
		request.setCharacterEncoding(env);
	}

	@Override
	public int getContentLength() {
		// TODO Auto-generated method stub
		return request.getContentLength();
	}

	@Override
	public long getContentLengthLong() {
		// TODO Auto-generated method stub
		return request.getContentLengthLong();
	}

	@Override
	public String getContentType() {
		// TODO Auto-generated method stub
		return request.getContentType();
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		// TODO Auto-generated method stub
		return request.getInputStream();
	}

	@Override
	public String getParameter(String name) {
		// TODO Auto-generated method stub
		return request.getParameter(name);
	}

	@Override
	public Enumeration<String> getParameterNames() {
		// TODO Auto-generated method stub
		return request.getParameterNames();
	}

	@Override
	public String[] getParameterValues(String name) {
		// TODO Auto-generated method stub
		return request.getParameterValues(name);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		// TODO Auto-generated method stub
		return request.getParameterMap();
	}

	@Override
	public String getProtocol() {
		// TODO Auto-generated method stub
		return request.getProtocol();
	}

	@Override
	public String getScheme() {
		// TODO Auto-generated method stub
		return request.getScheme();
	}

	@Override
	public String getServerName() {
		// TODO Auto-generated method stub
		return request.getServerName();
	}

	@Override
	public int getServerPort() {
		// TODO Auto-generated method stub
		return request.getServerPort();
	}

	@Override
	public BufferedReader getReader() throws IOException {
		// TODO Auto-generated method stub
		return request.getReader();
	}

	@Override
	public String getRemoteAddr() {
		// TODO Auto-generated method stub
		return getRemoteAddr();
	}

	@Override
	public String getRemoteHost() {
		// TODO Auto-generated method stub
		return request.getRemoteHost();
	}

	@Override
	public void setAttribute(String name, Object o) {
		// TODO Auto-generated method stub
		request.setAttribute(name, o);
	}

	@Override
	public void removeAttribute(String name) {
		// TODO Auto-generated method stub
		request.removeAttribute(name);
	}

	@Override
	public Locale getLocale() {
		// TODO Auto-generated method stub
		return request.getLocale();
	}

	@Override
	public Enumeration<Locale> getLocales() {
		// TODO Auto-generated method stub
		return request.getLocales();
	}

	@Override
	public boolean isSecure() {
		// TODO Auto-generated method stub
		return request.isSecure();
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		return request.getRequestDispatcher(path);
	}
	@Deprecated
	@Override
	public String getRealPath(String path) {
		// TODO Auto-generated method stub
		return request.getRealPath(path);
	}

	@Override
	public int getRemotePort() {
		// TODO Auto-generated method stub
		return request.getRemotePort();
	}

	@Override
	public String getLocalName() {
		// TODO Auto-generated method stub
		return request.getLocalName();
	}

	@Override
	public String getLocalAddr() {
		// TODO Auto-generated method stub
		return request.getLocalAddr();
	}

	@Override
	public int getLocalPort() {
		// TODO Auto-generated method stub
		return request.getLocalPort();
	}

	@Override
	public ServletContext getServletContext() {
		// TODO Auto-generated method stub
		return request.getServletContext();
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		// TODO Auto-generated method stub
		return request.startAsync();
	}

	@Override
	public AsyncContext startAsync(ServletRequest servletRequest,
			ServletResponse servletResponse) throws IllegalStateException {
		// TODO Auto-generated method stub
		return request.startAsync();
	}

	@Override
	public boolean isAsyncStarted() {
		// TODO Auto-generated method stub
		return request.isAsyncStarted();
	}

	@Override
	public boolean isAsyncSupported() {
		// TODO Auto-generated method stub
		return request.isAsyncSupported();
	}

	@Override
	public AsyncContext getAsyncContext() {
		// TODO Auto-generated method stub
		return request.getAsyncContext();
	}

	@Override
	public DispatcherType getDispatcherType() {
		// TODO Auto-generated method stub
		return request.getDispatcherType();
	}

	@Override
	public String getAuthType() {
		// TODO Auto-generated method stub
		return request.getAuthType();
	}

	@Override
	public Cookie[] getCookies() {
		// TODO Auto-generated method stub
		return request.getCookies();
	}

	@Override
	public long getDateHeader(String name) {
		// TODO Auto-generated method stub
		return request.getDateHeader(name);
	}

	@Override
	public String getHeader(String name) {
		// TODO Auto-generated method stub
		return request.getHeader(name);
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		// TODO Auto-generated method stub
		return request.getHeaders(name);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		// TODO Auto-generated method stub
		return request.getHeaderNames();
	}

	@Override
	public int getIntHeader(String name) {
		// TODO Auto-generated method stub
		return request.getIntHeader(name);
	}

	@Override
	public String getMethod() {
		// TODO Auto-generated method stub
		return request.getMethod();
	}

	@Override
	public String getPathInfo() {
		// TODO Auto-generated method stub
		return request.getPathInfo();
	}

	@Override
	public String getPathTranslated() {
		// TODO Auto-generated method stub
		return request.getPathTranslated();
	}

	@Override
	public String getContextPath() {
		// TODO Auto-generated method stub
		return request.getContextPath();
	}

	@Override
	public String getQueryString() {
		// TODO Auto-generated method stub
		return request.getQueryString();
	}

	@Override
	public String getRemoteUser() {
		// TODO Auto-generated method stub
		return request.getRemoteUser();
	}

	@Override
	public boolean isUserInRole(String role) {
		// TODO Auto-generated method stub
		return request.isUserInRole(role);
	}

	@Override
	public Principal getUserPrincipal() {
		// TODO Auto-generated method stub
		return request.getUserPrincipal();
	}

	@Override
	public String getRequestedSessionId() {
		// TODO Auto-generated method stub
		return request.getRequestedSessionId();
	}
	/**
	 * 不带root的原地址，包含查询串
	 * 
	 * <pre>
	 * 对于根程序集，则不带web上下文名
	 * 对于插件，则不带web上下文名和插件名
	 * </pre>
	 * 
	 * @return
	 */
	public String relativeUrl() {
		return relativeUrl;
	}
	public void relativeUrl(String relativeUrl) {
		this.relativeUrl = relativeUrl;
	}
	@Override
	public String getRequestURI() {
		return request.getRequestURI();
	}

	@Override
	public StringBuffer getRequestURL() {
		// TODO Auto-generated method stub
		return request.getRequestURL();
	}

	@Override
	public String getServletPath() {
		// TODO Auto-generated method stub
		return request.getServletPath();
	}

	@Override
	public HttpSession getSession(boolean create) {
		// TODO Auto-generated method stub
		return request.getSession();
	}

	@Override
	public HttpSession getSession() {
		// TODO Auto-generated method stub
		return request.getSession();
	}

	@Override
	public String changeSessionId() {
		// TODO Auto-generated method stub
		return request.changeSessionId();
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		// TODO Auto-generated method stub
		return request.isRequestedSessionIdValid();
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		// TODO Auto-generated method stub
		return request.isRequestedSessionIdFromCookie();
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		// TODO Auto-generated method stub
		return request.isRequestedSessionIdFromURL();
	}
	@Deprecated
	@Override
	public boolean isRequestedSessionIdFromUrl() {
		// TODO Auto-generated method stub
		return request.isRequestedSessionIdFromUrl();
	}

	@Override
	public boolean authenticate(HttpServletResponse response)
			throws IOException, ServletException {
		// TODO Auto-generated method stub
		return request.authenticate(response);
	}

	@Override
	public void login(String username, String password)
			throws ServletException {
		// TODO Auto-generated method stub
		request.login(username, password);
	}

	@Override
	public void logout() throws ServletException {
		// TODO Auto-generated method stub
		request.logout();
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		// TODO Auto-generated method stub
		return request.getParts();
	}

	@Override
	public Part getPart(String name) throws IOException, ServletException {
		// TODO Auto-generated method stub
		return request.getPart(name);
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass)
			throws IOException, ServletException {
		// TODO Auto-generated method stub
		return request.upgrade(handlerClass);
	}

}
