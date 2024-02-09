package org.gusdb.oauth2.tools;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Random;

import org.gusdb.oauth2.shared.CryptoException;
import org.gusdb.oauth2.shared.ECPublicKeyRepresentation;
import org.gusdb.oauth2.shared.Signatures;
import org.gusdb.oauth2.shared.SigningKeyStore;
import org.gusdb.oauth2.shared.ECPublicKeyRepresentation.ECCoordinateStrings;

import io.jsonwebtoken.security.Keys;

public class KeyGenerator {

  private static final String SECRET_KEY_ALG_NAME = Signatures.SECRET_KEY_ALGORITHM.getJcaName();
  private static final String ASYMMETRIC_KEY_ALG_NAME = Signatures.ASYMMETRIC_KEY_ALGORITHM.getJcaName();

  private static final List<String> ALLOWED_ALGORITHMS = List.of(SECRET_KEY_ALG_NAME, ASYMMETRIC_KEY_ALG_NAME);

  public static void main(String[] args) throws CryptoException {

    if (args.length != 1 || !ALLOWED_ALGORITHMS.contains(args[0])) {
      System.err.println("USAGE: java " + KeyGenerator.class.getName() + " [ " + String.join(", ", ALLOWED_ALGORITHMS) + " ]");
      System.exit(1);
    }

    if (SECRET_KEY_ALG_NAME.equals(args[0])) {
      Key key = Keys.secretKeyFor(Signatures.SECRET_KEY_ALGORITHM);
      System.out.println("HMAC: " + toOutputString(key));
    }
    else {
      // create a random seed for the asymmetric keys and initialize key store
      byte[] randomBytes = new byte[32];
      new Random().nextBytes(randomBytes);
      String randomString = Base64.getEncoder().encodeToString(randomBytes);

      SigningKeyStore keyStore = new SigningKeyStore(randomString);
      System.out.println("EC Private:\n" + toOutputString(keyStore.getAsyncKeys().getPrivate()));
      System.out.println("EC Public:\n" + toOutputString(keyStore.getAsyncKeys().getPublic()));

      ECCoordinateStrings publicKeyCoords = new ECPublicKeyRepresentation((ECPublicKey)keyStore.getAsyncKeys().getPublic()).getCoordinates();
      System.out.println("X Coordinate:\n" + publicKeyCoords.getX());
      System.out.println("Y Coordinate:\n" + publicKeyCoords.getY());
    }
  }

  public static String toOutputString(Key key) {
    return new String(Base64.getEncoder().encode(key.getEncoded()), StandardCharsets.UTF_8);
  }
}
