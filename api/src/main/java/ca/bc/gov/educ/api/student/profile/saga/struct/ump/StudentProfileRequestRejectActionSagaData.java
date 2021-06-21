package ca.bc.gov.educ.api.student.profile.saga.struct.ump;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class StudentProfileRequestRejectActionSagaData extends StudentProfileRequestActionSagaData {
  @NotNull
  String rejectionReason;
}
