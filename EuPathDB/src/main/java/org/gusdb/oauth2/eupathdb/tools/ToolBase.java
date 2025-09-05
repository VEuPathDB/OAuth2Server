package org.gusdb.oauth2.eupathdb.tools;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ToolBase {

  private final String[] _requiredProps;
  private final Properties _config;

  public ToolBase(String[] args, String[] requiredProps) {
    _requiredProps = requiredProps;
    if (args.length != 1) usage();
    _config = new Properties();
    try (InputStream in = new FileInputStream(args[0])) {
      _config.load(in);
    }
    catch (IOException e) {
      throw new RuntimeException("Could not load properties file", e);
    }
    for (String prop : requiredProps) {
      findProp(prop);
    }
  }

  protected String findProp(String key) {
    return _config.containsKey(key) ? _config.getProperty(key) : usage();
  }

  private String usage() {
    System.err.println("USAGE: java " + getClass().getName() + " <configFile>");
    System.err.println("   configFile must contain '='-delimited property rows with the following keys: " + String.join(", ", _requiredProps));
    System.exit(1);
    return null;
  }
}
