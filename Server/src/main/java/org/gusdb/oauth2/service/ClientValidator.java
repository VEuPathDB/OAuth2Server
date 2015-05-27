package org.gusdb.oauth2.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.gusdb.oauth2.config.AllowedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientValidator {

  private static final Logger LOG = LoggerFactory.getLogger(ClientValidator.class);

  private final Map<String, AllowedClient> _clientMap = new HashMap<>();
  private final boolean _validateDomains;

  public ClientValidator(List<AllowedClient> allowedClients, boolean validateDomains) {
    for (AllowedClient client : allowedClients) {
      _clientMap.put(client.getId(), client);
    }
    _validateDomains = validateDomains;
  }

  public boolean isValidAuthorizationClient(OAuthAuthzRequest oauthRequest) {
    return (isValidClientId(oauthRequest.getClientId()) &&
        isValidRedirectUri(oauthRequest.getClientId(), oauthRequest.getRedirectURI()));
  }

  public boolean isValidTokenClient(OAuthTokenRequest oauthRequest) {
    return (isValidClientId(oauthRequest.getClientId()) &&
        isValidClientSecret(oauthRequest.getClientId(), oauthRequest.getClientSecret()) &&
        isValidRedirectUri(oauthRequest.getClientId(), oauthRequest.getRedirectURI()));
  }

  private boolean isValidClientId(String clientId) {
    boolean valid = _clientMap.containsKey(clientId);
    LOG.debug("Valid client ID [" + clientId + "]? " + valid);
    return valid;
  }

  private boolean isValidClientSecret(String clientId, String clientSecret) {
    AllowedClient client = _clientMap.get(clientId);
    boolean valid = (client == null ? false : client.getSecret().equals(clientSecret));
    LOG.debug("Valid client secret for ID [" + clientId + "]? " + valid);
    return valid;
  }

  private boolean isValidRedirectUri(String clientId, String redirectUri) {
    if (!_validateDomains) return true;
    try {
      String redirectUriHost = new URI(redirectUri).getHost();
      AllowedClient client = _clientMap.get(clientId);
      boolean valid = false;
      for (String validDomain : client.getDomains()) {
        if (validDomain.equalsIgnoreCase(redirectUriHost) ||
            (validDomain.startsWith("*.") &&
             redirectUriHost.toLowerCase().endsWith(validDomain.toLowerCase().substring(2)))) {
          valid = true;
          break;
        }
      }
      LOG.debug("Valid redirectUri host [" + redirectUriHost + "] for client [" + clientId + "]? " + valid);
      return valid;
    }
    catch (URISyntaxException e) {
      LOG.warn("Unable to parse passed redirectUri [" + redirectUri + "] into URI object");
      return false;
    }
  }
}
