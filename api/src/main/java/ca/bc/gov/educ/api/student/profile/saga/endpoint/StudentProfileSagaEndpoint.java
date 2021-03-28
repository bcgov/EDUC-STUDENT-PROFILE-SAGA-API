package ca.bc.gov.educ.api.student.profile.saga.endpoint;

import ca.bc.gov.educ.api.student.profile.saga.struct.Saga;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileCommentsSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileCompleteSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileRequestRejectActionSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileReturnActionSagaData;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping("/")
@OpenAPIDefinition(info = @Info(title = "API for Student Profile SAGA.", description = "This SAGA API is for Student Profile, to handle distributed transactions.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"STUDENT_PROFILE_COMPLETE_SAGA", "STUDENT_PROFILE_COMMENT_SAGA"})})
public interface StudentProfileSagaEndpoint {

  @PostMapping("/student-profile-complete-saga")
  @PreAuthorize("hasAuthority('SCOPE_STUDENT_PROFILE_COMPLETE_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK.")})
  ResponseEntity<String> completeStudentProfile(@Validated @RequestBody StudentProfileCompleteSagaData studentProfileCompleteSagaData);

  @PostMapping("/student-profile-comment-saga")
  @PreAuthorize("hasAuthority('SCOPE_STUDENT_PROFILE_COMMENT_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK.")})
  ResponseEntity<String> submitStudentProfileComment(@Validated @RequestBody StudentProfileCommentsSagaData studentProfileCommentsSagaData);

  @PostMapping("/student-profile-reject-saga")
  @PreAuthorize("hasAuthority('SCOPE_STUDENT_PROFILE_REJECT_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK.")})
  ResponseEntity<String> rejectStudentProfile(@Validated @RequestBody StudentProfileRequestRejectActionSagaData studentProfileRequestRejectActionSagaData);

  @PostMapping("/student-profile-return-saga")
  @PreAuthorize("hasAuthority('SCOPE_STUDENT_PROFILE_RETURN_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK.")})
  ResponseEntity<String> returnStudentProfile(@Validated @RequestBody StudentProfileReturnActionSagaData studentProfileReturnActionSagaData);

  @GetMapping("/{sagaID}")
  @PreAuthorize("hasAuthority('SCOPE_STUDENT_PROFILE_READ_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK."), @ApiResponse(responseCode = "404", description = "NOT FOUND.")})
  ResponseEntity<Saga> getSagaBySagaID(@PathVariable UUID sagaID);

}
