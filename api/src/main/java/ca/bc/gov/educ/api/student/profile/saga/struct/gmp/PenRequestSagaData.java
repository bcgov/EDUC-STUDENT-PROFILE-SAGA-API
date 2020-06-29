package ca.bc.gov.educ.api.student.profile.saga.struct.gmp;

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
public class PenRequestSagaData {
  private String penRequestID;
  private String pen;
  private String digitalID;
  private String penRequestStatusCode;
  private String legalFirstName;
  private String legalMiddleNames;
  private String legalLastName;
  private String dob;
  private String genderCode;
  private String usualFirstName;
  private String usualMiddleName;
  private String usualLastName;
  private String email;
  private String maidenName;
  private String pastNames;
  private String lastBCSchool;
  private String lastBCSchoolStudentNumber;
  private String currentSchool;
  private String reviewer;
  private String failureReason;
  private String initialSubmitDate;
  private String statusUpdateDate;
  private String emailVerified;
  private String bcscAutoMatchOutcome;
  private String bcscAutoMatchDetails;
  private String updateUser;
}
