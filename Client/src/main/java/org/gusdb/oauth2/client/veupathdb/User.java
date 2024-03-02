package org.gusdb.oauth2.client.veupathdb;

import java.util.Collections;
import java.util.LinkedHashMap;
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
    return Collections.unmodifiableMap(userProps.stream().collect(Collectors.toMap(UserProperty::getName, x -> x, (a,b) -> a, () -> new LinkedHashMap<>())));
  }

  void setPropertyValues(JSONObject json);
  void setPropertyValues(Map<String,String> propertyValues);

  long getUserId();
  boolean isGuest();
  String getSignature();
  String getStableId();

  String getEmail();
  User setEmail(String email);

  String getUsername();
  User setUsername(String username);

  String getFirstName();
  User setFirstName(String firstName);

  String getMiddleName();
  User setMiddleName(String middleName);

  String getLastName();
  User setLastName(String lastName);

  String getOrganization();
  User setOrganization(String organization);

  String getInterests();
  User setInterests(String interests);

  /**
   * Provides a "pretty" display name for this user
   * 
   * @return display name for this user
   */
  default String getDisplayName() {
    return isGuest() ? "Guest User" : formatDisplayName(getFirstName(), getMiddleName(), getLastName());
  }

  static String formatDisplayName(String firstName, String middleName, String lastName) {
    return (
      formatNamePart(firstName) +
      formatNamePart(middleName) +
      formatNamePart(lastName)
    ).trim();
  }

  private static String formatNamePart(String namePart) {
    return (namePart == null || namePart.isEmpty() ? "" : " " + namePart.trim());
  }

}
