package ca.bc.gov.educ.api.student.profile.saga.struct.gmp;

import ca.bc.gov.educ.api.student.profile.saga.struct.base.DigitalIdSagaData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PenRequestCompleteSagaData {

  String studentID;
  @NotNull(message = "digitalID can not be null.")
  String digitalID;
  @NotNull(message = "penRequestID can not be null.")
  String penRequestID;
  @NotNull(message = "PEN Number can not be null.")
  @Size(max = 9, min = 9)
  String pen;
  @Size(max = 25)
  String legalFirstName;
  @Size(max = 25)
  String legalMiddleNames;
  @Size(max = 25)
  @NotNull(message = "Legal Last Name can not be null.")
  String legalLastName;
  @NotNull(message = "Date of Birth can not be null.")
  @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")
  String dob;
  @NotNull(message = "Sex Code can not be null.")
  String sexCode;
  String genderCode;
  @Size(max = 25)
  String usualFirstName;
  @Size(max = 25)
  String usualMiddleNames;
  @Size(max = 25)
  String usualLastName;
  @Size(max = 12)
  String localID;
  @Size(max = 7)
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
  String bcscAutoMatchOutcome;
  String bcscAutoMatchDetails;
  @NotNull(message = "penRequestStatusCode can not be null.")
  String penRequestStatusCode;
  String statusUpdateDate;
  @Size(max = 255)
  String reviewer;
  @Size(max = 1)
  @Pattern(regexp = "[YN]")
  String demogChanged;
  String completeComment;
  @NotNull(message = "identityType can not be null")
  String identityType;
  @NotNull(message = "History Activity Code can not be null.")
  String historyActivityCode;
  String documentTypeCode; // internal place-holder field
  List<DigitalIdSagaData> digitalIdLinkedStudents;
}
