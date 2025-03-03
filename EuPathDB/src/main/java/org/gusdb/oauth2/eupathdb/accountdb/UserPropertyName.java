package org.gusdb.oauth2.eupathdb.accountdb;

public class UserPropertyName {

  private String _name;
  private String _displayName;
  private String _dbKey;
  private boolean _isMultiLine = false;
  private boolean _isRequired = false;
  private boolean _isPublic = true;

  public UserPropertyName() { }

  public UserPropertyName(String name, String dbKey, boolean isRequired) {
    _name = name;
    _dbKey = dbKey;
    _isRequired = isRequired;
  }

  public void setName(String name) { _name = name; }
  public void setDisplayName(String displayName) { _displayName = displayName; }
  public void setDbKey(String dbKey) { _dbKey = dbKey; }
  public void setRequired(boolean isRequired) { _isRequired = isRequired; }
  public void setMultiLine(boolean isMultiLine) { _isMultiLine = isMultiLine; }
  public void setPublic(boolean isPublic) { _isPublic = isPublic; }

  public String getName() { return _name; }
  public String getDisplayName() { return _displayName; }
  public String getDbKey() { return _dbKey; }
  public boolean isRequired() { return _isRequired; }
  public boolean isMultiLine() { return _isMultiLine; }
  public boolean isPublic() { return _isPublic; }

}
