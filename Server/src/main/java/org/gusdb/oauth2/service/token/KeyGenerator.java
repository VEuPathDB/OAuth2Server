package org.gusdb.oauth2.service.token;

import java.security.KeyPair;
import java.util.List;

import io.jsonwebtoken.security.Keys;

public class KeyGenerator {

  private static final String SECRET_KEY_ALG_NAME = Signatures.SECRET_KEY_ALGORITHM.getJcaName();
  private static final String ASYMMETRIC_KEY_ALG_NAME = Signatures.ASYMMETRIC_KEY_ALGORITHM.getJcaName();

  private static final List<String> ALLOWED_ALGORITHMS = List.of(SECRET_KEY_ALG_NAME, ASYMMETRIC_KEY_ALG_NAME);

  public static void main(String[] args) {
    
    if (args.length != 1 || !ALLOWED_ALGORITHMS.contains(args[0])) {
      System.err.println("USAGE: java " + KeyGenerator.class.getName() + " [ " + String.join(", ", ALLOWED_ALGORITHMS) + " ]");
      System.exit(1);
    }

    if (SECRET_KEY_ALG_NAME.equals(args[0])) {
      System.out.println(Keys.secretKeyFor(Signatures.SECRET_KEY_ALGORITHM));
    }
    else {
      KeyPair keys = Keys.keyPairFor(Signatures.ASYMMETRIC_KEY_ALGORITHM);
      System.out.println("Private:\n" + keys.getPrivate());
      System.out.println("Public:\n" + keys.getPublic());
    }
  }
}
