package org.gusdb.oauth2.config;

import java.util.Set;

public class AllowedClient {

  private String _id;
  private String _secret;
  private Set<String> _domains;

  public AllowedClient(String id, String secret, Set<String> domains) {
    _id = id;
    _secret = secret;
    _domains = domains;
  }

  public String getId() { return _id; }
  public String getSecret() { return _secret; }
  public Set<String> getDomains() { return _domains; }

}
