package org.gusdb.oauth2.exception;

public class CryptoException extends Exception {

  public CryptoException(String message) {
    super(message);
  }

  public CryptoException(String message, Exception cause) {
    super(message, cause);
  }

}
