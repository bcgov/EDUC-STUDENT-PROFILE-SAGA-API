package ca.bc.gov.educ.api.student.profile.saga.endpoint.v1;

import ca.bc.gov.educ.api.student.profile.saga.constants.v1.URL;
import ca.bc.gov.educ.api.student.profile.saga.struct.Saga;
import ca.bc.gov.educ.api.student.profile.saga.struct.SagaEvent;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileCommentsSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileCompleteSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileRequestRejectActionSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileReturnActionSagaData;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping(URL.BASE_URL)
@OpenAPIDefinition(info = @Info(title = "API for Student Profile SAGA.", description = "This SAGA API is for Student Profile, to handle distributed transactions.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"STUDENT_PROFILE_COMPLETE_SAGA", "STUDENT_PROFILE_COMMENT_SAGA"})})
public interface StudentProfileSagaEndpoint {

  @PostMapping(URL.STUDENT_PROFILE_COMPLETE_SAGA)
  @PreAuthorize("hasAuthority('SCOPE_STUDENT_PROFILE_COMPLETE_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK.")})
  ResponseEntity<String> completeStudentProfile(@Validated @RequestBody StudentProfileCompleteSagaData studentProfileCompleteSagaData);

  @PostMapping(URL.STUDENT_PROFILE_COMMENT_SAGA)
  @PreAuthorize("hasAuthority('SCOPE_STUDENT_PROFILE_COMMENT_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK.")})
  ResponseEntity<String> submitStudentProfileComment(@Validated @RequestBody StudentProfileCommentsSagaData studentProfileCommentsSagaData);

  @PostMapping(URL.STUDENT_PROFILE_REJECT_SAGA)
  @PreAuthorize("hasAuthority('SCOPE_STUDENT_PROFILE_REJECT_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK.")})
  ResponseEntity<String> rejectStudentProfile(@Validated @RequestBody StudentProfileRequestRejectActionSagaData studentProfileRequestRejectActionSagaData);

  @PostMapping(URL.STUDENT_PROFILE_RETURN_SAGA)
  @PreAuthorize("hasAuthority('SCOPE_STUDENT_PROFILE_RETURN_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK.")})
  ResponseEntity<String> returnStudentProfile(@Validated @RequestBody StudentProfileReturnActionSagaData studentProfileReturnActionSagaData);

  @GetMapping(URL.SAGA_ID)
  @PreAuthorize("hasAuthority('SCOPE_STUDENT_PROFILE_READ_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK."), @ApiResponse(responseCode = "404", description = "NOT FOUND.")})
  ResponseEntity<Saga> getSagaBySagaID(@PathVariable UUID sagaID);

  @GetMapping(URL.SAGA_ID + URL.SAGA_EVENTS)
  @PreAuthorize("hasAuthority('SCOPE_STUDENT_PROFILE_READ_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK."), @ApiResponse(responseCode = "404", description = "NOT FOUND.")})
  ResponseEntity<List<SagaEvent>> getSagaEventsBySagaID(@PathVariable UUID sagaID);

  @PutMapping(URL.SAGA_ID)
  @PreAuthorize("hasAuthority('SCOPE_STUDENT_PROFILE_WRITE_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK."), @ApiResponse(responseCode = "404", description = "Not Found."), @ApiResponse(responseCode = "409", description = "Conflict.")})
  @Transactional
  @Tag(name = "Endpoint to update saga by its ID.", description = "Endpoint to update saga by its ID.")
  ResponseEntity<Saga> updateSaga(@RequestBody Saga saga, @PathVariable UUID sagaID);

  /**
   * Find all completable future.
   *
   * @param pageNumber             the page number
   * @param pageSize               the page size
   * @param sortCriteriaJson       the sort criteria json
   * @param searchCriteriaListJson the search criteria list json
   * @return the completable future
   */
  @GetMapping(URL.PAGINATED)
  @PreAuthorize("hasAuthority('SCOPE_STUDENT_PROFILE_READ_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to support data table view in frontend, with sort, filter and pagination.", description = "This API endpoint exposes flexible way to query the entity by leveraging JPA specifications.")
  Page<Saga> findAll(@RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber,
                     @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                     @RequestParam(name = "sort", defaultValue = "") String sortCriteriaJson,
                     @RequestParam(name = "searchCriteriaList", required = false) String searchCriteriaListJson);
}
