package cj.studio.ecm.net.jee;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import cj.studio.ecm.graph.CircuitException;

public class JeeHttpResponse implements HttpServletResponse {
	private HttpServletResponse response;
	private String errorCode;
	private String errorMessage;
	private String errorCause;

	public JeeHttpResponse(HttpServletResponse response) {
		this.response = response;
	}

	@Override
	public String getCharacterEncoding() {
		// TODO Auto-generated method stub
		return response.getCharacterEncoding();
	}

	@Override
	public String getContentType() {
		// TODO Auto-generated method stub
		return response.getContentType();
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		// TODO Auto-generated method stub
		return response.getOutputStream();
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		return response.getWriter();
	}

	@Override
	public void setCharacterEncoding(String charset) {
		// TODO Auto-generated method stub
		response.setCharacterEncoding(charset);
	}

	@Override
	public void setContentLength(int len) {
		// TODO Auto-generated method stub
		response.setContentLength(len);
	}

	@Override
	public void setContentLengthLong(long len) {
		// TODO Auto-generated method stub
		response.setContentLengthLong(len);
	}

	@Override
	public void setContentType(String type) {
		// TODO Auto-generated method stub
		response.setContentType(type);
	}

	@Override
	public void setBufferSize(int size) {
		// TODO Auto-generated method stub
		response.setBufferSize(size);
	}

	@Override
	public int getBufferSize() {
		// TODO Auto-generated method stub
		return response.getBufferSize();
	}

	@Override
	public void flushBuffer() throws IOException {
		// TODO Auto-generated method stub
		response.flushBuffer();
	}

	@Override
	public void resetBuffer() {
		// TODO Auto-generated method stub
		response.resetBuffer();
	}

	@Override
	public boolean isCommitted() {
		// TODO Auto-generated method stub
		return response.isCommitted();
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		response.reset();
	}

	@Override
	public void setLocale(Locale loc) {
		// TODO Auto-generated method stub
		response.setLocale(loc);
	}

	@Override
	public Locale getLocale() {
		// TODO Auto-generated method stub
		return response.getLocale();
	}

	@Override
	public void addCookie(Cookie cookie) {
		// TODO Auto-generated method stub
		response.addCookie(cookie);
	}

	@Override
	public boolean containsHeader(String name) {
		// TODO Auto-generated method stub
		return response.containsHeader(name);
	}

	@Override
	public String encodeURL(String url) {
		// TODO Auto-generated method stub
		return response.encodeURL(url);
	}

	@Deprecated
	@Override
	public String encodeRedirectURL(String url) {
		// TODO Auto-generated method stub
		return response.encodeRedirectUrl(url);
	}

	@Deprecated
	@Override
	public String encodeUrl(String url) {
		// TODO Auto-generated method stub
		return response.encodeUrl(url);
	}

	@Override
	public String encodeRedirectUrl(String url) {
		// TODO Auto-generated method stub
		return response.encodeRedirectURL(url);
	}

	@Override
	public void sendError(int sc, String msg) throws IOException {
		// TODO Auto-generated method stub
		response.sendError(sc, msg);
	}

	@Override
	public void sendError(int sc) throws IOException {
		// TODO Auto-generated method stub
		response.sendError(sc);
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		// TODO Auto-generated method stub
		response.sendRedirect(location);
	}

	@Override
	public void setDateHeader(String name, long date) {
		// TODO Auto-generated method stub
		response.setDateHeader(name, date);
	}

	@Override
	public void addDateHeader(String name, long date) {
		// TODO Auto-generated method stub
		response.addDateHeader(name, date);
	}

	@Override
	public void setHeader(String name, String value) {
		// TODO Auto-generated method stub
		response.setHeader(name, value);
	}

	@Override
	public void addHeader(String name, String value) {
		// TODO Auto-generated method stub
		response.addHeader(name, value);
	}

	@Override
	public void setIntHeader(String name, int value) {
		// TODO Auto-generated method stub
		response.setIntHeader(name, value);
	}

	@Override
	public void addIntHeader(String name, int value) {
		// TODO Auto-generated method stub
		response.addIntHeader(name, value);
	}

	@Override
	public void setStatus(int sc) {
		// TODO Auto-generated method stub
		response.setStatus(sc);
	}

	@Deprecated
	@Override
	public void setStatus(int sc, String sm) {
		// TODO Auto-generated method stub
		response.setStatus(sc, sm);
	}

	@Override
	public int getStatus() {
		// TODO Auto-generated method stub
		return response.getStatus();
	}

	@Override
	public String getHeader(String name) {
		// TODO Auto-generated method stub
		return response.getHeader(name);
	}

	@Override
	public Collection<String> getHeaders(String name) {
		// TODO Auto-generated method stub
		return response.getHeaders(name);
	}

	@Override
	public Collection<String> getHeaderNames() {
		// TODO Auto-generated method stub
		return response.getHeaderNames();
	}
	public String errorCause() {
		return errorCause;
	}
	public String errorCode() {
		return errorCode;
	}
	public String errorMessage() {
		return errorMessage;
	}
	public void setError(Throwable e) {
		CircuitException ce = CircuitException.search(e);
		StringWriter sw = new StringWriter();  
        PrintWriter pw = new PrintWriter(sw);  
        e.printStackTrace(pw);  
		if (ce != null) {
			errorCode=ce.getStatus();
			errorMessage=ce.getMessage();
			errorCause=sw.toString();
		} else {
			errorCode="500";
			errorMessage=e.getMessage();
			errorCause=sw.toString();
		}

	}

}
