package ca.bc.gov.educ.api.student.profile.saga.endpoint;

import ca.bc.gov.educ.api.student.profile.saga.struct.StudentProfileCommentsSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.StudentProfileCompleteSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.StudentProfileRequestRejectActionSagaData;
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
@OpenAPIDefinition(info = @Info(title = "API for Student Profile SAGA.", description = "This SAGA API is for Student Profile, to handle distributed transactions.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"STUDENT_PROFILE_COMPLETE_SAGA", "STUDENT_PROFILE_COMMENT_SAGA"})})
public interface StudentProfileSagaEndpoint {

  @PostMapping("/student-profile-complete-saga")
  @PreAuthorize("#oauth2.hasScope('STUDENT_PROFILE_COMPLETE_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "NO CONTENT.")})
  ResponseEntity<Void> completeStudentProfile(@Validated @RequestBody StudentProfileCompleteSagaData studentProfileCompleteSagaData);

  @PostMapping("/student-profile-comment-saga")
  @PreAuthorize("#oauth2.hasScope('STUDENT_PROFILE_COMMENT_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "NO CONTENT.")})
  ResponseEntity<Void> submitStudentProfileComment(@Validated @RequestBody StudentProfileCommentsSagaData studentProfileCommentsSagaData);

  @PostMapping("/student-profile-reject-saga")
  @PreAuthorize("#oauth2.hasScope('STUDENT_PROFILE_REJECT_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "NO CONTENT.")})
  ResponseEntity<Void> rejectStudentProfile(@Validated @RequestBody StudentProfileRequestRejectActionSagaData studentProfileRequestRejectActionSagaData);
}
