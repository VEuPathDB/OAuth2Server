package org.gusdb.oauth2.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.oauth2.InitializationException;
import org.gusdb.oauth2.assets.StaticResource;
import org.gusdb.oauth2.shared.CryptoException;
import org.gusdb.oauth2.shared.SigningKeyStore;

/** Example config JSON **
{
  "issuer":"https://integrate.eupathdb.org/oauth",
  "validateDomains": true,
  "tokenExpirationSecs": 3600,
  "bearerTokenExpirationSecs": 94608000,
  "keyPairRandomSeed": 12345,
  "loginFormPage": "login.html", // optional, login.html is default
  "loginSuccessPage": "success.html", // optional, success.html is default
  "authenticatorClass": "org.gusdb.oauth2.wdk.UserDbAuthenticator",
  "authenticatorConfig": {
     "login": "db_login",
     "password": "db_password",
     "connectionUrl": "jdbc:oracle:oci:@apicommDevN",
     "platform": "Oracle",
     "maxActive": "20",
     "maxIdle": "1",
     "minIdle": "1",
     "maxWait": "50",
     "userSchema": "userlogins5."
  },
  "allowedClients": [
    {
      "clientId": "apiComponentSite",
      "clientSecrets": [ "12345" ],
      "clientDomains": [ "localhost" ],
      "allowUserManagement": true,
      "allowROPCGrant": true,
      "allowGuestObtainment": true,
      "allowUserQueries": true
    },{
      "clientId: "globusGenomics",
      "clientSecrets": [ "12345" ],
      "clientDomains": [ "localhost" ]
    }
  ]
}
*/
public class ApplicationConfig extends SigningKeyStore {

  private static final Logger LOG = LogManager.getLogger(ApplicationConfig.class);

  private static final String NL = System.lineSeparator();

  private static final boolean VALIDATE_DOMAINS_BY_DEFAULT = true;
  private static final boolean ALLOW_ANONYMOUS_LOGIN_BY_DEFAULT = false;

  private static final String DEFAULT_LOGIN_FORM_PAGE = "login.html";
  private static final String DEFAULT_LOGIN_SUCCESS_PAGE = "success.html";

  public static final int DEFAULT_TOKEN_EXPIRATION_SECS = 300; // five minutes
  public static final int DEFAULT_BEARER_TOKEN_EXPIRATION_SECS = 5184000; // two months

  private static enum JsonKey {
    issuer,
    authenticatorClass,
    authenticatorConfig,
    loginFormPage,
    loginSuccessPage,
    tokenExpirationSecs,
    bearerTokenExpirationSecs,
    allowAnonymousLogin,
    validateDomains,
    allowedClients,
    keyPairRandomSeed
  }

  public static ApplicationConfig parseConfigFile(Path configFile) throws IOException, InitializationException {
    LOG.debug("Parsing config file: " + configFile.toAbsolutePath());
    try (FileInputStream in = new FileInputStream(configFile.toFile());
         JsonReader jsonIn = Json.createReader(in)) {
      JsonObject json = jsonIn.readObject();
      String issuer = json.getString(JsonKey.issuer.name(), null);
      if (issuer == null)
        throw new InitializationException("Configuration property '" + JsonKey.issuer.name() + "' is required.");
      String authClassName = json.getString(JsonKey.authenticatorClass.name());
      JsonObject authClassConfig = json.getJsonObject(JsonKey.authenticatorConfig.name());
      boolean validateDomains = json.getBoolean(JsonKey.validateDomains.name(), VALIDATE_DOMAINS_BY_DEFAULT);
      boolean allowAnonymousLogin = json.getBoolean(JsonKey.allowAnonymousLogin.name(), ALLOW_ANONYMOUS_LOGIN_BY_DEFAULT);
      String loginFormPage = json.getString(JsonKey.loginFormPage.name(), DEFAULT_LOGIN_FORM_PAGE);
      String loginSuccessPage = json.getString(JsonKey.loginSuccessPage.name(), DEFAULT_LOGIN_SUCCESS_PAGE);
      int tokenExpirationSecs = json.getInt(JsonKey.tokenExpirationSecs.name(), DEFAULT_TOKEN_EXPIRATION_SECS);
      int bearerTokenExpirationSecs = json.getInt(JsonKey.bearerTokenExpirationSecs.name(), DEFAULT_BEARER_TOKEN_EXPIRATION_SECS);
      validateResource(loginFormPage);
      validateResource(loginSuccessPage);
      JsonArray clientsJson = json.getJsonArray(JsonKey.allowedClients.name());
      List<AllowedClient> allowedClients = new ArrayList<>();
      Set<String> usedClientIds = new HashSet<String>();
      for (JsonValue clientJson : clientsJson) {
        JsonObject clientJsonObj = (JsonObject)clientJson;
        AllowedClient client = AllowedClient.createFromJson(clientJsonObj);
        // make sure clientIds are unique
        if (usedClientIds.contains(client.getId())) {
          throw new IllegalArgumentException("More than one allowed client configured with the same ID.  Client IDs must be unique.");
        }
        usedClientIds.add(client.getId());
        allowedClients.add(client);
      }
      String keyPairRandomSeed = json.getString(JsonKey.keyPairRandomSeed.name());
      return new ApplicationConfig(issuer, authClassName, authClassConfig, loginFormPage,
          loginSuccessPage, tokenExpirationSecs, bearerTokenExpirationSecs, allowAnonymousLogin, validateDomains,
          allowedClients, keyPairRandomSeed);
    }
    catch (ClassCastException | NullPointerException | IllegalArgumentException | CryptoException e) {
      throw new InitializationException("Improperly constructed configuration object", e);
    }
  }

  private static void validateResource(String resourcePath) throws InitializationException {
    StaticResource resource = new StaticResource(resourcePath);
    if (!resource.isValid()) {
      throw new InitializationException("Cannot find configured resource '" + resourcePath + "' in classpath.");
    }
  }

  private final String _issuer;
  private final String _authClassName;
  private final JsonObject _authClassConfig;
  private final String _loginFormPage;
  private final String _loginSuccessPage;
  private final int _tokenExpirationSecs;
  private final int _bearerTokenExpirationSecs;
  private final boolean _anonymousLoginsAllowed;
  private final boolean _validateDomains;
  private final List<AllowedClient> _allowedClients;
  // map from clientId -> clientSecret
  private final Map<String,Set<String>> _secretsMap;

  private ApplicationConfig(String issuer, String authClassName, JsonObject authClassConfig, String loginFormPage,
      String loginSuccessPage, int tokenExpirationSecs, int bearerTokenExpirationSecs, boolean anonymousLoginsAllowed,
      boolean validateDomains, List<AllowedClient> allowedClients, String keyPairRandomSeed) throws CryptoException {
    super(keyPairRandomSeed);
    _issuer = issuer;
    _authClassName = authClassName;
    _authClassConfig = authClassConfig;
    _loginFormPage = loginFormPage;
    _loginSuccessPage = loginSuccessPage;
    _tokenExpirationSecs = tokenExpirationSecs;
    _bearerTokenExpirationSecs = bearerTokenExpirationSecs;
    _anonymousLoginsAllowed = anonymousLoginsAllowed;
    _validateDomains = validateDomains;
    _allowedClients = allowedClients;
    _secretsMap = new HashMap<>();
    for (AllowedClient client : _allowedClients) {
      _secretsMap.put(client.getId(), client.getSecrets());
      setClientSigningKeys(client.getId(), client.getSecrets());
    }
  }

  public String getIssuer() {
    return _issuer;
  }

  public String getAuthClassName() {
    return _authClassName;
  }

  public JsonObject getAuthClassConfig() {
    return _authClassConfig;
  }

  public String getLoginFormPage() {
    return _loginFormPage;
  }

  public String getLoginSuccessPage() {
    return _loginSuccessPage;
  }

  public int getTokenExpirationSecs() {
    return _tokenExpirationSecs;
  }

  public int getBearerTokenExpirationSecs() {
    return _bearerTokenExpirationSecs;
  }

  public boolean anonymousLoginsAllowed() {
    return _anonymousLoginsAllowed;
  }

  public boolean validateDomains() {
    return _validateDomains;
  }

  public List<AllowedClient> getAllowedClients() {
    return _allowedClients;
  }

  public Map<String,Set<String>> getSecretsMap() {
    return _secretsMap;
  }

  @Override
  public String toString() {
    return new StringBuilder()
      .append("{").append(NL)
      .append("authClassName: ").append(_authClassName).append(NL)
      .append("authClassConfig: ").append(_authClassConfig.toString()).append(NL)
      .append("numAllowedClients: ").append(_allowedClients.size()).append(NL)
      .append("}").append(NL)
      .toString();
  }
}
