package org.gusdb.oauth2.service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.function.Supplier;

import javax.json.Json;
import javax.json.JsonObject;

import org.gusdb.oauth2.shared.token.CryptoException;
import org.gusdb.oauth2.shared.token.ECPublicKeyRepresentation;
import org.gusdb.oauth2.shared.token.Signatures;
import org.gusdb.oauth2.shared.token.SigningKeyStore;
import org.gusdb.oauth2.shared.token.ECPublicKeyRepresentation.ECCoordinateStrings;
import org.junit.Assert;
import org.junit.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.TextCodec;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.SignatureException;

// suppress deprecation warnings; we know the legacy method is deprecated- that's why we're testing the new method :)
@SuppressWarnings("deprecation")
public class TokenSigningValidationTest {

  // symmetric token config
  private static final String CLIENT_ID = "someClient";
  private static final String CLIENT_SECRET = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";

  // asymmetric token config
  private static final String KEY_PAIR_RANDOM_SEED = "1234567890123456";

  // claims object to be encoded/signed/verified/decoded
  private static final JsonObject DUMMY_CLAIMS = Json.createObjectBuilder().add("sub", "myUserId").build();

  // key store object for signing JWT
  private static final SigningKeyStore KEY_STORE = ((Supplier<SigningKeyStore>)() -> {
    try {
      SigningKeyStore keyStore = new SigningKeyStore(KEY_PAIR_RANDOM_SEED);
      keyStore.addClientSigningKey(CLIENT_ID, CLIENT_SECRET);
      byte[] privateKeyBytes = keyStore.getAsyncKeys().getPrivate().getEncoded();
      byte[] publicKeyBytes = keyStore.getAsyncKeys().getPublic().getEncoded();
      System.out.println("Generated the following key pair in base64:\nprivate\n" +
          new String(Base64.getEncoder().encode(privateKeyBytes), StandardCharsets.UTF_8) + "\npublic\n" +
          new String(Base64.getEncoder().encode(publicKeyBytes), StandardCharsets.UTF_8));
      System.out.println("Generated the following key pair in hex:\nprivate\n" +
          String.format("%040x", new BigInteger(1, privateKeyBytes)) + "\npublic\n" +
          String.format("%040x", new BigInteger(1, publicKeyBytes)));
      return keyStore;
    }
    catch (CryptoException e) {
      throw new RuntimeException(e);
    }
  }).get();

  private static final String BAD_PUBLIC_KEY = ((Supplier<String>)() -> {
    try {
      return Base64.getEncoder().encodeToString(new SigningKeyStore(KEY_PAIR_RANDOM_SEED + 1).getAsyncKeys().getPublic().getEncoded());
    }
    catch (Exception e) { throw new RuntimeException(e); }
  }).get();

  @Test
  public void testLegacySymmetricTokenValidator() throws Exception {

    // create a signed token
    String symmetricToken = Signatures.SECRET_KEY_SIGNER.getSignedEncodedToken(DUMMY_CLAIMS, KEY_STORE, CLIENT_ID);

    // encode the key as a base64 string
    String encodedKey = TextCodec.BASE64.encode(CLIENT_SECRET);
    System.out.println("legacy: " + encodedKey);

    // convert the key back to bytes
    byte[] keyBytes = Decoders.BASE64.decode(encodedKey);

    // verify signature and create claims object
    Claims claims = Jwts.parser()
        .setSigningKey(keyBytes)
        .parseClaimsJws(symmetricToken)
        .getBody();

    // check subject claim
    Assert.assertEquals("myUserId", claims.getSubject());

  }

  @Test
  public void testNewSymmetricTokenValidator() throws Exception {

    // create a signed token
    String symmetricToken = Signatures.SECRET_KEY_SIGNER.getSignedEncodedToken(DUMMY_CLAIMS, KEY_STORE, CLIENT_ID);

    // encode the key as a base64 string
    byte[] unencodedKey = CLIENT_SECRET.getBytes(StandardCharsets.UTF_8);
    String encodedKeyStr = Base64.getEncoder().encodeToString(unencodedKey);
    System.out.println("new   : " + encodedKeyStr);

    // convert the key back to bytes
    byte[] keyBytes = Base64.getDecoder().decode(encodedKeyStr);

    // verify signature and create claims object
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(keyBytes)
        .build()
        .parseClaimsJws(symmetricToken)
        .getBody();

    // check subject claim
    Assert.assertEquals("myUserId", claims.getSubject());

  }

  @Test
  public void testAsymmetricKeyTokenValidator() throws Exception {
    testAsymmetricKeyTokenValidator(false);
  }

  @Test(expected=SignatureException.class)
  public void testAsymmetricKeyTokenValidatorFail() throws Exception {
    testAsymmetricKeyTokenValidator(true);
  }

  public static void testAsymmetricKeyTokenValidator(boolean fail) throws Exception {
    // create a signed token
    String asymmetricToken = Signatures.ASYMMETRIC_KEY_SIGNER.getSignedEncodedToken(DUMMY_CLAIMS, KEY_STORE, CLIENT_ID);
    System.out.println(Signatures.getJwksContent(KEY_STORE));

    // encode the public key for distribution
    String encodedKeyStr = new ECPublicKeyRepresentation((ECPublicKey)KEY_STORE.getAsyncKeys().getPublic()).getBase64String();

    // add salt if failure desired
    if (fail) encodedKeyStr += "af";

    // convert the key string to a public key object
    PublicKey publicKey = new ECPublicKeyRepresentation(fail ? BAD_PUBLIC_KEY : encodedKeyStr).getPublicKey();

    // verify signature and create claims object
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(publicKey)
        .build()
        .parseClaimsJws(asymmetricToken)
        .getBody();

    // check subject claim
    Assert.assertTrue(claims.containsKey("sub"));
    Assert.assertEquals("myUserId", claims.getSubject());

  }

  @Test
  public void testAsymmetricCoordinatesTokenValidator() throws Exception {

    // create a signed token
    String asymmetricToken = Signatures.ASYMMETRIC_KEY_SIGNER.getSignedEncodedToken(DUMMY_CLAIMS, KEY_STORE, CLIENT_ID);
    System.out.println(Signatures.getJwksContent(KEY_STORE));

    // encode the public key into coordinates for distribution
    ECCoordinateStrings publicKeyCoords = new ECPublicKeyRepresentation((ECPublicKey)KEY_STORE.getAsyncKeys().getPublic()).getCoordinates();
    String x = publicKeyCoords.getX();
    String y = publicKeyCoords.getY();

    // convert the coordinate strings ito a public key object
    PublicKey publicKey = new ECPublicKeyRepresentation(x, y).getPublicKey();

    // verify signature and create claims object
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(publicKey)
        .build()
        .parseClaimsJws(asymmetricToken)
        .getBody();

    // check subject claim
    Assert.assertTrue(claims.containsKey("sub"));
    Assert.assertEquals("myUserId", claims.getSubject());

  }
}
