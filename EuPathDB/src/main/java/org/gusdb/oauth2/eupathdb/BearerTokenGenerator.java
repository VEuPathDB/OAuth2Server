package org.gusdb.oauth2.eupathdb;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.Properties;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;

import org.gusdb.oauth2.Authenticator;
import org.gusdb.oauth2.Authenticator.DataScope;
import org.gusdb.oauth2.config.ApplicationConfig;
import org.gusdb.oauth2.service.token.TokenFactory;
import org.gusdb.oauth2.service.token.TokenStore.IdTokenParams;
import org.gusdb.oauth2.shared.ECPublicKeyRepresentation;
import org.gusdb.oauth2.shared.Signatures;
import org.gusdb.oauth2.shared.SigningKeyStore;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

public class BearerTokenGenerator {

  private static final String PROP_ASYNC_KEYS_RANDOM_SEED = "asyncKeysRandomSeed";
  private static final String PROP_ACCOUNTDB_LOGIN = "accountDbLogin";
  private static final String PROP_ACCOUNTDB_PASSWORD = "accountDbPassword";

  public static void main(String[] args) throws Exception {
    if (args.length != 2) usage();
    Properties config = new Properties();
    try (InputStream in = new FileInputStream(args[0])) {
      config.load(in);
      String asyncKeysRandomSeed = findProp(config, PROP_ASYNC_KEYS_RANDOM_SEED);
      String accountDbLogin = findProp(config, PROP_ACCOUNTDB_LOGIN);
      String accountDbPassword = findProp(config, PROP_ACCOUNTDB_PASSWORD);
      String loginName = args[1];

      SigningKeyStore keyStore = new SigningKeyStore(asyncKeysRandomSeed);
      keyStore.setClientSigningKeys("abc", Set.of("mug2kfCI8qhXzrnuE/nh1gK9JbSFaXaih+zdsfD8io25MWH4b3V5u+U8E7SW4x7iBAHdq6yWWrF/TP9p098lfQ=="));

      // generate a new token for this account
      String token = generateToken(keyStore, accountDbLogin, accountDbPassword, loginName);
      System.out.println("Bearer Token\n\n" + token + "\n");

      // verify token using same method as the client
      System.out.println("JWKS content: " + Signatures.getJwksContent(keyStore));
      ECPublicKey publicKeyIn = (ECPublicKey)keyStore.getAsyncKeys().getPublic();
      String publicKeyString = new ECPublicKeyRepresentation(publicKeyIn).getBase64String();
      System.out.println("Public key string:\n\n" + publicKeyString);

      PublicKey publicKeyOut = new ECPublicKeyRepresentation(publicKeyString).getPublicKey();

      // verify signature and create claims object
      Claims claims = Jwts.parserBuilder()
          .setSigningKey(publicKeyOut)
          .build()
          .parseClaimsJws(token)
          .getBody();
      System.out.println("Subject after parsing token: " + claims.getSubject());
    }
  }

  private static String findProp(Properties config, String key) {
    String value = config.containsKey(key) ? config.getProperty(key) : usage();
    System.err.println("Config: " + key + " = " + value);
    return value;
  }

  private static String usage() {
    System.err.println("USAGE: java org.gusdb.oauth2.eupathdb.BearerTokenGenerator <configFile> <loginName>");
    System.err.println("   configFile must contain '='-delimited property rows with the following keys: " +
        PROP_ASYNC_KEYS_RANDOM_SEED + ", " + PROP_ACCOUNTDB_LOGIN + ", " + PROP_ACCOUNTDB_PASSWORD);
    System.exit(1);
    return null;
  }

  private static String generateToken(SigningKeyStore keyStore, String accountDbLogin, String accountDbPassword, String loginName) throws Exception {

    JsonObject authenticatorConfig = Json.createObjectBuilder()
        .add("login", accountDbLogin)
        .add("password", accountDbPassword)
        .add("connectionUrl", "jdbc:oracle:thin:@localhost:5011/acctdb.upenn.edu")
        .add("platform", "Oracle")
        .add("poolSize", 1)
        .add("schema", "useraccounts.")
        .build();

    try (Authenticator authenticator = new AccountDbAuthenticator()) {

      authenticator.initialize(authenticatorConfig);

      IdTokenParams params = new IdTokenParams("apiComponentSite", null);

      JsonObject tokenJson = TokenFactory.createTokenJson(authenticator, loginName, params,
          "https://eupathdb.org/oauth", ApplicationConfig.DEFAULT_BEARER_TOKEN_EXPIRATION_SECS, DataScope.BEARER_TOKEN);

      return Signatures.ASYMMETRIC_KEY_SIGNER.getSignedEncodedToken(tokenJson, keyStore, params.getClientId(), null);
    }
  }
}
