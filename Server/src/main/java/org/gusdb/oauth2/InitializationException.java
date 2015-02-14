package org.gusdb.oauth2;

public class InitializationException extends Exception {

  private static final long serialVersionUID = 1L;

  public InitializationException(String message) {
    super(message);
  }

  public InitializationException(String message, Exception cause) {
    super(message, cause);
  }
}
