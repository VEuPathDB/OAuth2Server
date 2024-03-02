package org.gusdb.oauth2.service;

import java.util.HashMap;

import javax.json.JsonObject;

public class UserPropertiesRequest extends HashMap<String,String> {

  private String _email;

  public UserPropertiesRequest(JsonObject jsonObject) {
    _email = jsonObject.getString("email");
    for (String key : jsonObject.keySet()) {
      put(key, jsonObject.getString(key));
    }
  }

  public String getEmail() {
    return _email;
  }

  public void setEmail(String email) {
    _email = email;
  }

}
