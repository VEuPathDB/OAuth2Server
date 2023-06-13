package org.gusdb.oauth2.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.oauth2.service.token.TokenStore;
import org.gusdb.oauth2.service.util.AuthzRequest;

public class Session {

  private static final Logger LOG = LogManager.getLogger(Session.class);

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

  public AuthzRequest getOriginalRequest(String formId) {
    return getFormIdMap().get(formId);
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

  public Set<String> getFormIds() {
    return getFormIdMap().keySet();
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
