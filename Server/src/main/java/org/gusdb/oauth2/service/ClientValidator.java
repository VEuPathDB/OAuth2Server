package org.gusdb.oauth2.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.gusdb.oauth2.config.AllowedClient;

public class ClientValidator {

  private static final Logger LOG = LogManager.getLogger(ClientValidator.class);

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

  public boolean isValidGuestTokenClient(String clientId, String clientSecret) {
    return isClientAllowed(clientId, clientSecret, cli -> cli.allowGuestObtainment());
  }

  public boolean isValidProfileEditClient(String clientId, String clientSecret) {
    return isClientAllowed(clientId, clientSecret, cli -> cli.allowUserManagement());
  }

  public boolean isValidROPCGrantClient(String clientId, String clientSecret) {
    return isClientAllowed(clientId, clientSecret, cli -> cli.allowROPCGrant());
  }

  public boolean isValidUserQueryClient(String clientId, String clientSecret) {
    return isClientAllowed(clientId, clientSecret, cli -> cli.allowUserQueries());
  }

  private boolean isClientAllowed(String clientId, String clientSecret, Function<AllowedClient,Boolean> predicate) {
    return clientId != null && clientSecret != null
      && isValidClientId(clientId) && isValidClientSecret(clientId, clientSecret)
      && predicate.apply(_clientMap.get(clientId));
  }

  private boolean isValidClientId(String clientId) {
    boolean valid = _clientMap.containsKey(clientId);
    LOG.debug("Valid client ID [" + clientId + "]? " + valid);
    return valid;
  }

  private boolean isValidClientSecret(String clientId, String clientSecret) {
    AllowedClient client = _clientMap.get(clientId);

    // Bug fix 2-29-2024: Accept both URL-encoded and non-encoded client secrets.  The OAuth 2.0
    //   spec (https://www.rfc-editor.org/rfc/rfc6749#section-2.3.1), and newest version of
    //   Apache's mod_auth (used by Apollo) URL-encode client secret values before sending in
    //   the request body.  However, our legacy code and more importantly, Globus's requests,
    //   use non-encoded values.  We could change our code, but don't want the headache of
    //   figuring out if Globus is in the wrong and asking them to change.  Thus, support both.
    // p.s. this means we cannot issue client secrets with '%' characters!!!
    if (clientSecret.contains("%"))
      clientSecret = URLDecoder.decode(clientSecret, StandardCharsets.UTF_8);

    boolean valid = (client == null ? false : client.getSecrets().contains(clientSecret));
    // FIXME: log shows submitted vs valid secrets; may use again since URL encoding of secret could be an issue in the future
    //LOG.debug("Client secrets:\nSubmitted: " + clientSecret + client.getSecrets().stream().map(s -> "\nValid    :" + s).collect(Collectors.joining()));
    LOG.debug("Valid client secret for ID [" + clientId + "]? " + valid);
    return valid;
  }

  private boolean isValidRedirectUri(String clientId, String redirectUri) {
    if (!_validateDomains) return true;
    try {
      String redirectUriHost = new URI(redirectUri).getHost();
      if (redirectUriHost == null) return false;
      AllowedClient client = _clientMap.get(clientId);
      boolean valid = false;
      for (String validDomain : client.getDomains()) {
        LOG.debug("For client '"+ clientId + "', checking passed URL host '" + redirectUriHost + "' against " + validDomain);
        // check for exact match of validDomain
        if (validDomain.equalsIgnoreCase(redirectUriHost) ||
            // if validDomain has a subdomain wildcard...
            (validDomain.startsWith("*.") && (
                // allow an exact match of the root domain, or...
                redirectUriHost.equalsIgnoreCase(validDomain.substring(2)) ||
                // allow a subdomain
                redirectUriHost.toLowerCase().endsWith(validDomain.toLowerCase().substring(1))))) {
          LOG.debug("Is valid!");
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

  public boolean isValidLogoutClient(String redirectUri) {
    // any valid domain on any client is legal
    for (String clientId : _clientMap.keySet()) {
      if (isValidRedirectUri(clientId, redirectUri)) return true;
    }
    return false;
  }

}
