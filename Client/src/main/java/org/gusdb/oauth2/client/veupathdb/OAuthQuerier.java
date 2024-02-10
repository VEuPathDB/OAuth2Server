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
    JSONArray response = new JSONArray(client.queryOAuth(config, new JSONObject().put("emails", new JSONArray(emails))));
    Map<String, T> userMap = new HashMap<>();
    for (int i = 0; i < response.length(); i++) {
      JSONObject userJson = response.getJSONObject(i);
      T user = userConverter.apply(userJson);
      userMap.put(user.getEmail(), user);
    }
    return userMap;
  }

  public static Map<Long, User> getUsersById(OAuthClient client, OAuthConfig config, List<Long> userIds) {
    return getUsersById(client, config, userIds, BasicUser::new);
  }

  public static <T extends User> Map<Long, T> getUsersById(OAuthClient client, OAuthConfig config, List<Long> userIds, Function<JSONObject, T> userConverter) {
    JSONArray response = new JSONArray(client.queryOAuth(config, new JSONObject().put("userIds", new JSONArray(userIds))));
    Map<Long, T> userMap = new HashMap<>();
    for (int i = 0; i < response.length(); i++) {
      JSONObject userJson = response.getJSONObject(i);
      T user = userConverter.apply(userJson);
      userMap.put(user.getUserId(), user);
    }
    return userMap;
  }
}
