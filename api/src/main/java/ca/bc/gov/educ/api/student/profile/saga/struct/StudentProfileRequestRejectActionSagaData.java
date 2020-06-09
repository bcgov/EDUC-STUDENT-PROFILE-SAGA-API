package ca.bc.gov.educ.api.student.profile.saga.struct;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
public class StudentProfileRequestRejectActionSagaData extends StudentProfileRequestActionSagaData {
  @NotNull
  String rejectionReason;
}
