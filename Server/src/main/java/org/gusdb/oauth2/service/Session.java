package org.gusdb.oauth2.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpSession;

import org.gusdb.oauth2.service.util.AuthzRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Session {

  private static final Logger LOG = LoggerFactory.getLogger(Session.class);

  private static final String SESSION_USERNAME_KEY = "username";
  private static final String SESSION_FORM_ID_MAP_KEY = "formIdMap";

  private final HttpSession _session;

  public Session(HttpSession session) {
    _session = session;
    synchronized(session) {
      if (getFormIdMap() == null) {
        _session.setAttribute(SESSION_FORM_ID_MAP_KEY, new HashMap<String, AuthzRequest>());
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

  public AuthzRequest clearFormId(String formId) {
    return getFormIdMap().remove(formId);
  }

  public boolean isFormId(String formId) {
    return getFormIdMap().containsKey(formId);
  }

  public String generateFormId(AuthzRequest authRequest) {
    String nextFormId = UUID.randomUUID().toString();
    LOG.debug("Generated formId [" + nextFormId + "] to reference AuthzRequest");
    getFormIdMap().put(nextFormId, authRequest);
    return nextFormId;
  }

  @SuppressWarnings("unchecked")
  private Map<String, AuthzRequest> getFormIdMap() {
    return (Map<String, AuthzRequest>)_session.getAttribute(SESSION_FORM_ID_MAP_KEY);
  }

  public void invalidate() {
    String username = getUsername();
    if (username != null) {
      TokenStore.clearObjectsForUser(username);
    }
    _session.invalidate();
  }
}
