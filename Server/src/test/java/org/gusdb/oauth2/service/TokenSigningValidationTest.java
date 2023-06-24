package org.gusdb.oauth2.service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.function.Supplier;

import javax.json.Json;
import javax.json.JsonObject;

import org.gusdb.oauth2.InitializationException;
import org.gusdb.oauth2.service.token.Signatures;
import org.gusdb.oauth2.service.token.SigningKeyStore;
import org.gusdb.oauth2.service.token.TokenStore.IdTokenParams;
import org.junit.Assert;
import org.junit.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.impl.DefaultHeader;
import io.jsonwebtoken.impl.TextCodec;
import io.jsonwebtoken.io.Decoders;

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
      return keyStore;
    }
    catch (InitializationException e) {
      throw new RuntimeException(e);
    }
  }).get();

  // params to sign tokens
  private static final IdTokenParams ID_TOKEN_PARAMS = new IdTokenParams(CLIENT_ID, null);

  @Test
  public void testLegacySymmetricTokenValidator() throws Exception {

    // create a signed token
    String symmetricToken = Signatures.SECRET_KEY_SIGNER.getSignedEncodedToken(DUMMY_CLAIMS, KEY_STORE, ID_TOKEN_PARAMS);

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
    String symmetricToken = Signatures.SECRET_KEY_SIGNER.getSignedEncodedToken(DUMMY_CLAIMS, KEY_STORE, ID_TOKEN_PARAMS);

    // encode the key as a base64 string
    byte[] unencodedKey = CLIENT_SECRET.getBytes(StandardCharsets.UTF_8);
    byte[] encodedKey = Base64.getEncoder().encode(unencodedKey);
    String encodedKeyStr = new String(encodedKey, StandardCharsets.UTF_8);
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
  public void testAsymmetricTokenValidator() throws Exception {

    // create a signed token
    String asymmetricToken = Signatures.ASYMMETRIC_KEY_SIGNER.getSignedEncodedToken(DUMMY_CLAIMS, KEY_STORE, ID_TOKEN_PARAMS);
    System.out.println(Signatures.getJwksContent(KEY_STORE));

    // encode the public key for distribution
    byte[] encodedKey = KEY_STORE.getAsyncKeys().getPublic().getEncoded();
    String encodedKeyStr = new String(encodedKey, StandardCharsets.UTF_8);

    // convert the key to a public key object
    byte[] publicBytes = Base64.getDecoder().decode(encodedKeyStr.getBytes(StandardCharsets.UTF_8));
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
    KeyFactory keyFactory = KeyFactory.getInstance("EC");
    PublicKey publicKey = keyFactory.generatePublic(keySpec);

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
