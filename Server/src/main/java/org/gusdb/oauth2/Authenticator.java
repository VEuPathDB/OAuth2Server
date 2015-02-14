package org.gusdb.oauth2;

import javax.json.JsonObject;

public interface Authenticator {

  public void initialize(JsonObject configJson) throws InitializationException;
  public boolean isCredentialsValid(String username, String password) throws Exception;
  public JsonObject getUserInfo(String username) throws Exception;
  public void close();

}
