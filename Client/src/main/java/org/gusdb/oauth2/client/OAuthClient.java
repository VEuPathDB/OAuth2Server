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
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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
    ID,
    BEARER;
  }

  public interface ValidatedToken {
    public static ValidatedToken build(TokenType type, String tokenValue, Claims claims) {
      return new ValidatedToken() {
        @Override public TokenType getTokenType()  { return type;       }
        @Override public String getTokenValue()    { return tokenValue; }
        @Override public Claims getTokenContents() { return claims;     }
      };
    }
    TokenType getTokenType();
    String getTokenValue();
    Claims getTokenContents();
  }

  public static String getTokenFromAuthHeader(String authHeader) {
    Objects.requireNonNull(authHeader);
    String prefix = "Bearer ";
    authHeader = authHeader.trim();
    if (!authHeader.startsWith(prefix)) {
      throw new NotAuthorizedException(HttpHeaders.AUTHORIZATION + " header must send token of type '" + prefix.trim() + "'");
    }
    return authHeader.substring(0, prefix.length());
  }

  public static TrustManager getTrustManager(KeyStoreConfig config) {
    String keyStoreFile = config.getKeyStoreFile();
    return (keyStoreFile.isEmpty() ? new KeyStoreTrustManager() :
        new KeyStoreTrustManager(Paths.get(keyStoreFile), config.getKeyStorePassPhrase()));
  }

  // manages SSL certs needed to connect to OAuth server (SSL required)
  private final TrustManager _trustManager;

  public OAuthClient(TrustManager trustManager) {
    _trustManager = trustManager;
  }

  private String getPublicSigningKey(String oauthBaseUrl) {
    String jwksEndpoint = oauthBaseUrl + Endpoints.JWKS;

    // get JWKS response from OAuth server
    try (Response response = ClientBuilder.newBuilder()
          .withConfig(new ClientConfig())
          .sslContext(createSslContext())
          .build()
          .target(jwksEndpoint)
          .request(MediaType.APPLICATION_JSON)
          .get()) {

      // check for successful processing
      if (response.getStatus() != 200) {
        String responseBody = !response.hasEntity() ? "<empty>" : readResponseBody(response);
        throw new RuntimeException("Failure to get JWKS information.  GET " + jwksEndpoint + " returned " + response.getStatus() + " with body: " + responseBody);
      }

      // parse response and find elliptic curve public key
      JSONObject jwksJson = new JSONObject(readResponseBody(response));
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

  private String readResponseBody(Response response) throws IOException {
    InputStream entity = (InputStream)response.getEntity();
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    entity.transferTo(body);
    return body.toString(StandardCharsets.UTF_8);
  }

  public Consumer<MultivaluedMap<String, String>> getAuthCodeFormModifier(String authCode) {
    return formData -> {
      formData.add("grant_type", "authorization_code");
      formData.add("code", authCode);
    };
  }

  public ValidatedToken getIdTokenFromAuthCode(OAuthConfig oauthConfig, String authCode, String redirectUri) {

    // get legacy token, signed with HMAC using client secret as the key
    String token = getToken(Endpoints.ID_TOKEN, getAuthCodeFormModifier(authCode), oauthConfig, redirectUri);

    // validate signature and return parsed claims
    return getValidatedHmacSignedToken(oauthConfig, token);
  }

  public ValidatedToken getBearerTokenFromAuthCode(OAuthConfig oauthConfig, String authCode, String redirectUri) {

    // get bearer token, signed with ECDSA using public/private key pair
    String token = getToken(Endpoints.BEARER_TOKEN, getAuthCodeFormModifier(authCode), oauthConfig, redirectUri);

    // validate signature and return parsed claims
    return getValidatedEcdsaSignedToken(oauthConfig, token);
  }

  public Consumer<MultivaluedMap<String, String>> getUserPassFormModifier(String username, String password) {
    return formData -> {
      formData.add("grant_type", "password");
      formData.add("username", username);
      formData.add("password", password);
    };
  }

  public ValidatedToken getIdTokenFromUsernamePassword(OAuthConfig oauthConfig, String username, String password, String redirectUri) {

    // get legacy token, signed with HMAC using client secret as the key
    String token = getToken(Endpoints.ID_TOKEN, getUserPassFormModifier(username, password), oauthConfig, redirectUri);

    // validate signature and return parsed claims
    return getValidatedHmacSignedToken(oauthConfig, token);
  }

  public ValidatedToken getBearerTokenFromUsernamePassword(OAuthConfig oauthConfig, String username, String password, String redirectUri) {

    // get bearer token, signed with ECDSA using public/private key pair
    String token = getToken(Endpoints.BEARER_TOKEN, getUserPassFormModifier(username, password), oauthConfig, redirectUri);

    // validate signature and return parsed claims
    return getValidatedEcdsaSignedToken(oauthConfig, token);
  }

  private String getToken(String path, Consumer<MultivaluedMap<String, String>> formModifier, OAuthConfig oauthConfig, String redirectUri) {

    String oauthUrl = oauthConfig.getOauthUrl() + path;

    // build form parameters for token request
    MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
    formData.add("redirect_uri", redirectUri);
    formData.add("client_id", oauthConfig.getOauthClientId());
    formData.add("client_secret", oauthConfig.getOauthClientSecret());

    // add custom form params for this grant type
    formModifier.accept(formData);

    LOG.info("Building token request with the following URL: " + oauthUrl +
        " and params: " + dumpMultiMap(formData));

    // build request and get token response
    try (Response response = ClientBuilder.newBuilder()
          .withConfig(new ClientConfig())
          .sslContext(createSslContext())
          .build()
          .target(oauthUrl)
          .request(MediaType.APPLICATION_JSON)
          .post(Entity.form(formData))) {
  
      if (response.getStatus() == 200) {
        // Success!  Read result into buffer and convert to JSON
        JSONObject json = new JSONObject(readResponseBody(response));
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

  public ValidatedToken getValidatedHmacSignedToken(OAuthConfig oauthConfig, String token) {

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

    return ValidatedToken.build(TokenType.ID, token, claims);
  }

  public ValidatedToken getValidatedEcdsaSignedToken(OAuthConfig oauthConfig, String token) {

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

    return ValidatedToken.build(TokenType.BEARER, token, claims);
  }

  private void validateClaims(Claims claims) {
    // TODO: add validation of claims
    //claims.getIssuer()
    //claims.getAudience()
    //claims.getExpiration()
  }

  private SSLContext createSslContext() throws NoSuchAlgorithmException, KeyManagementException {
    SSLContext sslContext = SSLContext.getInstance("SSL");
    sslContext.init(null, new TrustManager[]{ _trustManager }, null);
    return sslContext;
  }

  private static String dumpMultiMap(MultivaluedMap<String, String> formData) {
    StringBuilder str = new StringBuilder("{").append(NL);
    for (Entry<String,List<String>> entry : formData.entrySet()) {
      String value = entry.getKey().equals("password") ? "<hidden>" :
        entry.getValue().stream().collect(Collectors.joining(", "));
      str.append("  ").append(entry.getKey()).append(": ")
         .append("[ ").append(value).append(" ]").append(NL);
    }
    return str.append("}").append(NL).toString();
  }

  private static String getAuthorizationHeaderValue(ValidatedToken token) {
    if (token.getTokenType() != TokenType.BEARER) {
      throw new RuntimeException("User info and edit endpoints require a user's bearer token (legacy auth tokens are not supported).");
    }
    return "Bearer " + token.getTokenValue();
  }

  public JSONObject getUserData(OAuthConfig oauthConfig, ValidatedToken token) {
    String userEndpoint = oauthConfig.getOauthUrl() + Endpoints.USER_INFO;
    // build request and get JSON response
    try (Response response = ClientBuilder.newBuilder()
          .withConfig(new ClientConfig())
          .sslContext(createSslContext())
          .build()
          .target(userEndpoint)
          .request(MediaType.APPLICATION_JSON)
          .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeaderValue(token))
          .get()) {

      if (response.getStatus() == 200) {
        return new JSONObject(readResponseBody(response));
      }

      // otherwise request failed
      throw new RuntimeException("Unable to retrieve user info from OAuth server.  GET " + userEndpoint + " returned " + response.getStatus());

    }
    catch (Exception e) {
      throw new RuntimeException("Unable to retrieve user info from OAuth server", e);
    }
  }

  public JSONObject createNewUser(OAuthConfig oauthConfig, Map<String,String> userProperties) throws IllegalArgumentException {
    return performUserOperation(
        Endpoints.USER_CREATE,
        oauthConfig,
        json -> json.put("user", userProperties),
        (builder,entity) -> builder.post(entity)
    );
  }

  public JSONObject modifyUser(OAuthConfig oauthConfig, ValidatedToken token, Map<String,String> userProperties) {
    return performUserOperation(
        Endpoints.USER_EDIT,
        oauthConfig,
        json -> json.put("user",  userProperties),
        (builder,entity) -> builder
          .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeaderValue(token))
          .put(entity)
    );
  }

  public JSONObject resetPassword(OAuthConfig oauthConfig, String loginName) throws IllegalArgumentException {
    return performUserOperation(
        Endpoints.PASSWORD_RESET,
        oauthConfig,
        json -> json.put("loginName",  loginName),
        (builder,entity) -> builder.post(entity)
    );
  }

  private JSONObject performUserOperation(String endpoint, OAuthConfig oauthConfig,
      Function<JSONObject,JSONObject> jsonModifier, BiFunction<Invocation.Builder,Entity<String>,Response> responseSupplier) {

    String userEndpoint = oauthConfig.getOauthUrl() + endpoint;

    JSONObject initialJson = new JSONObject()
        .put("clientCredentials", new JSONObject()
            .put("clientId", oauthConfig.getOauthClientId())
            .put("clientSecret", oauthConfig.getOauthClientSecret()));

    String requestJson = jsonModifier.apply(initialJson).toString();

    // build request and get JSON response
    try (Response response = responseSupplier.apply(
        ClientBuilder.newBuilder()
          .withConfig(new ClientConfig())
          .sslContext(createSslContext())
          .build()
          .target(userEndpoint)
          .request(MediaType.APPLICATION_JSON),
         Entity.entity(requestJson, MediaType.APPLICATION_JSON))) {

      // return new user's user info object
      if (response.getStatus() == 200) {
        return new JSONObject(readResponseBody(response));
      }

      // check for input validation issues
      if (response.getStatusInfo().getFamily().equals(Status.Family.CLIENT_ERROR)) {
        throw new IllegalArgumentException(readResponseBody(response));
      }

      // else server error
      throw new RuntimeException("Unable to perform user operation on OAuth server.  GET " + userEndpoint + " returned " + response.getStatus());
      
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to perform user operation on OAuth server", e);
    }
  }

}
