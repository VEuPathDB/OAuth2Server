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
    clientDomains,
    allowUserManagement,
    allowROPCGrant,
    allowGuestObtainment;
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
    boolean allowUserManagement = json.getBoolean(JsonKey.allowUserManagement.name(), false);
    boolean allowROPCGrant = json.getBoolean(JsonKey.allowROPCGrant.name(), false);
    boolean allowGuestObtainment = json.getBoolean(JsonKey.allowGuestObtainment.name(), false);
    return new AllowedClient(clientId, clientSecret, signingKey, domainList, allowUserManagement, allowROPCGrant, allowGuestObtainment);
  }

  private final String _id;
  private final String _secret;
  private final String _signingKey;
  private final Set<String> _domains;
  private final boolean _allowUserManagement;
  private final boolean _allowROPCGrant;
  private final boolean _allowGuestObtainment;

  public AllowedClient(String id, String secret, String signingKey, Set<String> domains, boolean allowUserManagement, boolean allowROPCGrant, boolean allowGuestObtainment) throws InitializationException {
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
    _allowUserManagement = allowUserManagement;
    _allowROPCGrant = allowROPCGrant;
    _allowGuestObtainment = allowGuestObtainment;
    LOG.debug("Creating AllowedClient " + id + "/" + secret + " with allowed domains " + Arrays.toString(domains.toArray()));
  }

  public String getId() { return _id; }
  public String getSecret() { return _secret; }
  public String getSigningKey() { return _signingKey; }
  public Set<String> getDomains() { return _domains; }
  public boolean allowUserManagement() { return _allowUserManagement; }
  public boolean allowROPCGrant() { return _allowROPCGrant; }
  public boolean allowGuestObtainment() { return _allowGuestObtainment; }

}
