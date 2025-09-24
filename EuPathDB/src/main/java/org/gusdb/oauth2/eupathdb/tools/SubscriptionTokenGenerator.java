package org.gusdb.oauth2.eupathdb.tools;

import org.gusdb.oauth2.eupathdb.AccountDbAuthenticator;

public class SubscriptionTokenGenerator {

  private static final int SUBSCRIPTION_TOKEN_SIZE = 10;

  public static void main(String[] args) {
    System.out.println(AccountDbAuthenticator.generateRandomChars(SUBSCRIPTION_TOKEN_SIZE));
  }
}
