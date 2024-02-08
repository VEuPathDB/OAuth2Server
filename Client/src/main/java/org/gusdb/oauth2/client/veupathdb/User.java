package org.gusdb.oauth2.client.veupathdb;

import org.json.JSONObject;

public interface User {

  public void setPropertyValues(JSONObject json);

  public long getUserId();
  public boolean isGuest();
  public String getSignature();
  public String getStableId();

  public String getEmail();
  public User setEmail(String email);

  public String getUsername();
  public User setUsername(String username);

  public String getFirstName();
  public User setFirstName(String firstName);

  public String getMiddleName();
  public User setMiddleName(String middleName);

  public String getLastName();
  public User setLastName(String lastName);

  public String getOrganization();
  public User setOrganization(String organization);

  public String getInterests();
  public User setInterests(String interests);

  public String getDisplayName();
}
