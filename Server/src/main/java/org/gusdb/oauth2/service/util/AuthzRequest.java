package org.gusdb.oauth2.service.util;

import java.util.HashSet;
import java.util.Set;

import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;

public class AuthzRequest {

  private final String _clientId;
  private final String _clientSecret;
  private final String _redirectUri;
  private final String _responseType;
  private final String _state;
  private final Set<String> _scopes;
  
  public AuthzRequest(OAuthAuthzRequest request) {
    _clientId = request.getClientId();
    _clientSecret = request.getClientSecret();
    _redirectUri = request.getRedirectURI();
    _responseType = request.getResponseType();
    _state = request.getState();
    _scopes = new HashSet<>(request.getScopes());
  }

  public String getClientId() {
    return _clientId;
  }

  public String getClientSecret() {
    return _clientSecret;
  }

  public String getRedirectUri() {
    return _redirectUri;
  }

  public String getResponseType() {
    return _responseType;
  }

  public String getState() {
    return _state;
  }

  public Set<String> getScopes() {
    return _scopes;
  }
}
