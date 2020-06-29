package ca.bc.gov.educ.api.student.profile.saga.struct.gmp;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
public class PenRequestRejectSagaData extends PenRequestActionsSagaData {
  @NotNull
  String rejectionReason;
}
