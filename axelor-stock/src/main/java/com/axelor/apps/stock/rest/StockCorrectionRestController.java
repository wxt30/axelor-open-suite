package com.axelor.apps.stock.rest;

import com.axelor.apps.stock.db.StockCorrection;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.repo.StockCorrectionRepository;
import com.axelor.apps.stock.rest.dto.StockCorrectionPostRequest;
import com.axelor.apps.stock.rest.dto.StockCorrectionPutRequest;
import com.axelor.apps.stock.rest.dto.StockCorrectionResponse;
import com.axelor.apps.stock.service.StockCorrectionService;
import com.axelor.apps.tool.api.*;
import com.axelor.inject.Beans;
import java.util.Arrays;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/aos/stock-correction")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class StockCorrectionRestController {

  @Path("/")
  @POST
  @HttpExceptionHandler
  public Response createStockCorrection(StockCorrectionPostRequest requestBody) throws Exception {
    RequestValidator.validateBody(requestBody);
    new SecurityCheck().createAccess(Arrays.asList(StockCorrection.class, StockMove.class)).check();

    StockCorrection stockCorrection =
        Beans.get(StockCorrectionService.class)
            .generateStockCorrection(
                requestBody.fetchStockLocation(),
                requestBody.fetchProduct(),
                requestBody.fetchTrackingNumber(),
                requestBody.getRealQty(),
                requestBody.fetchReason());

    if (requestBody.getStatus() == StockCorrectionRepository.STATUS_VALIDATED) {
      Beans.get(StockCorrectionService.class).validate(stockCorrection);
    }

    return ResponseConstructor.build(
        Response.Status.CREATED,
        "Resource successfully created",
        new StockCorrectionResponse(stockCorrection));
  }

  @Path("/{id}")
  @PUT
  @HttpExceptionHandler
  public Response saveStockCorrection(
      @PathParam("id") long stockCorrectionId, StockCorrectionPutRequest requestBody)
      throws Exception {
    RequestValidator.validateBody(requestBody);
    new SecurityCheck().writeAccess(StockCorrection.class).createAccess(StockMove.class).check();

    StockCorrection stockCorrection = ObjectFinder.find(StockCorrection.class, stockCorrectionId);

    String message = "";
    if (requestBody.getRealQty() != null) {
      Beans.get(StockCorrectionService.class)
          .updateCorrectionQtys(stockCorrection, requestBody.getRealQty());
      message += "Real qty updated; ";
    }

    // Stock correction is not already validated
    if (stockCorrection.getStatusSelect() != StockCorrectionRepository.STATUS_VALIDATED
        && requestBody.getStatus() != null) {
      int status = requestBody.getStatus();
      // user wants to validate stock correction
      if (status == StockCorrectionRepository.STATUS_VALIDATED) {
        if (Beans.get(StockCorrectionService.class).validate(stockCorrection)) {
          message += "Status updated; ";
        }
      }
    }

    StockCorrectionResponse objectBody = new StockCorrectionResponse(stockCorrection);
    return ResponseConstructor.build(Response.Status.OK, message, objectBody);
  }
}
