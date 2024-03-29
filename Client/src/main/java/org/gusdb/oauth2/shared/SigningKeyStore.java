package org.gusdb.oauth2.shared;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.crypto.SecretKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.oauth2.exception.CryptoException;

import io.jsonwebtoken.security.WeakKeyException;

public class SigningKeyStore {

  private static final Logger LOG = LogManager.getLogger(SigningKeyStore.class);

  // async keys for bearer tokens
  private final KeyPair _asyncKeys;

  // maps client IDs -> client secrets -> SecretKey objects representing those secrets
  private final Map<String,Map<String,SecretKey>> _clientSecretKeys = new HashMap<>();

  // secret key format (for JWKS)
  private String _secretKeyFormat;

  public SigningKeyStore(String asyncKeysRandomSeed) throws CryptoException {
    _asyncKeys = Signatures.getKeyPair(asyncKeysRandomSeed);
  }

  public SigningKeyStore(KeyPair asyncKeys) {
    _asyncKeys = asyncKeys;
  }

  public void setClientSigningKeys(String clientId, Set<String> rawSigningKeys) throws CryptoException {
    Map<String,SecretKey> secretMap = new HashMap<>();
    for (String rawSigningKey : rawSigningKeys) {
      try {
        SecretKey signingKey = Signatures.getValidatedSecretKey(rawSigningKey);
        secretMap.put(rawSigningKey, signingKey);
        if (_secretKeyFormat == null) {
          _secretKeyFormat = signingKey.getFormat();
        }
      }
      catch (WeakKeyException e) {
        String message = "Raw HMAC signing key '" + rawSigningKey + "' for client '" + clientId + "' is too weak for the algorithm.";
        LOG.error(message, e);
        throw new CryptoException(message + " " + e.getMessage());
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
