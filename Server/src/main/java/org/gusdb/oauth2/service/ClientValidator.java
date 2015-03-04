package org.gusdb.oauth2.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.gusdb.oauth2.config.AllowedClient;

public class ClientValidator {

  private Map<String, AllowedClient> _clientMap = new HashMap<>();
  private Set<String> _allAllowedDomains = new HashSet<>();

  public ClientValidator(List<AllowedClient> allowedClients) {
    for (AllowedClient client : allowedClients) {
      _allAllowedDomains.addAll(client.getDomains());
      _clientMap.put(client.getId(), client);
    }
  }

  public boolean isValidTokenClient(OAuthTokenRequest oauthRequest) {
    return (isValidClientId(oauthRequest.getClientId()) &&
        isValidRedirectUri(oauthRequest.getClientId(), oauthRequest.getRedirectURI()));
  }

  public boolean isValidAuthorizationClient(OAuthAuthzRequest oauthRequest) {
    return (isValidClientId(oauthRequest.getClientId()) &&
        isValidClientSecret(oauthRequest.getClientId(), oauthRequest.getClientSecret()) &&
        isValidRedirectUri(oauthRequest.getClientId(), oauthRequest.getRedirectURI()));
  }

  private boolean isValidClientId(String clientId) {
    return _clientMap.containsKey(clientId);
  }

  private boolean isValidClientSecret(String clientId, String clientSecret) {
    AllowedClient client = _clientMap.get(clientId);
    if (client == null) return false;
    return client.getSecret().equals(clientSecret);
  }

  private boolean isValidRedirectUri(String clientId, String redirectUri) {
    try {
      String redirectUriHost = new URI(redirectUri).getHost();
      AllowedClient client = _clientMap.get(clientId);
      return (client.getDomains().contains(redirectUriHost));
    }
    catch (URISyntaxException e) {
      return false;
    }
  }
}
