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

import org.gusdb.oauth2.InitializationException;
import org.gusdb.oauth2.assets.StaticResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
{
  "issuer":"https://integrate.eupathdb.org/oauth",
  "validateDomains": true,
  "tokenExpirationSecs": 3600,
  "useOpenIdConnect": true,
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
      "clientSecret": "12345",
      "clientDomains": [ "localhost" ]
    },{
      "clientId: "globusGenomics",
      "clientSecret": "12345",
      "clientDomains": [ "localhost" ]
    }
  ]
}
*/
public class ApplicationConfig {

  private static final Logger LOG = LoggerFactory.getLogger(ApplicationConfig.class);

  private static final String NL = System.lineSeparator();

  private static final boolean VALIDATE_DOMAINS_BY_DEFAULT = true;
  private static final boolean ALLOW_ANONYMOUS_LOGIN_BY_DEFAULT = false;
  private static final boolean USE_OPEN_ID_CONNECT_BY_DEFAULT = true;
  private static final String DEFAULT_LOGIN_FORM_PAGE = "login.html";
  private static final String DEFAULT_LOGIN_SUCCESS_PAGE = "success.html";
  private static final int DEFAULT_TOKEN_EXPIRATION_SECS = 300;

  private static enum JsonKey {
    issuer,
    authenticatorClass,
    authenticatorConfig,
    loginFormPage,
    loginSuccessPage,
    tokenExpirationSecs,
    allowAnonymousLogin,
    validateDomains,
    useOpenIdConnect,
    allowedClients
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
      boolean useOpenIdConnect = json.getBoolean(JsonKey.useOpenIdConnect.name(), USE_OPEN_ID_CONNECT_BY_DEFAULT);
      String loginFormPage = json.getString(JsonKey.loginFormPage.name(), DEFAULT_LOGIN_FORM_PAGE);
      String loginSuccessPage = json.getString(JsonKey.loginSuccessPage.name(), DEFAULT_LOGIN_SUCCESS_PAGE);
      int tokenExpirationSecs = json.getInt(JsonKey.tokenExpirationSecs.name(), DEFAULT_TOKEN_EXPIRATION_SECS);
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
      return new ApplicationConfig(issuer, authClassName, authClassConfig, loginFormPage,
          loginSuccessPage, tokenExpirationSecs, allowAnonymousLogin, validateDomains,
          useOpenIdConnect, allowedClients);
    }
    catch (ClassCastException | NullPointerException | IllegalArgumentException e) {
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
  private final boolean _anonymousLoginsAllowed;
  private final boolean _validateDomains;
  private final boolean _useOpenIdConnect;
  private final List<AllowedClient> _allowedClients;
  // map from clientId -> clientSecret
  private final Map<String,String> _secretMap;

  private ApplicationConfig(String issuer, String authClassName, JsonObject authClassConfig, String loginFormPage,
      String loginSuccessPage, int tokenExpirationSecs, boolean anonymousLoginsAllowed,
      boolean validateDomains, boolean useOpenIdConnect, List<AllowedClient> allowedClients) {
    _issuer = issuer;
    _authClassName = authClassName;
    _authClassConfig = authClassConfig;
    _loginFormPage = loginFormPage;
    _loginSuccessPage = loginSuccessPage;
    _tokenExpirationSecs = tokenExpirationSecs;
    _anonymousLoginsAllowed = anonymousLoginsAllowed;
    _validateDomains = validateDomains;
    _useOpenIdConnect = useOpenIdConnect;
    _allowedClients = allowedClients;
    _secretMap = new HashMap<>();
    for (AllowedClient client : _allowedClients) {
      _secretMap.put(client.getId(), client.getSecret());
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

  public boolean anonymousLoginsAllowed() {
    return _anonymousLoginsAllowed;
  }

  public boolean validateDomains() {
    return _validateDomains;
  }

  public boolean useOpenIdConnect() {
    return _useOpenIdConnect;
  }

  public List<AllowedClient> getAllowedClients() {
    return _allowedClients;
  }

  public Map<String,String> getSecretMap() {
    return _secretMap;
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
