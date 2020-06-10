package ca.bc.gov.educ.api.student.profile.saga.struct;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class StudentProfileRequestActionSagaData {
  @NotNull(message = "studentProfileRequestID content can not be null")
  protected String studentProfileRequestID;
  @NotNull(message = "createUser can not be null")
  protected String createUser;
  @NotNull(message = "updateUser can not be null")
  protected String updateUser;
  @NotNull(message = "email can not be null")
  @Size(max = 80)
  @Email(message = "Email must be valid email address.")
  protected String email;
  @NotNull(message = "identityType can not be null")
  protected String identityType;
  protected String staffMemberIDIRGUID;
  protected String staffMemberName;
}
