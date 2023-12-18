package org.gusdb.oauth2.shared.token;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.crypto.SecretKey;

public class SigningKeyStore {

  // async keys for bearer tokens
  private final KeyPair _asyncKeys;

  // maps client IDs -> client secrets -> SecretKey objects representing those secrets
  private final Map<String,Map<String,SecretKey>> _clientSecretKeys;

  // secret key format (for JWKS)
  private String _secretKeyFormat;

  public SigningKeyStore(String asyncKeysRandomSeed) throws CryptoException {
    _asyncKeys = Signatures.getKeyPair(asyncKeysRandomSeed);
    _clientSecretKeys = new HashMap<>();
  }

  public void setClientSigningKeys(String clientId, Set<String> rawSigningKeys) throws CryptoException {
    Map<String,SecretKey> secretMap = new HashMap<>();
    for (String rawSigningKey : rawSigningKeys) {
      SecretKey signingKey = Signatures.getValidatedSecretKey(rawSigningKey);
      secretMap.put(rawSigningKey, signingKey);
      if (_secretKeyFormat == null) {
        _secretKeyFormat = signingKey.getFormat();
      }
    }
    _clientSecretKeys.put(clientId, secretMap);
  }

  public KeyPair getAsyncKeys() {
    return _asyncKeys;
  }

  public SecretKey getSecretKey(String clientId, String clientSecret) {
    return _clientSecretKeys.get(clientId).get(clientSecret);
  }

  public String getSecretKeyFormat() {
    if (_secretKeyFormat == null) {
      throw new IllegalStateException("At least one client must be configured.");
    }
    return _secretKeyFormat;
  }

}
