package org.gusdb.oauth2.client.veupathdb;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;

public interface User {

  public static final Map<String,UserProperty> USER_PROPERTIES = createUserPropertyDefs();

  private static Map<String,UserProperty> createUserPropertyDefs() {
    List<UserProperty> userProps = List.of(
        new UserProperty("username", "Username", "username", false, false, false, User::getUsername, User::setUsername),
        new UserProperty("firstName", "First Name", "first_name", true, true, false, User::getFirstName, User::setFirstName),
        new UserProperty("middleName", "Middle Name", "middle_name", false, true, false, User::getMiddleName, User::setMiddleName),
        new UserProperty("lastName", "Last Name", "last_name", true, true, false, User::getLastName, User::setLastName),
        new UserProperty("organization", "Organization", "organization", true, true, false, User::getOrganization, User::setOrganization),
        new UserProperty("interests", "Interests", "interests", false, false, true, User::getInterests, User::setInterests)
    );
    return Collections.unmodifiableMap(userProps.stream().collect(Collectors.toMap(UserProperty::getName, x -> x)));
  }

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
