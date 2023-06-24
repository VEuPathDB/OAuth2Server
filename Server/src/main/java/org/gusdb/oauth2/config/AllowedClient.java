package org.gusdb.oauth2.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.oauth2.InitializationException;

public class AllowedClient {

  private static final Logger LOG = LogManager.getLogger(AllowedClient.class);

  private static enum JsonKey {
    clientId,
    clientSecret,
    signingKey,
    clientDomains
  }

  public static AllowedClient createFromJson(JsonObject json) throws InitializationException {
    // get id and secret
    String clientId = json.getString(JsonKey.clientId.name());
    String clientSecret = json.getString(JsonKey.clientSecret.name());
    // validate domain list
    JsonArray clientDomains = json.getJsonArray(JsonKey.clientDomains.name());
    Set<String> domainList = new HashSet<>();
    if (clientDomains != null) {
      for (int i = 0; i < clientDomains.size(); i++) {
        domainList.add(clientDomains.getString(i));
      }
    }
    String signingKey = json.getString(JsonKey.signingKey.name());
    return new AllowedClient(clientId, clientSecret, signingKey, domainList);
  }

  private String _id;
  private String _secret;
  private String _signingKey;
  private Set<String> _domains;

  public AllowedClient(String id, String secret, String signingKey, Set<String> domains) throws InitializationException {
    _id = id;
    _secret = secret;
    _signingKey = signingKey;
    _domains = domains;
    if (_id == null || _id.isEmpty() ||
        _secret == null || _secret.isEmpty() ||
        _domains == null || domains.isEmpty() ||
        _domains.iterator().next() == null ||
        _domains.iterator().next().isEmpty()) {
      throw new InitializationException("clientId and clientSecret must be populated, and  for each allowed client");
    }
    LOG.debug("Creating AllowedClient " + id + "/" + secret + " with allowed domains " + Arrays.toString(domains.toArray()));
  }

  public String getId() { return _id; }
  public String getSecret() { return _secret; }
  public String getSigningKey() { return _signingKey; }
  public Set<String> getDomains() { return _domains; }

}
