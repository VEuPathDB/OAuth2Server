package org.gusdb.oauth2.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.servlet.ServletProperties;
import org.gusdb.oauth2.Authenticator;
import org.gusdb.oauth2.InitializationException;
import org.gusdb.oauth2.config.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuthServlet extends ServletContainer {

  private static final long serialVersionUID = 1L;
  
  private static final Logger LOG = LoggerFactory.getLogger(OAuthServlet.class);

  private static final String CONFIG_FILE_PARAM_KEY = "oauth.config.file";
  private static final String OAUTH_CONFIG_KEY = "oauth.config";
  private static final String OAUTH_AUTHENTICATOR_KEY = "oauth.authenticator";

  public static ApplicationConfig getApplicationConfig(ServletContext context) {
    return (ApplicationConfig)context.getAttribute(OAUTH_CONFIG_KEY);
  }

  public static Authenticator getAuthenticator(ServletContext context) {
    return (Authenticator)context.getAttribute(OAUTH_AUTHENTICATOR_KEY);
  }

  @Override
  public void init() throws ServletException {
    super.init();
    try {
      configureApplication();
    }
    catch (InitializationException e) {
      String message = "Failed to initialize OAuth server";
      LOG.error(message, e);
      throw new ServletException(message, e);
    }
  }

  private void configureApplication() throws InitializationException {
    String configFileName = getServletConfig().getInitParameter(CONFIG_FILE_PARAM_KEY);
    LOG.info("Initializing OAuth Server with config: " + configFileName);
    if (configFileName == null || configFileName.isEmpty()) {
      throw new InitializationException("Missing required servlet init parameter: '" + CONFIG_FILE_PARAM_KEY + "'");
    }
    Path configFile = Paths.get(configFileName);
    if (!Files.isReadable(configFile)) {
      throw new InitializationException("Unable to read OAuth configuration file: " + configFileName +
          " (attempted full path: " + configFile.toAbsolutePath() + ")");
    }
    try {
      ServletContext context = getServletContext();
      ApplicationConfig config = ApplicationConfig.parseConfigFile(configFile);
      LOG.info("Configuration parsed successfully.");
      LOG.info("Will initialize Authenticator implementation: " + config.getAuthClassName());
      context.setAttribute(OAUTH_CONFIG_KEY, config);
      context.setAttribute(OAUTH_AUTHENTICATOR_KEY, getAuthenticator(config));
      LOG.info("Authenticator successfully initialized.");
      LOG.info("Ready to serve requests from " + config.getAllowedClients().size() + " unique clients.");
    }
    catch (InitializationException e) {
      throw e;
    }
    catch (Exception e) {
      throw new InitializationException("Unable to parse OAuth configuration file: " + configFileName, e);
    }
  }

  private static Authenticator getAuthenticator(ApplicationConfig config) throws InitializationException {
    String classMsg = "Specified authenticator class '" + config.getAuthClassName() + "'";
    try {
      @SuppressWarnings("unchecked")
      Class<? extends Authenticator> authClass = (Class<? extends Authenticator>)Class.forName(config.getAuthClassName());
      Authenticator authenticator = authClass.newInstance();
      authenticator.initialize(config.getAuthClassConfig());
      return authenticator;
    }
    catch (ClassCastException e) {
      throw new InitializationException(classMsg + " does not implement " + Authenticator.class.getName(), e);
    }
    catch (ClassNotFoundException e) {
      throw new InitializationException(classMsg + " cannot be found", e);
    }
    catch (IllegalAccessException e) {
      throw new InitializationException(classMsg + " must have a public no-arg constructor", e);
    }
    catch (InitializationException e) {
      throw e;
    }
    catch (Exception e) {
      throw new InitializationException(classMsg + " failed to initialize with provided configuration", e);
    }
  }

  @Override
  public String getInitParameter(String name) {
    if (name.equals(ServletProperties.JAXRS_APPLICATION_CLASS)) {
      return ServiceApplication.class.getName();
    }
    return super.getInitParameter(name);
  }
}
