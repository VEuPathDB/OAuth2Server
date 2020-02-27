package org.gusdb.oauth2.assets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticResource {

  private static final Logger LOG = LoggerFactory.getLogger(StaticResource.class);

  public static final String RESOURCE_PREFIX = "assets/";

  private static final List<ResponseType> VALID_TYPES =
      Arrays.asList(new ResponseType[] {
          ResponseType.html,
          ResponseType.css,
          ResponseType.javascript,
          ResponseType.jpeg,
          ResponseType.gif,
          ResponseType.png
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
      if (VALID_TYPES.contains(responseType)) {
        // make sure we can actually access this resource
        LOG.info("Resource type and name look valid.  Make sure system can find resource on classpath with path: " + resourceName);
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
    LOG.info("StaticResource object created for " + name + ", isValid="+ isValid);
    _resourceName = resourceName;
    _isValid = isValid;
    _responseType = responseType;
  }

  public boolean isValid() {
    return _isValid;
  }

  public StreamingOutput getStreamingOutput() {
    return (!_isValid ? null : out -> {
      LOG.info("Opening resource " + _resourceName);
      try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(_resourceName)) {
        LOG.info("Transfer starting");
        transferStream(out, in);
        LOG.info("Transfer ended");
      }
      LOG.info("Resource closed");
    });
  }

  public String getMimeType() {
    return (_isValid ?
      _responseType.getMimeType() :
      ResponseType.text.getMimeType());
  }

  /**
   * NOTE: this is a copy of the transferStream method in FgpUtil (sans logging)
   * 
   * Transfers data from input stream to the output stream until no more data
   * is available, then closes input stream (but not output stream).
   * 
   * @param outputStream output stream data is written to
   * @param inputStream input stream data is read from
   * @throws IOException if problem reading/writing data occurs
   */
  public static void transferStream(OutputStream outputStream, InputStream inputStream)
      throws IOException {
    try {
      byte[] buffer = new byte[10240]; // send 10kb at a time
      int bytesRead = inputStream.read(buffer);
      while (bytesRead != -1) {
        outputStream.write(buffer, 0, bytesRead);
        bytesRead = inputStream.read(buffer);
      }
      outputStream.flush();
    }
    finally {
      // only close input stream; container will close output stream
      inputStream.close();
    }
  }
}
