package ca.bc.gov.educ.api.student.profile.saga.struct.ump;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class StudentProfileRequestActionSagaData {
  @NotNull(message = "studentProfileRequestID content can not be null")
  protected String studentProfileRequestID;
  @NotNull(message = "createUser can not be null")
  protected String createUser;
  @NotNull(message = "updateUser can not be null")
  protected String updateUser;
  protected String reviewer;
  @NotNull(message = "email can not be null")
  @Size(max = 80)
  @Email(message = "Email must be valid email address.")
  protected String email;
  @NotNull(message = "identityType can not be null")
  protected String identityType;
  protected String staffMemberIDIRGUID;
  protected String staffMemberName;
}
