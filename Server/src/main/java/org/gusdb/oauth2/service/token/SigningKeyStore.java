package org.gusdb.oauth2.service.token;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.gusdb.oauth2.InitializationException;

public class SigningKeyStore {

  private final KeyPair _asyncKeys;
  private final Map<String,SecretKey> _clientSecretKeys;

  public SigningKeyStore(String asyncKeysRandomSeed) throws InitializationException {
    _asyncKeys = Signatures.getKeyPair(asyncKeysRandomSeed);
    _clientSecretKeys = new HashMap<>();
  }

  public void addClientSigningKey(String clientId, String rawSigningKey) throws InitializationException {
    SecretKey signingKey = Signatures.getValidatedSecretKey(rawSigningKey);
    _clientSecretKeys.put(clientId, signingKey);
  }

  public KeyPair getAsyncKeys() {
    return _asyncKeys;
  }

  public SecretKey getSecretKey(String clientId) {
    return _clientSecretKeys.get(clientId);
  }

  public SecretKey getFirstSecretKey() {
    return _clientSecretKeys.values().stream().findAny().orElseThrow();
  }

}
