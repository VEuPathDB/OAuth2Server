package org.gusdb.oauth2.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

import org.gusdb.oauth2.config.ApplicationConfig;
import org.gusdb.oauth2.server.OAuthServlet;

public class IFrameAllowanceFilter implements ContainerResponseFilter {

  @Context
  private ServletContext _context;

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {

    ApplicationConfig config = OAuthServlet.getApplicationConfig(_context);
    Set<String> iframeAllowedSites = config.getIFrameAllowedSites();
    if (!iframeAllowedSites.isEmpty()) {

      MultivaluedMap<String,Object> headers = responseContext.getHeaders();

      // Two steps to allow configured domains to host login form in an iframe:
      // 1. remove (default) frame options header
      headers.remove("X-FRAME-OPTIONS");

      // 2. set frame ancestors to configured sites
      headers.put("Content-Security-Policy", List.of("frame-ancestors localhost 'self' " + String.join(" ", iframeAllowedSites)));
    }
  }
}
