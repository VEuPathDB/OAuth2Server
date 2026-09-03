package org.gusdb.oauth2.eupathdb;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.oauth2.Authenticator.RequestingUser;
import org.gusdb.oauth2.client.OAuthClient;
import org.gusdb.oauth2.server.OAuthServlet;
import org.gusdb.oauth2.service.OAuthService;
import org.gusdb.oauth2.service.Session;

public abstract class AbstractService {

  private static final Logger LOG = LogManager.getLogger(AbstractService.class);

  protected static final String TSV_MEDIA_TYPE = "text/tab-separated-values";

  @Context
  protected ServletContext _context;

  @Context
  protected HttpServletRequest _request;

  protected AccountDbAuthenticator getAuthenticator() {
    return ((AccountDbAuthenticator) OAuthServlet.getAuthenticator(_context));
  }

  protected AccountDbInfo getAccountDb() {
    return getAuthenticator().getAccountDbInfo();
  }

  protected Long getRequestingUserId() {

    // this endpoint is only accessed directly using a bearer token
    String authHeader = _request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authHeader == null) throw new NotAuthorizedException(Response.status(Status.UNAUTHORIZED).build());

    // validate token and parse user
    String token = OAuthClient.getTokenFromAuthHeader(authHeader);
    RequestingUser user = OAuthService.parseRequestingUser(token, _context);

    // only allow registered users
    if (user.isGuest()) throw new NotAuthorizedException(Response.status(Status.UNAUTHORIZED).build());

    return Long.valueOf(user.getUserId());
  }

  protected void assertAdmin() {
    Session session = new Session(_request.getSession());
    String userId = "none";
    List<String> adminUserIds = ((AccountDbAuthenticator) OAuthServlet.getAuthenticator(_context)).getAdminUserIds();
    if (session.isAuthenticated()) {
      // user is logged in; get user ID and compare to known admin IDs
      userId = session.getUserId();
      if (adminUserIds.contains(userId)) {
        // current user is an admin
        return;
      }
    }
    LOG.warn("Attempt by " + userId + " to access admin endpoint denied (must be one of [ " + String.join(", ", adminUserIds) + " ].");
    throw new ForbiddenException();
  }
}
