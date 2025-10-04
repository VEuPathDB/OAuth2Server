package org.gusdb.oauth2.eupathdb.tools;

import org.gusdb.oauth2.eupathdb.AccountDbAuthenticator;

public class SubscriptionTokenGenerator {

  private static final int SUBSCRIPTION_TOKEN_SIZE = 10;

  public static void main(String[] args) {
    for (int i = 0; i < 17; i++) {
      System.out.println(getNewToken());
    }
  }

  public static String getNewToken() {
    return AccountDbAuthenticator.generateRandomChars(SUBSCRIPTION_TOKEN_SIZE);
  }
}
