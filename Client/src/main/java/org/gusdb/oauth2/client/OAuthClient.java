package org.gusdb.oauth2.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.gusdb.oauth2.client.KeyStoreTrustManager.KeyStoreConfig;
import org.gusdb.oauth2.shared.token.ECPublicKeyRepresentation;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

public class OAuthClient {

  private static final Logger LOG = LogManager.getLogger(OAuthClient.class);

  private static final String NL = System.lineSeparator();

  public enum TokenType {
    AUTH,
    BEARER;
  }

  public static TrustManager getTrustManager(KeyStoreConfig config) {
    String keyStoreFile = config.getKeyStoreFile();
    return (keyStoreFile.isEmpty() ? new KeyStoreTrustManager() :
        new KeyStoreTrustManager(Paths.get(keyStoreFile), config.getKeyStorePassPhrase()));
  }

  public interface ValidatedToken {
    TokenType getTokenType();
    String getTokenValue();
    Claims getTokenContents();
  }

  // private "constructor" of the interface above
  private static ValidatedToken newValidatedToken(TokenType type, String tokenValue, Claims claims) {
    return new ValidatedToken() {
      @Override public TokenType getTokenType()  { return type;       }
      @Override public String getTokenValue()    { return tokenValue; }
      @Override public Claims getTokenContents() { return claims;     }
    };
  }

  // manages SSL certs needed to connect to OAuth server (SSL required)
  private final TrustManager _trustManager;

  public OAuthClient(TrustManager trustManager) {
    _trustManager = trustManager;
  }

  private String getPublicSigningKey(String oauthBaseUrl) {
    String jwksEndpoint = oauthBaseUrl + Endpoints.JWKS;
    try {

      // get JWKS response from OAuth server
      Response response = ClientBuilder.newBuilder()
          .withConfig(new ClientConfig())
          .sslContext(createSslContext())
          .build()
          .target(jwksEndpoint)
          .request(MediaType.APPLICATION_JSON)
          .get();

      // check for successful processing
      if (response.getStatus() != 200) {
        String responseBody = !response.hasEntity() ? "<empty>" : readResponseBody((InputStream)response.getEntity());
        throw new RuntimeException("Failure to get JWKS information.  GET " + jwksEndpoint + " returned " + response.getStatus() + " with body: " + responseBody);
      }

      // parse response and find elliptic curve public key
      JSONObject jwksJson = new JSONObject(readResponseBody((InputStream)response.getEntity()));
      return findECPublicKeyValue(jwksJson);

    }
    catch (KeyManagementException | NoSuchAlgorithmException | JSONException | IOException e) {
      throw new RuntimeException("Unable to retrieve public signing key from OAuth service at " + jwksEndpoint, e);
    }
  }

  private static String findECPublicKeyValue(JSONObject jwksJson) {
    JSONArray keys = jwksJson.getJSONArray("keys");
    for (int i = 0; i < keys.length(); i++) {
      JSONObject keyDef = keys.getJSONObject(i);
      if (keyDef.getString("kty").equals("EC")) {
        // specific to our OAuth implementation; typically EC key returns
        //   x and y coords, which we also do, but this is easier to interpret
        return keyDef.getString("k");
      }
    }
    throw new RuntimeException("Unable to find EC key information in JWKS response: " + jwksJson.toString(2));
  }

  private String readResponseBody(InputStream entity) throws IOException {
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    entity.transferTo(body);
    return body.toString();
  }

  public ValidatedToken getValidatedAuthToken(OAuthConfig oauthConfig, String authCode, String redirectUri) {

    // get legacy token, signed with HMAC using client secret as the key
    String token = getTokenFromAuthCode(Endpoints.AUTH_TOKEN, oauthConfig, authCode, redirectUri);
    String key = oauthConfig.getOauthClientSecret();

    // encode the key as a base64 string
    byte[] unencodedKey = key.getBytes(StandardCharsets.UTF_8);
    String encodedKeyStr = Base64.getEncoder().encodeToString(unencodedKey);

    // convert the key back to bytes
    byte[] keyBytes = Base64.getDecoder().decode(encodedKeyStr);

    // verify signature and create claims object
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(keyBytes)
        .build()
        .parseClaimsJws(token)
        .getBody();

    validateClaims(claims);

    return newValidatedToken(TokenType.AUTH, token, claims);
  }

  public ValidatedToken getValidatedBearerToken(OAuthConfig oauthConfig, String authCode, String redirectUri) {

    // get bearer token, signed with ECDSA using public/private key pair
    String token = getTokenFromAuthCode(Endpoints.BEARER_TOKEN, oauthConfig, authCode, redirectUri);
    String key = getPublicSigningKey(oauthConfig.getOauthUrl());

    // convert the key string to a public key object
    PublicKey publicKey = new ECPublicKeyRepresentation(key).getPublicKey();

    // verify signature and create claims object
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(publicKey)
        .build()
        .parseClaimsJws(token)
        .getBody();

    validateClaims(claims);

    return newValidatedToken(TokenType.BEARER, token, claims);
  }

  private void validateClaims(Claims claims) {
    // TODO: add validation of claims
    //claims.getIssuer()
    //claims.getAudience()
    //claims.getExpiration()
  }

  private String getTokenFromAuthCode(String path, OAuthConfig oauthConfig, String authCode, String redirectUri) {
    try {
      String oauthUrl = oauthConfig.getOauthUrl() + path;

      // build form parameters for token request
      MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
      formData.add("grant_type", "authorization_code");
      formData.add("code", authCode);
      formData.add("redirect_uri", redirectUri);
      formData.add("client_id", oauthConfig.getOauthClientId());
      formData.add("client_secret", oauthConfig.getOauthClientSecret());

      LOG.info("Building token request with the following URL: " + oauthUrl +
          " and params: " + dumpMultiMap(formData));

      // build request and get token response
      Response response = ClientBuilder.newBuilder()
          .withConfig(new ClientConfig())
          .sslContext(createSslContext())
          .build()
          .target(oauthUrl)
          .request(MediaType.APPLICATION_JSON)
          .post(Entity.form(formData));
  
      if (response.getStatus() == 200) {
        // Success!  Read result into buffer and convert to JSON
        InputStream resultStream = (InputStream)response.getEntity();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        resultStream.transferTo(buffer);

        
        JSONObject json = new JSONObject(new String(buffer.toByteArray()));
        LOG.debug("Response received from OAuth server for token request: " + json.toString(2));

        // get id_token from object and decode to user ID
        return json.getString("id_token");
      }
      else {
        // Failure; throw exception
        throw new RuntimeException("OAuth2 token request failed with status " +
            response.getStatus() + ": " + response.getStatusInfo().getReasonPhrase() + NL + response.getEntity());
      }
    }
    catch(Exception e) {
      throw new RuntimeException("Unable to complete OAuth token request to fetch user id", e);
    }
  }

  private SSLContext createSslContext() throws NoSuchAlgorithmException, KeyManagementException {
    SSLContext sslContext = SSLContext.getInstance("SSL");
    sslContext.init(null, new TrustManager[]{ _trustManager }, null);
    return sslContext;
  }

  private static String dumpMultiMap(MultivaluedMap<String, String> formData) {
    StringBuilder str = new StringBuilder("{").append(NL);
    for (Entry<String,List<String>> entry : formData.entrySet()) {
      str.append("  ").append(entry.getKey()).append(": ")
         .append("[ ").append(entry.getValue().stream().collect(Collectors.joining(", "))).append(" ]").append(NL);
    }
    return str.append("}").append(NL).toString();
  }

  public ValidatedToken getValidatedAuthToken(OAuthConfig _config, String email, String password, String redirectUrl) {
    // TODO Auto-generated method stub
    return null;
  }

  public ValidatedToken getValidatedBearerToken(OAuthConfig _config, String email, String password, String redirectUrl) {
    // TODO Auto-generated method stub
    return null;
  }

  private static String getAuthorizationHeaderValue(ValidatedToken token) {
    if (token.getTokenType() != TokenType.BEARER) {
      throw new RuntimeException("User info and edit endpoints require a user's bearer token (legacy auth tokens are not supported).");
    }
    return "Bearer " + token.getTokenValue();
  }

  public static String getTokenFromAuthHeader(String authHeader) {
    Objects.requireNonNull(authHeader);
    String prefix = "Bearer ";
    authHeader = authHeader.trim();
    if (!authHeader.startsWith(prefix)) {
      throw new NotAuthorizedException("Authentication header must send token of type '" + prefix.trim() + "'");
    }
    return authHeader.substring(0, prefix.length());
  }

  public JSONObject getUserData(OAuthConfig oauthConfig, ValidatedToken token) {
    try {
      String userEndpoint = oauthConfig.getOauthUrl() + Endpoints.USER_INFO;

      // build request and get token response
      Response response = ClientBuilder.newBuilder()
          .withConfig(new ClientConfig())
          .sslContext(createSslContext())
          .build()
          .target(userEndpoint)
          .request(MediaType.APPLICATION_JSON)
          .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeaderValue(token))
          .get();

      if (response.getStatus() != 200) {
        throw new RuntimeException("Unable to retrieve user info from OAuth server.  GET " + userEndpoint + " returned " + response.getStatus());
      }

      ByteArrayOutputStream body = new ByteArrayOutputStream();
      ((InputStream)response.getEntity()).transferTo(body);
      return new JSONObject(body.toString(StandardCharsets.UTF_8));
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to retrieve user info from OAuth server", e);
    }
  }
}
