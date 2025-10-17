package org.gusdb.oauth2.service;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ParamException.PathParamException;
import org.json.JSONException;

@Provider
public class ExceptionMapper implements jakarta.ws.rs.ext.ExceptionMapper<Exception> {

  private static Logger LOG = LogManager.getLogger(ExceptionMapper.class);

  @Override
  public Response toResponse(Exception e) {

    LOG.error("Error processing request", e);
    try { throw e; }

    // typically parsing errors are because of bad input
    catch (JSONException | NumberFormatException e400) {
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.TEXT_PLAIN)
          .entity(e400.getMessage())
          .build();
    }

    catch (NotFoundException | PathParamException e404) {
      return Response.status(Status.NOT_FOUND)
          .type(MediaType.TEXT_PLAIN)
          .entity("Not Found")
          .build();
    }

    catch (WebApplicationException eApp) {
      // use the appropriate status code and return the message of the exception as the response body
      return Response
          .status(eApp.getResponse().getStatus())
          .entity(eApp.getMessage()).build();
    }

    catch (Exception other) {
      LOG.error("Error during request", other);
      return Response.serverError()
          .type(MediaType.TEXT_PLAIN)
          .entity("Internal Error")
          .build();
    }
  }
}
