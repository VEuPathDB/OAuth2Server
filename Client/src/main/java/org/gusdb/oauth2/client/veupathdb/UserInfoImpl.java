package org.gusdb.oauth2.client.veupathdb;

import java.util.Map;

import org.gusdb.oauth2.shared.IdTokenFields;
import org.json.JSONObject;

/**
 * Represents a VEupathDB user's information.  However, cannot be used to act
 * on the user's behalf since it does not contain an authentication token.
 *
 * @author rdoherty
 */
public class UserInfoImpl implements UserInfo {

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
  private String _country;
  private String _organization;
  private String _groupName;
  private String _subscriptionToken;
  private String _position;
  private String _organizationType;
  private String _interests;

  public UserInfoImpl(long userId, boolean isGuest, String signature, String stableId) {
    _userId = userId;
    _isGuest = isGuest;
    _signature = signature;
    _stableId = stableId;
  }

  public UserInfoImpl(JSONObject json) {
    this(
        Long.valueOf(json.getString(IdTokenFields.sub.name())),
        json.getBoolean(IdTokenFields.is_guest.name()),
        json.getString(IdTokenFields.signature.name()),
        json.getString(IdTokenFields.preferred_username.name())
    );
    setPropertyValues(json);
  }

  public UserInfoImpl(UserInfo user) {
    this(user.getUserId(), user.isGuest(), user.getSignature(), user.getStableId());
    setEmail(user.getEmail());
    for (UserProperty prop : USER_PROPERTIES.values()) {
      prop.setValue(this, prop.getValue(user));
    }
  }

  @Override
  public void setPropertyValues(JSONObject userInfo) {
    // set email (standard property but mutable so set on user profile and not token
    if (userInfo.has(IdTokenFields.email.name())) {
      setEmail(userInfo.getString(IdTokenFields.email.name()));
    }
    // set other user properties found only on user profile object
    for (UserProperty userProp : USER_PROPERTIES.values()) {
      userProp.setValue(this, userInfo.optString(userProp.getName(), null));
    }
  }

  @Override
  public void setPropertyValues(Map<String, String> propertyValues) {
    // set email (standard property but mutable so set on user profile and not token
    if (propertyValues.containsKey(IdTokenFields.email.name())) {
      setEmail(propertyValues.get(IdTokenFields.email.name()));
    }
    // set other user properties found only on user profile object
    for (UserProperty userProp : USER_PROPERTIES.values()) {
      userProp.setValue(this, propertyValues.get(userProp.getName()));
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
  public UserInfoImpl setEmail(String email) {
    _email = email;
    return this;
  }

  @Override
  public String getUsername() {
    fetchUserInfo();
    return _username;
  }

  @Override
  public UserInfoImpl setUsername(String username) {
    _username = username;
    return this;
  }

  @Override
  public String getFirstName() {
    fetchUserInfo();
    return _firstName;
  }

  @Override
  public UserInfoImpl setFirstName(String firstName) {
    _firstName = firstName;
    return this;
  }

  @Override
  public String getMiddleName() {
    fetchUserInfo();
    return _middleName;
  }

  @Override
  public UserInfoImpl setMiddleName(String middleName) {
    _middleName = middleName;
    return this;
  }

  @Override
  public String getLastName() {
    fetchUserInfo();
    return _lastName;
  }

  @Override
  public UserInfoImpl setLastName(String lastName) {
    _lastName = lastName;
    return this;
  }

  @Override
  public String getCountry() {
    return _country;
  }

  @Override
  public UserInfoImpl setCountry(String country) {
    _country = country;
    return this;
  }

  @Override
  public String getOrganization() {
    fetchUserInfo();
    return _organization;
  }

  @Override
  public UserInfoImpl setOrganization(String organization) {
    _organization = organization;
    return this;
  }

  @Override
  public String getGroupName() {
    return _groupName;
  }

  @Override
  public UserInfoImpl setGroupName(String groupName) {
    _groupName = groupName;
    return this;
  }

  @Override
  public String getSubscriptionToken() {
    return _subscriptionToken;
  }

  @Override
  public UserInfo setSubscriptionToken(String subscriptionToken) {
    _subscriptionToken = subscriptionToken;
    return this;
  }

  @Override
  public String getPosition() {
    return _position;
  }

  @Override
  public UserInfoImpl setPosition(String position) {
    _position = position;
    return this;
  }

  @Override
  public String getOrganizationType() {
    return _organizationType;
  }

  @Override
  public UserInfoImpl setOrganizationType(String organizationType) {
    _organizationType = organizationType;
    return this;
  }

  @Override
  public String getInterests() {
    fetchUserInfo();
    return _interests;
  }

  @Override
  public UserInfoImpl setInterests(String interests) {
    _interests = interests;
    return this;
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
    if (!(obj instanceof UserInfoImpl)) {
      return false;
    }
    return getUserId() == ((UserInfoImpl)obj).getUserId();
  }

}
