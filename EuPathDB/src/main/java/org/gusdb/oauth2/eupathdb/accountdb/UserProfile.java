package org.gusdb.oauth2.eupathdb.accountdb;

import static org.gusdb.fgputil.FormatUtil.NL;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

public class UserProfile {

  public static final int MAX_EMAIL_LENGTH = 255;
  public static final int MAX_PROPERTY_VALUE_SIZE = 4000;

  private Long _userId;
  private String _email;
  private boolean _isGuest;
  private String _signature;
  private String _stableId;
  private Date _registerTime;
  private Date _lastLoginTime;
  private Map<String, String> _properties;

  public Long getUserId() {
    return _userId;
  }
  public void setUserId(Long userId) {
    _userId = userId;
  }
  public String getEmail() {
    return _email;
  }
  public void setEmail(String email) {
    _email = email;
  }
  public boolean isGuest() {
    return _isGuest;
  }
  public void setGuest(boolean isGuest) {
    _isGuest = isGuest;
  }
  public String getSignature() {
    return _signature;
  }
  public void setSignature(String signature) {
    _signature = signature;
  }
  public String getStableId() {
    return _stableId;
  }
  public void setStableId(String stableId) {
    _stableId = stableId;
  }
  public Date getRegisterTime() {
    return _registerTime;
  }
  public void setRegisterTime(Date registerTime) {
    _registerTime = registerTime;
  }
  public Date getLastLoginTime() {
    return _lastLoginTime;
  }
  public void setLastLoginTime(Date lastLoginTime) {
    _lastLoginTime = lastLoginTime;
  }
  public Map<String, String> getProperties() {
    return _properties;
  }
  public void setProperties(Map<String, String> properties) {
    _properties = properties;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("User {").append(NL)
        .append("  userId:        ").append(_userId).append(NL)
        .append("  email:         ").append(_email).append(NL)
        .append("  isGuest:       ").append(_isGuest).append(NL)
        .append("  signature:     ").append(_signature).append(NL)
        .append("  stableId:      ").append(_stableId).append(NL)
        .append("  registerTime:  ").append(_registerTime).append(NL)
        .append("  lastLoginTime: ").append(_lastLoginTime).append(NL)
        .append("  properties: {").append(NL);
    for (Entry<String,String> prop : _properties.entrySet()) {
      sb.append("    ").append(prop.getKey()).append(": ").append(prop.getValue()).append(NL);
    }
    return sb.append("  }").append(NL).append("}").append(NL).toString();
  }
}
