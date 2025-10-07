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

  static final String USERNAME_HELP = "You are able to log in with this value or your email.";
  static final String ORGANIZATION_HELP = "Please use the full name of your academic institution, company, or foundation.";
  static final String GROUP_NAME_HELP = "Please use the official name of your group or lab or the full name of your primary investigator.";
  static final String SUBSCRIPTION_TOKEN_HELP = "Enter your group's subscription token to become a subscribing user.";
  static final String ORGANIZATION_SUGGEST = "e.g. University of Pennsylvania";
  static final String GROUP_NAME_SUGGEST = "e.g. Jane Doe Lab, Professor Jones Bioinformatics 101";
  static final String NO_VALUE = null;

  public static final String FIRST_NAME_PROP_KEY = "first_name";
  public static final String LAST_NAME_PROP_KEY = "last_name";

  // define valid user properties and their attributes
  public static final List<UserProperty> USER_PROPERTY_LIST = List.of(
      new UserProperty("username", "Username", USERNAME_HELP, NO_VALUE, "username", false, false, InputType.TEXT, UserInfo::getUsername, UserInfo::setUsername),
      new UserProperty("firstName", "First name", NO_VALUE, NO_VALUE, FIRST_NAME_PROP_KEY, true, true, InputType.TEXT, UserInfo::getFirstName, UserInfo::setFirstName),
      new UserProperty("middleName", "Middle name", NO_VALUE, NO_VALUE, "middle_name", false, true, InputType.TEXT, UserInfo::getMiddleName, UserInfo::setMiddleName),
      new UserProperty("lastName", "Last name", NO_VALUE, NO_VALUE, LAST_NAME_PROP_KEY, true, true, InputType.TEXT, UserInfo::getLastName, UserInfo::setLastName),
      new UserProperty("country", "Country", NO_VALUE, NO_VALUE, "country", true, true, InputType.SELECT, UserInfo::getCountry, UserInfo::setCountry),
      new UserProperty("organization", "Organization name", ORGANIZATION_HELP, ORGANIZATION_SUGGEST, "organization", true, true, InputType.TEXT, UserInfo::getOrganization, UserInfo::setOrganization),
      new UserProperty("organizationType", "Organization type", NO_VALUE, NO_VALUE, "organization_type", true, true, InputType.SELECT, UserInfo::getOrganizationType, UserInfo::setOrganizationType),
      new UserProperty("position", "Primary position", NO_VALUE, NO_VALUE, "position", true, true, InputType.SELECT, UserInfo::getPosition, UserInfo::setPosition),
      new UserProperty("groupName", "Group name", GROUP_NAME_HELP, GROUP_NAME_SUGGEST, "group_name", true, true, InputType.TEXT, UserInfo::getGroupName, UserInfo::setGroupName),
      new UserProperty("groupType", "Group type", NO_VALUE, NO_VALUE, "group_type", false, true, InputType.SELECT, UserInfo::getGroupType, UserInfo::setGroupType),
      new UserProperty("subscriptionToken", "Subscription token", SUBSCRIPTION_TOKEN_HELP, NO_VALUE, "subscription_token", false, false, InputType.TEXT, UserInfo::getSubscriptionToken, UserInfo::setSubscriptionToken),
      new UserProperty("interests", "Interests", NO_VALUE, NO_VALUE, "interests", false, false, InputType.TEXTBOX, UserInfo::getInterests, UserInfo::setInterests)
  );

  // convert user properties to a map keyed on property name (JSON key)
  public static final Map<String,UserProperty> USER_PROPERTIES = Collections.unmodifiableMap(
      USER_PROPERTY_LIST.stream().collect(
          Collectors.toMap(UserProperty::getName, x -> x, (a,b) -> a, () -> new LinkedHashMap<>())));

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

  String getGroupType();
  UserInfo setGroupType(String groupType);

  String getSubscriptionToken();
  UserInfo setSubscriptionToken(String subscriptionToken);

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
