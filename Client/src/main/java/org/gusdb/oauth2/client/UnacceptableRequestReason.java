package org.gusdb.oauth2.client;

import java.util.Optional;

public enum UnacceptableRequestReason {
  UNSUPPORTED_GRANT_TYPE("Unsupported Grant Type"),
  INVALID_USERNAME_PASSWORD("Invalid Username/Password"),
  INVALID_CLIENT("Invalid Client"),
  INVALID_AUTH_CODE("Invalid Auth Code"),
  UNSUPPORTED_RESPONSE_TYPE("Unsupported Response Type"),
  INVALID_REDIRECT_URI("Invalid Redirect URI in Request");

  private final String _display;

  private UnacceptableRequestReason(String display) {
    _display = display;
  }

  public String getDisplay() {
    return _display;
  }

  public static Optional<UnacceptableRequestReason> parse(String reasonString) {
    try {
      return Optional.of(valueOf(reasonString));
    }
    catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
