package org.gusdb.oauth2.service;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Trimmed down version of the request logger we use in WDK
 */
public class RequestLoggingFilter implements ContainerRequestFilter {

  private static final Logger LOG = LogManager.getLogger(RequestLoggingFilter.class);

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    logRequestStart(
        requestContext.getMethod(),
        requestContext.getUriInfo());
  }

  private static void logRequestStart(String method, UriInfo uriInfo) {

    StringBuilder log = new StringBuilder("HTTP ")
      .append(method).append(" /").append(uriInfo.getPath());

    // add query params if present
    MultivaluedMap<String,String> query = uriInfo.getQueryParameters();
    if (!query.isEmpty()) {
      log.append("\n").append("Query Parameters: ").append(queryToJson(query));
    }

    LOG.info(log.toString());
  }

  private static String queryToJson(MultivaluedMap<String, String> map) {
    JSONObject json = new JSONObject();
    for (Entry<String,List<String>> entry : map.entrySet()) {
      json.put(entry.getKey(), new JSONArray(entry.getValue()));
    }
    return json.toString(2);
  }
}
