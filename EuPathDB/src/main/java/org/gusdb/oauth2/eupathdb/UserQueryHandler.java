package org.gusdb.oauth2.eupathdb;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;

import org.gusdb.fgputil.accountdb.AccountManager;
import org.gusdb.fgputil.accountdb.UserProfile;
import org.gusdb.oauth2.Authenticator.DataScope;
import org.gusdb.oauth2.UserInfo;
import org.gusdb.oauth2.service.OAuthRequestHandler;
import org.gusdb.oauth2.shared.IdTokenFields;

public class UserQueryHandler {

  private enum ValidQueryTypePropKey { userId, userIds, email, emails }

  private static final List<String> VALID_PROPS = Arrays
      .stream(ValidQueryTypePropKey.values())
      .map(ValidQueryTypePropKey::name)
      .collect(Collectors.toList());

  private static final String FOUND_KEY = "found";

  private final AccountDbAuthenticator _authenticator;
  private final AccountManager _accountDb;

  public UserQueryHandler(AccountDbAuthenticator authenticator, AccountManager accountDb) {
    _authenticator = authenticator;
    _accountDb = accountDb;
  }

  public JsonValue handleQuery(JsonObject querySpec) {
    try {
      // validate query and find query type
      String submittedQueryType;
      if (querySpec.size() != 1 || !VALID_PROPS.contains(submittedQueryType = querySpec.keySet().iterator().next())) {
        throw new IllegalArgumentException("Query must contain exactly one of [" + String.join(", ", VALID_PROPS) + "]");
      }

      switch(ValidQueryTypePropKey.valueOf(submittedQueryType)) {

        case userId:
          long requestedUserId = Long.valueOf(querySpec.getInt(ValidQueryTypePropKey.userId.name()));
          return getUserJsonById(requestedUserId);

        case userIds:
          JsonArray idInputArray = querySpec.getJsonArray(ValidQueryTypePropKey.userIds.name());
          JsonArrayBuilder idOutputArray = Json.createArrayBuilder();
          for (int i = 0; i < idInputArray.size(); i++) {
            idOutputArray.add(getUserJsonById(Long.valueOf(idInputArray.getInt(i))));
          }
          return idOutputArray.build();

        case email:
          String requestedEmail = querySpec.getString(ValidQueryTypePropKey.email.name());
          return getUserJsonByEmail(requestedEmail);

        case emails:
          JsonArray emailInputArray = querySpec.getJsonArray(ValidQueryTypePropKey.emails.name());
          JsonArrayBuilder emailOutputArray = Json.createArrayBuilder();
          for (int i = 0; i < emailInputArray.size(); i++) {
            emailOutputArray.add(getUserJsonByEmail(emailInputArray.getString(i)));
          }
          return emailOutputArray.build();

        default:
          throw new IllegalStateException("This should never happen.");
      }
    }
    catch (JsonParsingException e) {
      throw new IllegalArgumentException("Illegal query; " + e.getMessage());
    }
  }

  private JsonValue getUserJsonByEmail(String requestedEmail) {
    // only registered users have email
    UserProfile userProfile = _accountDb.getUserProfileByEmail(requestedEmail);
    if (userProfile == null) {
      return Json.createObjectBuilder()
        .add(IdTokenFields.email.name(), requestedEmail)
        .add(FOUND_KEY, false)
        .build();
    }
    else {
      UserInfo user = _authenticator.getUserInfo(userProfile, DataScope.PROFILE).get();
      return OAuthRequestHandler.getUserInfoResponseJson(user, Optional.empty())
        .add(FOUND_KEY, true)
        .build();
    }
  }

  private JsonObject getUserJsonById(long requestedUserId) {
    // look for registered user first
    UserProfile userProfile = _accountDb.getUserProfile(requestedUserId);
    Optional<UserInfo> userOpt = userProfile == null
      // no registered user found; look for guest
      ? _authenticator.getGuestProfileInfo(String.valueOf(requestedUserId))
      // convert profile to UserInfo
      : _authenticator.getUserInfo(userProfile, DataScope.PROFILE);
    return userOpt
      // found a user with this ID
      .map(user -> OAuthRequestHandler.getUserInfoResponseJson(user, Optional.empty())
        .add(FOUND_KEY, true)
        .build())
      // did not find user of any type
      .orElse(Json.createObjectBuilder()
        .add(IdTokenFields.sub.name(), String.valueOf(requestedUserId))
        .add(FOUND_KEY, false)
        .build());
  }
}
