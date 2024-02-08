package org.gusdb.oauth2.client.veupathdb;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.gusdb.oauth2.shared.IdTokenFields;
import org.json.JSONObject;

/**
 * Represents a VEupathDB user
 *
 * @author rdoherty
 */
public class BasicUser implements User {

  public static final Map<String,UserProperty> USER_PROPERTIES = createUserPropertyDefs();

  private static Map<String,UserProperty> createUserPropertyDefs() {
    List<UserProperty> userProps = List.of(
        new UserProperty("username", "Username", "username", false, false, false, BasicUser::getUsername, BasicUser::setUsername),
        new UserProperty("firstName", "First Name", "first_name", true, true, false, BasicUser::getFirstName, BasicUser::setFirstName),
        new UserProperty("middleName", "Middle Name", "middle_name", false, true, false, BasicUser::getMiddleName, BasicUser::setMiddleName),
        new UserProperty("lastName", "Last Name", "last_name", true, true, false, BasicUser::getLastName, BasicUser::setLastName),
        new UserProperty("organization", "Organization", "organization", true, true, false, BasicUser::getOrganization, BasicUser::setOrganization),
        new UserProperty("interests", "Interests", "interests", false, false, true, BasicUser::getInterests, BasicUser::setInterests)
    );
    return Collections.unmodifiableMap(userProps.stream().collect(Collectors.toMap(UserProperty::getName, x -> x)));
  }

  // immutable fields supplied by bearer token
  private final long _userId;
  private final boolean _isGuest;
  private final String _signature;
  private final String _stableId;

  // mutable fields that may need to be fetched
  private String _email; // standard; not a user property
  private String _username;
  private String _firstName;
  private String _middleName;
  private String _lastName;
  private String _organization;
  private String _interests;

  public BasicUser(long userId, boolean isGuest, String signature, String stableId) {
    _userId = userId;
    _isGuest = isGuest;
    _signature = signature;
    _stableId = stableId;
  }

  public BasicUser(JSONObject json) {
    this(
        Long.valueOf(json.getString(IdTokenFields.sub.name())),
        json.getBoolean(IdTokenFields.is_guest.name()),
        json.getString(IdTokenFields.signature.name()),
        json.getString(IdTokenFields.preferred_username.name())
    );
    setPropertyValues(json);
  }

  @Override
  public void setPropertyValues(JSONObject json) {
    for (UserProperty userProp : BasicUser.USER_PROPERTIES.values()) {
      userProp.setValue(this, json.optString(userProp.getName(), null));
    }
  }

  @Override
  public long getUserId() {
    return _userId;
  }

  @Override
  public boolean isGuest() {
    return _isGuest;
  }

  @Override
  public String getSignature() {
    return _signature;
  }

  @Override
  public String getStableId() {
    return _stableId;
  }

  protected void fetchUserInfo() {
    // nothing to do in this base class; all info must be explicitly set
  }

  @Override
  public String getEmail() {
    fetchUserInfo();
    return _email;
  }

  @Override
  public BasicUser setEmail(String email) {
    _email = email;
    return this;
  }

  @Override
  public String getUsername() {
    fetchUserInfo();
    return _username;
  }

  @Override
  public BasicUser setUsername(String username) {
    _username = username;
    return this;
  }

  @Override
  public String getFirstName() {
    fetchUserInfo();
    return _firstName;
  }

  @Override
  public BasicUser setFirstName(String firstName) {
    _firstName = firstName;
    return this;
  }

  @Override
  public String getMiddleName() {
    fetchUserInfo();
    return _middleName;
  }

  @Override
  public BasicUser setMiddleName(String middleName) {
    _middleName = middleName;
    return this;
  }

  @Override
  public String getLastName() {
    fetchUserInfo();
    return _lastName;
  }

  @Override
  public BasicUser setLastName(String lastName) {
    _lastName = lastName;
    return this;
  }

  @Override
  public String getOrganization() {
    fetchUserInfo();
    return _organization;
  }

  @Override
  public BasicUser setOrganization(String organization) {
    _organization = organization;
    return this;
  }

  @Override
  public String getInterests() {
    fetchUserInfo();
    return _interests;
  }

  @Override
  public BasicUser setInterests(String interests) {
    _interests = interests;
    return this;
  }

  /**
   * Provides a "pretty" display name for this user
   * 
   * @return display name for this user
   */
  @Override
  public String getDisplayName() {
    return isGuest() ? "Guest User" : (
        formatNamePart(getFirstName()) +
        formatNamePart(getMiddleName()) +
        formatNamePart(getLastName())).trim();
  }

  private static String formatNamePart(String namePart) {
    return (namePart == null || namePart.isEmpty() ? "" : " " + namePart.trim());
  }

  @Override
  public String toString() {
    return "User #" + getUserId() + " - " + getEmail();
  }

  @Override
  public int hashCode() {
    return String.valueOf(getUserId()).hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof BasicUser)) {
      return false;
    }
    return getUserId() == ((BasicUser)obj).getUserId();
  }

}
