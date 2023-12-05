package org.gusdb.oauth2.service;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ParamException.PathParamException;

@Provider
public class ExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Exception> {

  private static Logger LOG = LogManager.getLogger(ExceptionMapper.class);

  @Override
  public Response toResponse(Exception e) {

    LOG.error("Error processing request", e);
    try { throw e; }

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
