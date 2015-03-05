package org.gusdb.oauth2.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
  "validateDomains": true,
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
  private static final String DEFAULT_LOGIN_FORM_PAGE = "login.html";
  private static final String DEFAULT_LOGIN_SUCCESS_PAGE = "success.html";

  private static enum JsonKey {
    authenticatorClass,
    authenticatorConfig,
    loginFormPage,
    loginSuccessPage,
    allowAnonymousLogin,
    validateDomains,
    allowedClients,
    clientId,
    clientSecret,
    clientDomains
  }

  public static ApplicationConfig parseConfigFile(Path configFile) throws IOException, InitializationException {
    LOG.debug("Parsing config file: " + configFile.toAbsolutePath());
    try (FileInputStream in = new FileInputStream(configFile.toFile());
         JsonReader jsonIn = Json.createReader(in)) {
      JsonObject json = jsonIn.readObject();
      String authClassName = json.getString(JsonKey.authenticatorClass.name());
      JsonObject authClassConfig = json.getJsonObject(JsonKey.authenticatorConfig.name());
      boolean validateDomains = json.getBoolean(JsonKey.validateDomains.name(), VALIDATE_DOMAINS_BY_DEFAULT);
      boolean allowAnonymousLogin = json.getBoolean(JsonKey.allowAnonymousLogin.name(), ALLOW_ANONYMOUS_LOGIN_BY_DEFAULT);
      String loginFormPage = json.getString(JsonKey.loginFormPage.name(), DEFAULT_LOGIN_FORM_PAGE);
      String loginSuccessPage = json.getString(JsonKey.loginSuccessPage.name(), DEFAULT_LOGIN_SUCCESS_PAGE);
      validateResource(loginFormPage);
      validateResource(loginSuccessPage);
      JsonArray clientsJson = json.getJsonArray(JsonKey.allowedClients.name());
      List<AllowedClient> allowedClients = new ArrayList<>();
      Set<String> usedClientIds = new HashSet<String>();
      for (JsonValue client : clientsJson) {
        JsonObject clientObj = (JsonObject)client;

        // validate domain list
        JsonArray clientDomains = clientObj.getJsonArray(JsonKey.clientDomains.name());
        Set<String> domainList = new HashSet<>();
        if (clientDomains != null) {
          for (int i = 0; i < clientDomains.size(); i++) {
            domainList.add(clientDomains.getString(i));
          }
        }

        // validate client id uniqueness
        String clientId = clientObj.getString(JsonKey.clientId.name());
        if (usedClientIds.contains(clientId)) {
          throw new IllegalArgumentException("More than one allowed client configured with the same ID.  Client IDs must be unique.");
        }
        usedClientIds.add(clientId);

        allowedClients.add(new AllowedClient(
            clientId,
            clientObj.getString(JsonKey.clientSecret.name()),
            domainList
        ));
      }
      return new ApplicationConfig(authClassName, authClassConfig,
          loginFormPage, loginSuccessPage, allowAnonymousLogin, validateDomains, allowedClients);
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

  private final String _authClassName;
  private final JsonObject _authClassConfig;
  private final String _loginFormPage;
  private final String _loginSuccessPage;
  private final boolean _anonymousLoginsAllowed;
  private final boolean _validateDomains;
  private final List<AllowedClient> _allowedClients;

  private ApplicationConfig(String authClassName, JsonObject authClassConfig, String loginFormPage,
      String loginSuccessPage, boolean anonymousLoginsAllowed, boolean validateDomains, List<AllowedClient> allowedClients) {
    _authClassName = authClassName;
    _authClassConfig = authClassConfig;
    _loginFormPage = loginFormPage;
    _loginSuccessPage = loginSuccessPage;
    _anonymousLoginsAllowed = anonymousLoginsAllowed;
    _validateDomains = validateDomains;
    _allowedClients = allowedClients;
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

  public boolean anonymousLoginsAllowed() {
    return _anonymousLoginsAllowed;
  }
  public boolean validateDomains() {
    return _validateDomains;
  }

  public List<AllowedClient> getAllowedClients() {
    return _allowedClients;
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
