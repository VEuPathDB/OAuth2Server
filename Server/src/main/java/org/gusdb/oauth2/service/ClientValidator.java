package org.gusdb.oauth2.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
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

  public boolean checkClientId(String clientId) {
    return _clientMap.containsKey(clientId);
  }

  public boolean checkClientSecret(String clientId, String clientSecret) {
    AllowedClient client = _clientMap.get(clientId);
    if (client == null) return false;
    return client.getSecret().equals(clientSecret);
  }

  // FIXME: implement
  public void validateRedirectionURI(OAuthAuthzRequest oauthRequest) throws OAuthProblemException {
    boolean bad = false;
    if (bad) {
      throw OAuthProblemException.error("bad");
    }
  }
}
