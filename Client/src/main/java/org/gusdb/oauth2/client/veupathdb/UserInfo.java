package org.gusdb.oauth2.client.veupathdb;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.gusdb.oauth2.client.veupathdb.UserProperty.InputType;
import org.json.JSONObject;

/**
 * Represents a VEupathDB user's information.  However, cannot be used to act
 * on the user's behalf since it does not contain an authentication token.
 *
 * @author rdoherty
 */
public interface UserInfo {

  public static final int MAX_EMAIL_LENGTH = 255;
  public static final int MAX_PROPERTY_VALUE_SIZE = 4000;

  public static final Map<String,UserProperty> USER_PROPERTIES = createUserPropertyDefs();

  static final String USERNAME_HELP = "You are able to log in with this value or your email.";
  static final String ORGANIZATION_HELP = "Please use the full name of your academic institution, company, or foundation.";
  static final String GROUP_NAME_HELP = "Please use the official name of your group or lab or the full name of your primary investigator.";
  static final String ORGANIZATION_SUGGEST = "e.g. University of Pennsylvania";
  static final String GROUP_NAME_SUGGEST = "e.g. Jane Doe Lab";
  static final String NO_VALUE = null;

  private static Map<String,UserProperty> createUserPropertyDefs() {
    List<UserProperty> userProps = List.of(
        new UserProperty("username", "Username", USERNAME_HELP, NO_VALUE, "username", false, false, InputType.TEXT, UserInfo::getUsername, UserInfo::setUsername),
        new UserProperty("firstName", "First Name", NO_VALUE, NO_VALUE, "first_name", true, true, InputType.TEXT, UserInfo::getFirstName, UserInfo::setFirstName),
        new UserProperty("middleName", "Middle Name", NO_VALUE, NO_VALUE, "middle_name", false, true, InputType.TEXT, UserInfo::getMiddleName, UserInfo::setMiddleName),
        new UserProperty("lastName", "Last Name", NO_VALUE, NO_VALUE, "last_name", true, true, InputType.TEXT, UserInfo::getLastName, UserInfo::setLastName),
        new UserProperty("country", "Country", NO_VALUE, NO_VALUE, "country", true, true, InputType.SELECT, UserInfo::getCountry, UserInfo::setCountry),
        new UserProperty("organization", "Organization Name", ORGANIZATION_HELP, ORGANIZATION_SUGGEST, "organization", true, true, InputType.TEXT, UserInfo::getOrganization, UserInfo::setOrganization),
        new UserProperty("organizationType", "Organization Type", NO_VALUE, NO_VALUE, "organization_type", true, true, InputType.SELECT, UserInfo::getOrganizationType, UserInfo::setOrganizationType),
        new UserProperty("position", "Primary Position", NO_VALUE, NO_VALUE, "position", true, true, InputType.SELECT, UserInfo::getPosition, UserInfo::setPosition),
        new UserProperty("groupName", "Group Name", GROUP_NAME_HELP, GROUP_NAME_SUGGEST, "group_name", true, true, InputType.TEXT, UserInfo::getGroupName, UserInfo::setGroupName),
        new UserProperty("interests", "Interests", NO_VALUE, NO_VALUE, "interests", false, false, InputType.TEXTBOX, UserInfo::getInterests, UserInfo::setInterests)
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
  UserInfo setEmail(String email);

  String getUsername();
  UserInfo setUsername(String username);

  String getFirstName();
  UserInfo setFirstName(String firstName);

  String getMiddleName();
  UserInfo setMiddleName(String middleName);

  String getLastName();
  UserInfo setLastName(String lastName);

  String getCountry();
  UserInfo setCountry(String country);

  String getOrganization();
  UserInfo setOrganization(String organization);

  String getGroupName();
  UserInfo setGroupName(String groupName);

  String getPosition();
  UserInfo setPosition(String position);

  String getOrganizationType();
  UserInfo setOrganizationType(String organizationType);

  String getInterests();
  UserInfo setInterests(String interests);

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
