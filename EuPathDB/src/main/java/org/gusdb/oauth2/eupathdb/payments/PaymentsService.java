package org.gusdb.oauth2.eupathdb.payments;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.gusdb.oauth2.eupathdb.AbstractService;
import org.gusdb.oauth2.service.OAuthResponseFactory;
import org.gusdb.oauth2.service.OAuthService;

@Path("/payments")
public class PaymentsService extends AbstractService {

  @GET
  @Produces(TSV_MEDIA_TYPE)
  public Response getAllPayments() {
    // must be a subscription admin to download payments
    assertAdmin();

    // get handle on DB, fetch payments and stream out in tabular format
    PaymentsManager db = new PaymentsManager(getAccountDb());
    StreamingOutput tabularOutput = out -> db.writeAllPaymentsAsTabular(out);
    return Response.ok(tabularOutput).build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response postPayment(String body) {
    try {

      // must be valid user management client for this endpoint
      JsonObject requestJson = Json.createReader(new StringReader(body)).readObject();
      if (!OAuthService.isUserManagementClient(requestJson, _context)) {
        return new OAuthResponseFactory().buildInvalidClientResponse();
      }

      JsonObject payment = requestJson.getJsonObject("payment");
      String referenceNumber = payment.getString("referenceNumber");

      PaymentsManager db = new PaymentsManager(getAccountDb());

      // check if reference number is unique; minor race condition here but very unlikely
      if (db.findPayment(referenceNumber).isPresent()) {
        return Response
            .status(Status.CONFLICT)
            .entity("Payment already exists with reference number " + referenceNumber)
            .build();
      }

      // insert the payment
      db.insertPayment(payment);

      return Response.noContent().build();
    }
    catch(JsonParsingException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  @POST
  @Path("{referenceNumber}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getPaymentByReferenceNumber(@PathParam("referenceNumber") String referenceNumber, String body) {

    // basic validation check on reference number
    referenceNumber = referenceNumber.trim();
    if (referenceNumber.isBlank()) {
      throw new BadRequestException("Reference number cannot be empty.");
    }

    // must be valid user management client for this endpoint
    JsonObject requestJson = Json.createReader(new StringReader(body)).readObject();
    if (!OAuthService.isUserManagementClient(requestJson, _context)) {
      return new OAuthResponseFactory().buildInvalidClientResponse();
    }

    // find payment and return it, or 404 if not found in DB
    return new PaymentsManager(getAccountDb()).findPayment(referenceNumber)
      .map(payment -> Response.ok(payment.toString()).build())
      .orElse(Response.status(Status.NOT_FOUND).build());
  }
}
