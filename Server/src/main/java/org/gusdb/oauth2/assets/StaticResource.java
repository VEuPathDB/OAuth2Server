package org.gusdb.oauth2.assets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticResource {

  private static final Logger LOG = LoggerFactory.getLogger(StaticResource.class);

  private static final String RESOURCE_PREFIX = "assets/";

  private static final List<ResponseType> VALID_TYPES =
      Arrays.asList(new ResponseType[] {
          ResponseType.html,
          ResponseType.css,
          ResponseType.javascript,
          ResponseType.jpeg
      });

  private static final List<String> VALID_RESOURCES =
      Arrays.asList(new String[] {
          "login.html",
          "success.html",
          "css/oauth.css",
          "js/lib/jquery-2.1.1.min.js",
          "js/oauth.js"
      });

  private final String _resourceName;
  private final boolean _isValid;
  private final ResponseType _responseType;

  public StaticResource(String name) {
    String resourceName = RESOURCE_PREFIX + name;
    boolean isValid;
    ResponseType responseType = null;
    try {
      responseType = ResponseType.resolveType(Paths.get(resourceName));
      LOG.trace("Is response type '" + responseType + "' in valid types? " + VALID_TYPES.contains(responseType));
      LOG.trace("Is name '" + name + "' in valid names? " + VALID_RESOURCES.contains(name));
      if (VALID_TYPES.contains(responseType) && VALID_RESOURCES.contains(name)) {
        // make sure we can actually access this resource
        LOG.debug("Resource type and name look valid.  Make sure system can find resource on classpath with path: " + resourceName);
        URL resource = Thread.currentThread().getContextClassLoader().getResource(resourceName);
        isValid = (resource != null);
      }
      else {
        isValid = false;
      }
    }
    catch (IllegalArgumentException e) {
      isValid = false;
    }
    _resourceName = resourceName;
    _isValid = isValid;
    _responseType = responseType;
  }

  public boolean isValid() {
    return _isValid;
  }

  public StreamingOutput getStreamingOutput() {
    if (!_isValid) return null;
    return new StreamingOutput() {
      @Override
      public void write(OutputStream out) throws IOException, WebApplicationException {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(_resourceName)) {
          IoUtil.transferStream(out, in);
        }
      }
    };
  }

  public String getMimeType() {
    return (_isValid ?
      _responseType.getMimeType() :
      ResponseType.text.getMimeType());
  }

}
