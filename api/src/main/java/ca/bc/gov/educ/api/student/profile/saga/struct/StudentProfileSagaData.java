package ca.bc.gov.educ.api.student.profile.saga.struct;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentProfileSagaData {
  private String studentRequestID;
  private String digitalID;
  private String studentRequestStatusCode;
  private String legalFirstName;
  private String legalMiddleNames;
  private String legalLastName;
  private String dob;
  private String genderCode;
  private String email;
  private String recordedPen;
  private String recordedLegalFirstName;
  private String recordedLegalMiddleNames;
  private String recordedLegalLastName;
  private String recordedDob;
  private String recordedGenderCode;
  private String recordedEmail;
  private String reviewer;
  private String failureReason;
  private String initialSubmitDate;
  private String statusUpdateDate;
  private String emailVerified;
  private String completeComment;
  private String createUser;
  private String updateUser;
}
