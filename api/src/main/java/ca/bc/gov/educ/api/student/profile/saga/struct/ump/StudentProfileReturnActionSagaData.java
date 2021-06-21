package ca.bc.gov.educ.api.student.profile.saga.struct.ump;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class StudentProfileReturnActionSagaData extends StudentProfileRequestActionSagaData {

  @NotNull(message = "Comment content can not be null")
  String commentContent;
  @Pattern(regexp = "(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})")
  @Size(max = 19)
  @NotNull(message = "commentTimestamp can not be null")
  String commentTimestamp;
}
