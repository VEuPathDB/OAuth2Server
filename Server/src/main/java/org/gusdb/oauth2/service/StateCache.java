package org.gusdb.oauth2.service;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public class StateCache {

  private static final String SESSION_USERNAME_KEY = "username";

  private static final List<String> AUTH_CODES = new ArrayList<String>();
  private static final List<String> TOKENS = new ArrayList<String>();

  public static synchronized void addAuthCode(String authorizationCode) {
    AUTH_CODES.add(authorizationCode);
  }

  public static void addToken(String accessToken) {
    TOKENS.add(accessToken);
  }

  public static boolean isValidToken(String accessToken) {
    return TOKENS.contains(accessToken);
  }

  public static void setUsername(HttpServletRequest request, String username) {
    request.getSession().setAttribute(SESSION_USERNAME_KEY, username);
  }

  public static String getUsername(HttpServletRequest request) {
    return (String)request.getSession().getAttribute(SESSION_USERNAME_KEY);
  }

  public static void logOut(HttpServletRequest request) {
    request.getSession().removeAttribute(SESSION_USERNAME_KEY);
    request.getSession().invalidate();
  }

  public static boolean checkAuthCode(String authorizationCode) {
    return AUTH_CODES.contains(authorizationCode);
  }

}
