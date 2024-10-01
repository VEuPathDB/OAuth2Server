package org.gusdb.oauth2.client.veupathdb;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class UserProperty {

  public static enum InputType {
    TEXT,
    TEXTBOX,
    SELECT;
  }

  private final String _name;
  private final String _displayName;
  private final String _helpText;
  private final String _suggest;
  private final String _dbKey;
  private final InputType _inputType;
  private final boolean _isRequired;
  private final boolean _isPublic;

  private final Function<User,String> _getter;
  private final BiConsumer<User,String> _setter;

  public UserProperty(String name, String displayName, String helpText, String suggest, String dbKey, boolean isRequired, boolean isPublic, InputType inputType, Function<User,String> getter, BiConsumer<User,String> setter) {
    _name = name;
    _dbKey = dbKey;
    _isRequired = isRequired;
    _displayName = displayName;
    _helpText = helpText;
    _suggest = suggest;
    _isPublic = isPublic;
    _inputType = inputType;
    _getter = getter;
    _setter = setter;
  }

  public String getValue(User user) {
    return _getter.apply(user);
  }

  public void setValue(User user, String value) {
    _setter.accept(user, value);
  }

  public String getName() { return _name; }
  public String getDisplayName() { return _displayName; }
  public String getHelpText() { return _helpText; }
  public String getSuggest() { return _suggest; }
  public String getDbKey() { return _dbKey; }
  public boolean isRequired() { return _isRequired; }
  public InputType getInputType() { return _inputType; }
  public boolean isPublic() { return _isPublic; }

}

