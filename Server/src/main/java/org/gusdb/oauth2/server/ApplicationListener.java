package org.gusdb.oauth2.server;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Application listener closes resources on the client-provided Authenticator class.  This
 * listener should be included in web.xml if your Authenticator opens resources during its
 * init() method that must be closed before the object is finalized.  Including this
 * listener ensures those resources are closed during web app shut down; if your
 * Authenticator does not open resources, you need not register this listener in web.xml.
 * 
 * @author ryan
 */
public class ApplicationListener implements ServletContextListener {

  @Override
  public void contextInitialized(ServletContextEvent event) {
    // nothing to do here
  }

  @Override
  public void contextDestroyed(ServletContextEvent event) {
    OAuthServlet.getAuthenticator(event.getServletContext()).close();
  }

}
