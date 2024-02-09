package org.gusdb.oauth2.eupathdb;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import javax.json.Json;
import javax.json.JsonObject;

import org.gusdb.oauth2.Authenticator;
import org.gusdb.oauth2.config.ApplicationConfig;
import org.gusdb.oauth2.service.token.IdTokenFactory;
import org.gusdb.oauth2.service.token.TokenStore.IdTokenParams;
import org.gusdb.oauth2.shared.Signatures;
import org.gusdb.oauth2.shared.SigningKeyStore;

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
      String token = generateToken(asyncKeysRandomSeed, accountDbLogin, accountDbPassword, loginName);
      System.out.println(token);
    }
  }

  private static String findProp(Properties config, String key) {
    return config.containsKey(key) ? config.getProperty(key) : usage();
  }

  private static String usage() {
    System.err.println("USAGE: java org.gusdb.oauth2.eupathdb.BearerTokenGenerator <configFile> <loginName>");
    System.err.println("   configFile must contain '='-delimited property rows with the following keys: " +
        PROP_ASYNC_KEYS_RANDOM_SEED + ", " + PROP_ACCOUNTDB_LOGIN + ", " + PROP_ACCOUNTDB_PASSWORD);
    System.exit(1);
    return null;
  }

  private static String generateToken(String asyncKeysRandomSeed, String accountDbLogin, String accountDbPassword, String loginName) throws Exception {

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

      JsonObject tokenJson = IdTokenFactory.createIdTokenJson(authenticator, loginName, params,
          "https://eupathdb.org/oauth", ApplicationConfig.DEFAULT_BEARER_TOKEN_EXPIRATION_SECS, false);

      SigningKeyStore keyStore = new SigningKeyStore(asyncKeysRandomSeed);

      return Signatures.ASYMMETRIC_KEY_SIGNER.getSignedEncodedToken(tokenJson, keyStore, params.getClientId(), null);
    }
  }
}
