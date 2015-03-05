package org.gusdb.oauth2.config;

import java.util.Arrays;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllowedClient {

  private static final Logger LOG = LoggerFactory.getLogger(AllowedClient.class);
  
  private String _id;
  private String _secret;
  private Set<String> _domains;

  public AllowedClient(String id, String secret, Set<String> domains) {
    _id = id;
    _secret = secret;
    _domains = domains;
    LOG.debug("Creating AllowedClient " + id + "/" + secret + " with allowed domains " + Arrays.toString(domains.toArray()));
  }

  public String getId() { return _id; }
  public String getSecret() { return _secret; }
  public Set<String> getDomains() { return _domains; }

}
