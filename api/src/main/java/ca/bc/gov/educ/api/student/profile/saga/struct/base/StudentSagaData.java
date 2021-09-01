package ca.bc.gov.educ.api.student.profile.saga.struct.base;

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
public class StudentSagaData {
  private String studentID;
  private String pen;
  private String legalFirstName;
  private String legalMiddleNames;
  private String legalLastName;
  private String dob;
  private String sexCode;
  private String genderCode;
  private String usualFirstName;
  private String usualMiddleNames;
  private String usualLastName;
  private String email;
  private String deceasedDate;
  private String createUser;
  private String updateUser;
  private String localID;
  private String postalCode;
  private String gradeCode;
  private String mincode;
  private String emailVerified;
  private String historyActivityCode;
  private String gradeYear;
  private String demogCode;
  private String statusCode;
  private String memo;
  private String trueStudentID;
  private String documentTypeCode;
  private String dateOfConfirmation;
}
