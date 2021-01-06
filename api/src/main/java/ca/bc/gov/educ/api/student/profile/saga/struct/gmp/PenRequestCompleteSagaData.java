package ca.bc.gov.educ.api.student.profile.saga.struct.gmp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PenRequestCompleteSagaData {

  private String studentID;
  @NotNull(message = "digitalID can not be null.")
  private String digitalID;
  @NotNull(message = "penRequestID can not be null.")
  private String penRequestID;
  @NotNull(message = "PEN Number can not be null.")
  String pen;
  @Size(max = 40)
  String legalFirstName;
  @Size(max = 60)
  String legalMiddleNames;
  @Size(max = 40)
  @NotNull(message = "Legal Last Name can not be null.")
  String legalLastName;
  @NotNull(message = "Date of Birth can not be null.")
  @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")
  String dob;
  @NotNull(message = "Sex Code can not be null.")
  String sexCode;
  String genderCode;
  @Size(max = 40)
  String usualFirstName;
  @Size(max = 60)
  String usualMiddleNames;
  @Size(max = 40)
  String usualLastName;
  @Size(max = 12)
  String localID;
  @Size(max = 6)
  String postalCode;
  @Size(max = 2)
  String gradeCode;
  @Size(max = 8)
  String mincode;
  @Pattern(regexp = "[YN]")
  String emailVerified;
  @Size(max = 80)
  @Email(message = "Email must be valid email address.")
  String email;
  String deceasedDate;
  @Size(max = 32)
  String createUser;
  @Size(max = 32)
  String updateUser;
  private String bcscAutoMatchOutcome;
  private String bcscAutoMatchDetails;
  @NotNull(message = "penRequestStatusCode can not be null.")
  private String penRequestStatusCode;
  private String statusUpdateDate;
  @Size(max = 255)
  private String reviewer;
  @Size(max = 1)
  @Pattern(regexp = "[YN]")
  private String demogChanged;
  private String completeComment;
  @NotNull(message = "identityType can not be null")
  protected String identityType;
  @NotNull(message = "History Activity Code can not be null.")
  String historyActivityCode;
}
