package org.gusdb.oauth2.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpSession;

import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;

public class Session {

  private static final String SESSION_USERNAME_KEY = "username";
  private static final String SESSION_FORM_ID_MAP_KEY = "formIdMap";

  private final HttpSession _session;

  public Session(HttpSession session) {
    _session = session;
    synchronized(session) {
      if (getFormIdMap() == null) {
        _session.setAttribute(SESSION_FORM_ID_MAP_KEY, new HashMap<String, OAuthAuthzRequest>());
      }
    }
  }

  public boolean isAuthenticated() {
    return getUsername() != null;
  }

  public String getUsername() {
    return (String)_session.getAttribute(SESSION_USERNAME_KEY);
  }

  public void setUsername(String username) {
    _session.setAttribute(SESSION_USERNAME_KEY, username);
  }

  public OAuthAuthzRequest clearFormId(String formId) {
    return getFormIdMap().remove(formId);
  }

  public String generateFormId(OAuthAuthzRequest authRequest) {
    String nextFormId = UUID.randomUUID().toString();
    getFormIdMap().put(nextFormId, authRequest);
    return nextFormId;
  }

  @SuppressWarnings("unchecked")
  private Map<String, OAuthAuthzRequest> getFormIdMap() {
    return (Map<String, OAuthAuthzRequest>)_session.getAttribute(SESSION_FORM_ID_MAP_KEY);
  }
}
