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
    clientSecrets,
    clientDomains
  }

  public static AllowedClient createFromJson(JsonObject json) throws InitializationException {
    // get id and secrets
    String clientId = json.getString(JsonKey.clientId.name());
    JsonArray secretsArray = json.getJsonArray(JsonKey.clientSecrets.name());
    Set<String> clientSecrets = new HashSet<>();
    if (secretsArray != null) {
      for (int i = 0; i < secretsArray.size(); i++) {
        clientSecrets.add(secretsArray.getString(i));
      }
    }
    // validate domain list
    JsonArray clientDomains = json.getJsonArray(JsonKey.clientDomains.name());
    Set<String> domainList = new HashSet<>();
    if (clientDomains != null) {
      for (int i = 0; i < clientDomains.size(); i++) {
        domainList.add(clientDomains.getString(i));
      }
    }
    return new AllowedClient(clientId, clientSecrets, domainList);
  }

  private String _id;
  private Set<String> _secrets;
  private Set<String> _domains;

  public AllowedClient(String id, Set<String> secrets, Set<String> domains) throws InitializationException {
    _id = id;
    _secrets = secrets;
    _domains = domains;
    if (_id == null || _id.isEmpty() ||
        _secrets == null || _secrets.isEmpty() ||
        _domains == null || domains.isEmpty() ||
        _domains.iterator().next() == null ||
        _domains.iterator().next().isEmpty()) {
      throw new InitializationException("clientId and clientSecrets must be populated, and at least one domain must exist for each allowed client");
    }
    LOG.debug("Creating AllowedClient " + id + " with allowed domains " + Arrays.toString(domains.toArray()));
  }

  public String getId() { return _id; }
  public Set<String> getSecrets() { return _secrets; }
  public Set<String> getDomains() { return _domains; }

}
