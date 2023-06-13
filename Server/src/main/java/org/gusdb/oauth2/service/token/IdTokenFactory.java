package org.gusdb.oauth2.service.token;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.gusdb.oauth2.Authenticator;
import org.gusdb.oauth2.Authenticator.UserInfo;
import org.gusdb.oauth2.service.token.TokenStore.AccessTokenData;

public class IdTokenFactory {

  private static final Logger LOG = LogManager.getLogger(IdTokenFactory.class);

  public static enum IdTokenFields {
    iss, // issuer of this token
    sub, // subject (unique user ID)
    aud, // audience (client ID of consumer)
    azp, // authorized parth (same as aud in our case)
    auth_time, // time of authentication (Unix integer seconds)
    iat, // time of issuance (Unix integer seconds)
    exp, // time of expiration (Unix integer seconds)
    nonce, // string value linking original auth request with ID token
    email, // user's email
    email_verified, // whether email is verified
    preferred_username; // human-friendly display name for the user (may or may not be unique/stable)

    public static Set<String> getNames() {
      Set<String> names = new HashSet<>();
      for (IdTokenFields val : values()) {
        names.add(val.name());
      }
      return names;
    }
  }

  public static JsonObject createIdTokenJson(Authenticator authenticator,
      AccessTokenData tokenData, String issuer, int expirationSecs)
          throws OAuthProblemException, OAuthSystemException {

    // OpenID Connect claims that we support
    long now = new Date().getTime() / 1000;
    JsonObjectBuilder jsonBuilder = Json.createObjectBuilder()
      .add(IdTokenFields.iss.name(), issuer)
      .add(IdTokenFields.aud.name(), tokenData.authCodeData.clientId)
      .add(IdTokenFields.azp.name(), tokenData.authCodeData.clientId)
      .add(IdTokenFields.auth_time.name(), tokenData.authCodeData.creationTime)
      .add(IdTokenFields.iat.name(), now)
      .add(IdTokenFields.exp.name(), now + expirationSecs);

    // add nonce if client sent as part of original authentication request
    String nonce = tokenData.authCodeData.nonce;
    if (nonce != null && !nonce.isEmpty()) {
      jsonBuilder.add(IdTokenFields.nonce.name(), nonce);
    }

    // get values from authenticator and use to populate remaining fields
    UserInfo user = getUserInfo(authenticator, tokenData.authCodeData.username);
    String userId = user.getUserId();
    if (userId == null || userId.isEmpty())
      throw OAuthProblemException.error("Authenticator returned null or empty " +
          "user ID for username [" + tokenData.authCodeData.username + "].");
    jsonBuilder.add(IdTokenFields.sub.name(), userId);

    // add user's email if returned by Authenticator
    String email = user.getEmail();
    if (email != null && !email.isEmpty()) {
      jsonBuilder
        .add(IdTokenFields.email.name(), email)
        .add(IdTokenFields.email_verified.name(), user.isEmailVerified());
    }

    // add user's preferred_username if returned by Authenticator
    String preferredUsername = user.getPreferredUsername();
    if (preferredUsername != null && !preferredUsername.isEmpty()) {
      jsonBuilder
        .add(IdTokenFields.preferred_username.name(), preferredUsername);
    }

    // add any supplemental fields from Authenticator
    Map<String, JsonValue> extra = user.getSupplementalFields();
    Set<String> reservedKeys = IdTokenFields.getNames();
    for (Entry<String,JsonValue> entry : extra.entrySet()) {
      if (reservedKeys.contains(entry.getKey())) {
        LOG.warn("Authenticator tried to override ID token property [" + entry.getKey() + "]. Skipping...");
      }
      else {
        jsonBuilder.add(entry.getKey(), entry.getValue());
      }
    }

    return jsonBuilder.build();
  }

  private static UserInfo getUserInfo(Authenticator authenticator, String username) throws OAuthSystemException {
    try {
      return authenticator.getUserInfo(username);
    }
    catch (Exception e) {
      LOG.error("Unable to retrieve user info for usernaem '" + username + "'", e);
      throw new OAuthSystemException(e);
    }
  }

}
