package ca.bc.gov.educ.api.student.profile.saga.struct.ump;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StudentProfileCompleteSagaData extends StudentProfileRequestActionSagaData {
  //Student Api/Digitalid Api
  private String studentID;
  @NotNull(message = "digitalID can not be null.")
  private String digitalID;
  //Student Api
  @NotNull(message = "penRequestID can not be null.")
  private String penRequestID;
  @NotNull(message = "PEN Number can not be null.")
  private String pen;
  @Size(max = 40)
  @NotNull(message = "Legal First Name can not be null.")
  private String legalFirstName;
  @Size(max = 60)
  private String legalMiddleNames;
  @Size(max = 40)
  @NotNull(message = "Legal Last Name can not be null.")
  private String legalLastName;
  @NotNull(message = "Date of Birth can not be null.")
  @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")
  private String dob;
  @NotNull(message = "Sex Code can not be null.")
  private String sexCode;
  private String genderCode;
  @Size(max = 40)
  private String usualFirstName;
  @Size(max = 60)
  private String usualMiddleNames;
  @Size(max = 40)
  private String usualLastName;
  @Size(max = 80)
  private String deceasedDate;
  private String emailVerified;
  //Student Profile Request
  private String completeComment;
}
