package org.gusdb.oauth2.service.token;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;

import org.apache.logging.log4j.LogManager;
import org.gusdb.oauth2.InitializationException;
import org.gusdb.oauth2.config.ApplicationConfig;
import org.gusdb.oauth2.service.token.TokenStore.AccessTokenData;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.EllipticCurveProvider;
import io.jsonwebtoken.security.Keys;

public class Signatures {

  public static final SignatureAlgorithm SECRET_KEY_ALGORITHM = SignatureAlgorithm.HS512;
  public static final SignatureAlgorithm ASYMMETRIC_KEY_ALGORITHM = SignatureAlgorithm.ES512;

  public static final TokenSigner SECRET_KEY_SIGNER = Signatures::createJwtFromJsonHmac;
  public static final TokenSigner ASYMMETRIC_KEY_SIGNER = Signatures::createJwtFromJsonEcdsa;

  private static final int MINIMUM_SEED_LENGTH = 16;

  public interface TokenSigner {
    /**
     * Takes a JSON object representing a JWT and produces a JWS (i.e. signed JWT).  The
     * signing strategy decides how to procure a key from the passed config and token data.
     *
     * @param tokenJson raw JSON that constitutes the JWT (claims as properties)
     * @param config configuration of the application
     * @param tokenData data associated with the token 
     * @return
     */
    String getSignedEncodedToken(JsonObject tokenJson, ApplicationConfig config, AccessTokenData tokenData);
  }

  /**
   * Converts a configured secret key string into a validated SecretKey object
   * that can be used to sign tokens for our chosen secret key algorithm.
   *
   * @param secretKey raw text secret key
   * @return validated key object created from the input
   * @throws InitializationException 
   */
  public static SecretKey getValidatedSecretKey(String secretKey) throws InitializationException {
    SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    if (!key.getAlgorithm().equals(SECRET_KEY_ALGORITHM.getJcaName())) {
      throw new InitializationException("Signing key '" + secretKey + "' is not compatible with " + SECRET_KEY_ALGORITHM.getJcaName());
    }
    return key;
  }

  public static KeyPair getKeyPair(String seed) throws InitializationException {
    if (seed == null || seed.length() < MINIMUM_SEED_LENGTH) {
      throw new InitializationException("Asynchronous key pair generation seed must be present and greater than " + MINIMUM_SEED_LENGTH + " characters");
    }
    SecureRandom random = new SecureRandom(seed.getBytes(StandardCharsets.UTF_8));
    return EllipticCurveProvider.generateKeyPair(ASYMMETRIC_KEY_ALGORITHM, random);
  }

  private static String createJwtFromJsonHmac(JsonObject tokenJson, ApplicationConfig config, AccessTokenData tokenData) {
    Key key = config.getKeyMap().get(tokenData.authCodeData.clientId);
    log("Will create JWT using HMAC from the following token body: " + prettyPrintJson(tokenJson));
    String jwt = Jwts.builder()
        .setPayload(tokenJson.toString())
        .signWith(key)
        .compact();
    log("JWT created: " + jwt);
    return jwt;
  }

  private static String createJwtFromJsonEcdsa(JsonObject tokenJson, ApplicationConfig config, AccessTokenData tokenData) {
    PrivateKey privateKey = config.getAsyncKeys().getPrivate();
    log("Will create JWT using ECDSA from the following token body: " + prettyPrintJson(tokenJson));
    String jwt = Jwts.builder()
        .setPayload(tokenJson.toString())
        .signWith(privateKey)
        .compact();
    log("JWT created: " + jwt);
    return jwt;
  }

  private static void log(String message) {
    LogManager.getLogger(Signatures.class).debug(message);
  }

  private static String prettyPrintJson(JsonStructure json) {
    Map<String, Boolean> config = new HashMap<>();
    config.put(JsonGenerator.PRETTY_PRINTING, true);
    StringWriter writer = new StringWriter();
    try (JsonWriter jsonWriter = Json.createWriterFactory(config).createWriter(writer)) {
      jsonWriter.write(json);
    }
    return writer.toString();
  }

  public static JsonObject getJwksContent(ApplicationConfig applicationConfig) {
    return Json.createObjectBuilder()
      .add("keys", Json.createArrayBuilder()
        .add(Json.createObjectBuilder()
          .add("kty", "RSA")
          .add("n", "<your_client_secret>")
          .add("alg", Signatures.SECRET_KEY_ALGORITHM.getValue())
          .add("kid", "0")
          .build())
        .add(Json.createObjectBuilder()
          .add("kty", "RSA")
          .add("n", "<your_client_secret>")
          .add("alg", Signatures.ASYMMETRIC_KEY_ALGORITHM.getValue())
          .add("kid", "0")
          .build())
        .build())
      .build();
  }
}
