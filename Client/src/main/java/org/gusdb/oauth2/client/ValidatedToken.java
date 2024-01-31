package org.gusdb.oauth2.client;

import org.gusdb.oauth2.shared.IdTokenFields;

import io.jsonwebtoken.Claims;

public interface ValidatedToken {

  /**
   * Indicates how the token was acquired (token vs bearer token endpoint), which determines how
   * it was signed.  This is important for verification of the token and also limits its use.
   * For example, ID tokens cannot safely be passed to arbitrary services since, to verify them,
   * the service must know the client secret with which it was signed.  Bearer tokens are more
   * flexible.
   */
  public enum TokenType {
    ID,
    BEARER;
  }

  /**
   * Build a new immutable ValidatedToken object from the passed parameters
   *
   * @param type type of token
   * @param tokenValue 
   * @param claims
   * @return
   */
  public static ValidatedToken build(TokenType type, String tokenValue, Claims claims) {
    return new ValidatedToken() {
      @Override public TokenType getTokenType()  { return type;       }
      @Override public String getTokenValue()    { return tokenValue; }
      @Override public Claims getTokenContents() { return claims;     }
    };
  }

  TokenType getTokenType();
  String getTokenValue();
  Claims getTokenContents();

  /**
   * @return ID 
   */
  default String getUserId() {
    return getTokenContents().get(IdTokenFields.sub.name(), String.class);
  }

  default boolean isGuest() {
    return getTokenContents().get(IdTokenFields.is_guest.name(), Boolean.class);
  }

}
