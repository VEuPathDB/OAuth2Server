package org.gusdb.oauth2.client.veupathdb;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.gusdb.oauth2.client.veupathdb.UserProperty.InputType;
import org.json.JSONObject;

public interface User {

  public static final Map<String,UserProperty> USER_PROPERTIES = createUserPropertyDefs();

  static final String USERNAME_HELP = "You are able to log in with this value or your email.";
  static final String ORGANIZATION_HELP = "Please use the full name of your academic institution, company, or foundation.";
  static final String GROUP_NAME_HELP = "Please use the official name of your group or lab or the full name of your primary investigator.";
  static final String ORGANIZATION_SUGGEST = "e.g. University of Pennsylvania";
  static final String GROUP_NAME_SUGGEST = "e.g. Charles Darwin Lab";
  static final String NO_VALUE = null;

  private static Map<String,UserProperty> createUserPropertyDefs() {
    List<UserProperty> userProps = List.of(
        new UserProperty("username", "Username", USERNAME_HELP, NO_VALUE, "username", false, false, InputType.TEXT, User::getUsername, User::setUsername),
        new UserProperty("firstName", "First Name", NO_VALUE, NO_VALUE, "first_name", true, true, InputType.TEXT, User::getFirstName, User::setFirstName),
        new UserProperty("middleName", "Middle Name", NO_VALUE, NO_VALUE, "middle_name", false, true, InputType.TEXT, User::getMiddleName, User::setMiddleName),
        new UserProperty("lastName", "Last Name", NO_VALUE, NO_VALUE, "last_name", true, true, InputType.TEXT, User::getLastName, User::setLastName),
        new UserProperty("country", "Country", NO_VALUE, NO_VALUE, "country", true, true, InputType.SELECT, User::getCountry, User::setCountry),
        new UserProperty("organization", "Organization", ORGANIZATION_HELP, ORGANIZATION_SUGGEST, "organization", true, true, InputType.TEXT, User::getOrganization, User::setOrganization),
        new UserProperty("groupName", "Group Name", GROUP_NAME_HELP, GROUP_NAME_SUGGEST, "group_name", true, true, InputType.TEXT, User::getGroupName, User::setGroupName),
        new UserProperty("position", "Primary Position", NO_VALUE, NO_VALUE, "position", true, true, InputType.SELECT, User::getPosition, User::setPosition),
        new UserProperty("positionType", "Position Type", NO_VALUE, NO_VALUE, "position_type", true, true, InputType.SELECT, User::getPositionType, User::setPositionType),
        new UserProperty("interests", "Interests", NO_VALUE, NO_VALUE, "interests", false, false, InputType.TEXTBOX, User::getInterests, User::setInterests)
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

  String getCountry();
  User setCountry(String country);

  String getOrganization();
  User setOrganization(String organization);

  String getGroupName();
  User setGroupName(String lab);

  String getPosition();
  User setPosition(String position);

  String getPositionType();
  User setPositionType(String positionType);

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
