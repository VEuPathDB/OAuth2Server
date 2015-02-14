package org.gusdb.oauth2;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
{
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
      "clientSecret": "12345"
    },{
      "clientId: "globusGenomics",
      "clientSecret": "12345"
    }
  ]
}
*/
public class ApplicationConfig {

  private static final Logger LOG = LoggerFactory.getLogger(ApplicationConfig.class);

  private static final String NL = System.lineSeparator();

  private static enum JsonKey {
    authenticatorClass,
    authenticatorConfig,
    allowedClients,
    clientId,
    clientSecret
  }

  public static class AllowedClient {
    private String _id;
    private String _secret;
    public AllowedClient(String id, String secret) {
      _id = id;
      _secret = secret;
    }
    public String getId() { return _id; }
    public String getSecret() { return _secret; }
  }

  public static ApplicationConfig parseConfigFile(Path configFile) throws IOException, InitializationException {
    LOG.info("Parsing config file: " + configFile.toAbsolutePath());
    try (FileInputStream in = new FileInputStream(configFile.toFile());
         JsonReader jsonIn = Json.createReader(in)) {
      JsonObject json = jsonIn.readObject();
      String authClassName = json.getString(JsonKey.authenticatorClass.name());
      JsonObject authClassConfig = json.getJsonObject(JsonKey.authenticatorConfig.name());
      JsonArray clientsJson = json.getJsonArray(JsonKey.allowedClients.name());
      List<AllowedClient> allowedClients = new ArrayList<>();
      for (JsonValue client : clientsJson) {
        JsonObject clientObj = (JsonObject)client;
        allowedClients.add(new AllowedClient(
            clientObj.getString(JsonKey.clientId.name()),
            clientObj.getString(JsonKey.clientSecret.name())
        ));
      }
      return new ApplicationConfig(authClassName, authClassConfig, allowedClients);
    }
    catch (ClassCastException | NullPointerException | IllegalArgumentException e) {
      throw new InitializationException("Improperly constructed configuration object", e);
    }
  }

  private String _authClassName;
  private JsonObject _authClassConfig;
  private Map<String, AllowedClient> _allowedClients = new HashMap<>();

  public ApplicationConfig(String authClassName, JsonObject authClassConfig, List<AllowedClient> allowedClients) {
    _authClassName = authClassName;
    _authClassConfig = authClassConfig;
    for (AllowedClient client : allowedClients) {
      if (_allowedClients.containsKey(client.getId()))
        throw new IllegalArgumentException("More than one allowed client configured with the same ID.  Client IDs must be unique.");
      _allowedClients.put(client.getId(), client);
    }
  }

  public String getAuthClassName() {
    return _authClassName;
  }

  public JsonObject getAuthClassConfig() {
    return _authClassConfig;
  }

  public AllowedClient getClient(String clientId) {
    return _allowedClients.get(clientId);
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

  public Map<String, AllowedClient> getAllowedClients() {
    return _allowedClients;
  }
}
