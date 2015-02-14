package org.gusdb.oauth2.assets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IoUtil {
  /**
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
      byte[] buffer = new byte[1024]; // send 1kb at a time
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }
    }
    finally {
      // only close input stream; container will close output stream
      inputStream.close();
    }
  }
}
