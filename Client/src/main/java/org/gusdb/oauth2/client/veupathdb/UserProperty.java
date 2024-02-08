package org.gusdb.oauth2.client.veupathdb;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class UserProperty {

  private String _name;
  private String _displayName;
  private String _dbKey;
  private boolean _isMultiLine = false;
  private boolean _isRequired = false;
  private boolean _isPublic = true;

  private final Function<BasicUser,String> _getter;
  private final BiConsumer<BasicUser,String> _setter;

  public UserProperty(String name, String displayName, String dbKey, boolean isRequired, boolean isPublic, boolean isMultiLine, Function<BasicUser,String> getter, BiConsumer<BasicUser,String> setter) {
    _name = name;
    _dbKey = dbKey;
    _isRequired = isRequired;
    _displayName = displayName;
    _isPublic = isPublic;
    _isMultiLine = isMultiLine;
    _getter = getter;
    _setter = setter;
  }

  public String getValue(BasicUser user) {
    return _getter.apply(user);
  }

  public void setValue(BasicUser user, String value) {
    _setter.accept(user, value);
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

