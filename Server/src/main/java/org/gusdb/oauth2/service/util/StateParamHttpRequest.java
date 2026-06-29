package org.gusdb.oauth2.service.util;

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

import org.apache.oltu.oauth2.common.OAuth;

public class StateParamHttpRequest implements HttpServletRequest {

  private static final String MESSAGE = "This class exists only to provide the 'state' param value";

  private final String _state;

  public StateParamHttpRequest(String state) {
    _state = state;
  }

  @Override
  public String getParameter(String name) {
    if (OAuth.OAUTH_STATE.equals(name)) {
      return _state;
    }
    throw new UnsupportedOperationException(MESSAGE);
  }

  /************************************************************************/
  /** All methods below this comment throw UnsupportedOperationException **/
  /************************************************************************/

  @Override
  public Object getAttribute(String name) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getCharacterEncoding() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
    throw new UnsupportedOperationException(MESSAGE);

  }

  @Override
  public int getContentLength() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getContentType() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public Enumeration<String> getParameterNames() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String[] getParameterValues(String name) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public Map<String, String[]> getParameterMap() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getProtocol() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getScheme() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getServerName() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public int getServerPort() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public BufferedReader getReader() throws IOException {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getRemoteAddr() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getRemoteHost() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public void setAttribute(String name, Object o) {
    throw new UnsupportedOperationException(MESSAGE);

  }

  @Override
  public void removeAttribute(String name) {
    throw new UnsupportedOperationException(MESSAGE);

  }

  @Override
  public Locale getLocale() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public Enumeration<Locale> getLocales() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public boolean isSecure() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String path) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getRealPath(String path) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public int getRemotePort() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getLocalName() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getLocalAddr() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public int getLocalPort() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getAuthType() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public Cookie[] getCookies() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public long getDateHeader(String name) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getHeader(String name) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public int getIntHeader(String name) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getMethod() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getPathInfo() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getPathTranslated() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getContextPath() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getQueryString() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getRemoteUser() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public boolean isUserInRole(String role) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public Principal getUserPrincipal() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getRequestedSessionId() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getRequestURI() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public StringBuffer getRequestURL() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getServletPath() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public HttpSession getSession(boolean create) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public HttpSession getSession() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public boolean isRequestedSessionIdValid() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public boolean isRequestedSessionIdFromCookie() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public boolean isRequestedSessionIdFromURL() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public boolean isRequestedSessionIdFromUrl() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public long getContentLengthLong() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public ServletContext getServletContext() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public AsyncContext startAsync() throws IllegalStateException {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
      throws IllegalStateException {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public boolean isAsyncStarted() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public boolean isAsyncSupported() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public AsyncContext getAsyncContext() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public DispatcherType getDispatcherType() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String changeSessionId() {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public void login(String username, String password) throws ServletException {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public void logout() throws ServletException {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public Collection<Part> getParts() throws IOException, ServletException {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public Part getPart(String name) throws IOException, ServletException {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass)
      throws IOException, ServletException {
    throw new UnsupportedOperationException(MESSAGE);
  }

}
