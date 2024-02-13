package org.gusdb.oauth2.client.veupathdb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.gusdb.oauth2.client.OAuthClient;
import org.gusdb.oauth2.client.OAuthConfig;
import org.json.JSONArray;
import org.json.JSONObject;

public class OAuthQuerier {

  public static Map<String, User> getUsersByEmail(OAuthClient client, OAuthConfig config, List<String> emails) {
    return getUsersByEmail(client, config, emails, BasicUser::new);
  }

  public static <T extends User> Map<String, T> getUsersByEmail(OAuthClient client, OAuthConfig config, List<String> emails, Function<JSONObject, T> userConverter) {
    return getUsers(client, config, emails, userConverter, "emails", u -> u.getEmail());
  }

  public static Map<Long, User> getUsersById(OAuthClient client, OAuthConfig config, List<Long> userIds) {
    return getUsersById(client, config, userIds, BasicUser::new);
  }

  public static <T extends User> Map<Long, T> getUsersById(OAuthClient client, OAuthConfig config, List<Long> userIds, Function<JSONObject, T> userConverter) {
    return getUsers(client, config, userIds, userConverter, "userIds", u -> u.getUserId());
  }

  private static <T extends User, S> Map<S, T> getUsers(OAuthClient client, OAuthConfig config, List<S> identifiers,
      Function<JSONObject, T> userConverter, String identifiersJsonPropKey, Function<T,S> keyGenerator) {
    JSONArray response = new JSONArray(client.queryOAuth(config, new JSONObject().put("emails", new JSONArray(identifiers))));
    Map<S, T> userMap = new HashMap<>();
    for (int i = 0; i < response.length(); i++) {
      JSONObject userJson = response.getJSONObject(i);
      T user = userJson.getBoolean("found") ? userConverter.apply(userJson) : null;
      userMap.put(keyGenerator.apply(user), user);
    }
    return userMap;
  }
}
