package ca.bc.gov.educ.api.student.profile.saga.endpoint;

import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.*;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/")
@OpenAPIDefinition(info = @Info(title = "API for Pen Requests SAGA.", description = "This SAGA API is for Pen Requests, to handle distributed transactions.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"READ_PEN_REQUEST", "WRITE_PEN_REQUEST"})})
public interface PenRequestSagaEndpoint {

  @PostMapping("/pen-request-complete-saga")
  @PreAuthorize("#oauth2.hasScope('PEN_REQUEST_COMPLETE_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK.")})
  ResponseEntity<String> completePENRequest(@Validated @RequestBody PenRequestCompleteSagaData penRequestCompleteSagaData);

  /**
   * this is for student making a comment and returning to staff, flow.
   */
  @PostMapping("/pen-request-comment-saga")
  @PreAuthorize("#oauth2.hasScope('PEN_REQUEST_COMMENT_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK.")})
  ResponseEntity<String> updatePenRequestAndAddComment(@Validated @RequestBody PenRequestCommentsSagaData penRequestCommentsSagaData);

  @PostMapping("/pen-request-return-saga")
  @PreAuthorize("#oauth2.hasScope('PEN_REQUEST_RETURN_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK.")})
  ResponseEntity<String> returnPENRequest(@Validated @RequestBody PenRequestReturnSagaData penRequestReturnSagaData);

  @PostMapping("/pen-request-reject-saga")
  @PreAuthorize("#oauth2.hasScope('PEN_REQUEST_REJECT_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK.")})
  ResponseEntity<String> rejectPENRequest(@Validated @RequestBody PenRequestRejectSagaData penRequestRejectSagaData);

  /**
   * If staff members first complete a PEN request, then reject the same request, DIGITAL_ID is updated to remove the link with STUDENT record and
   *  PEN_REQUEST is updated subsequent review.
   * @param penRequestUnlinkSagaData the payload which will be processed by this saga.
   * @return String the saga Id
   */
  @PostMapping("/pen-request-unlink-saga")
  @PreAuthorize("#oauth2.hasScope('PEN_REQUEST_UNLINK_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "OK.")})
  ResponseEntity<String> unlinkPenRequest(@Validated @RequestBody PenRequestUnlinkSagaData penRequestUnlinkSagaData);
}
